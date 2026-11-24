package com.leonai.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView

class MainActivity : Activity() {
    private lateinit var command: EditText
    private lateinit var result: TextView
    private val speechRequest = 42
    private lateinit var history: TextView
    private val prefs by lazy { getSharedPreferences("leon_local_chat", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
        }
        val title = TextView(this).apply { text = "🦁 León AI"; textSize = 28f }
        command = EditText(this).apply { hint = "Escribe: inicio, atrás o ajustes" }
        result = TextView(this).apply { text = "Control local. Ningún comando se envía a servidores."; textSize = 16f }
        history = TextView(this).apply { text = prefs.getString("history", "") ?: ""; textSize = 15f }
        val historyScroll = ScrollView(this).apply { addView(history) }
        val clearHistory = Button(this).apply { text = "Borrar conversación local"; setOnClickListener { prefs.edit().remove("history").apply(); history.text = ""; result.text = "Conversación borrada de este dispositivo" } }
        val send = Button(this).apply { text = "Ejecutar comando"; setOnClickListener { execute(command.text.toString()) } }
        val voice = Button(this).apply { text = "Hablar"; setOnClickListener { startVoice() } }
        val permissions = Button(this).apply {
            text = "Habilitar accesibilidad"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        listOf(title, command, send, voice, permissions, result, clearHistory, historyScroll).forEach(layout::addView)
        setContentView(layout)
    }
    private fun execute(raw: String) {
        if (raw.isBlank()) return
        when (raw.trim().lowercase()) {
            "inicio", "home" -> result.text = if (LeonAccessibilityService.navigateHome()) "Abriendo inicio" else "Activa accesibilidad primero"
            "atrás", "atras", "volver" -> result.text = if (LeonAccessibilityService.navigateBack()) "Volviendo" else "Activa accesibilidad primero"
            "ajustes" -> { startActivity(Intent(Settings.ACTION_SETTINGS)); result.text = "Abriendo ajustes" }
            else -> result.text = "El modelo de IA local aún no está instalado. Por ahora puedo ejecutar: inicio, atrás o ajustes."
        }
        val entry = "Tú: ${raw.trim()}" + System.lineSeparator() + "León: ${result.text}" + System.lineSeparator() + System.lineSeparator()
        val previous = prefs.getString("history", "") ?: ""
        val updated = (previous + entry).takeLast(100_000)
        prefs.edit().putString("history", updated).apply()
        history.text = updated
    }
    private fun startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 43)
            result.text = "Concede el permiso de micrófono y pulsa Hablar otra vez."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-DO")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con León AI")
        }
        try { startActivityForResult(intent, speechRequest) }
        catch (_: Exception) { result.text = "No hay reconocimiento de voz disponible en este dispositivo." }
    }
    @Deprecated("Uses platform speech activity callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
            command.setText(spoken)
            execute(spoken)
        }
    }
}
