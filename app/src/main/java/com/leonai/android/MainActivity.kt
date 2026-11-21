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

class MainActivity : Activity() {
    private lateinit var command: EditText
    private lateinit var result: TextView
    private val speechRequest = 42
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
        }
        val title = TextView(this).apply { text = "🦁 León AI"; textSize = 28f }
        command = EditText(this).apply { hint = "Escribe: inicio, atrás o ajustes" }
        result = TextView(this).apply { text = "Control local. Ningún comando se envía a servidores."; textSize = 16f }
        val send = Button(this).apply { text = "Ejecutar comando"; setOnClickListener { execute(command.text.toString()) } }
        val voice = Button(this).apply { text = "Hablar"; setOnClickListener { startVoice() } }
        val permissions = Button(this).apply {
            text = "Habilitar accesibilidad"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        listOf(title, command, send, voice, permissions, result).forEach(layout::addView)
        setContentView(layout)
    }
    private fun execute(raw: String) {
        when (raw.trim().lowercase()) {
            "inicio", "home" -> result.text = if (LeonAccessibilityService.navigateHome()) "Abriendo inicio" else "Activa accesibilidad primero"
            "atrás", "atras", "volver" -> result.text = if (LeonAccessibilityService.navigateBack()) "Volviendo" else "Activa accesibilidad primero"
            "ajustes" -> { startActivity(Intent(Settings.ACTION_SETTINGS)); result.text = "Abriendo ajustes" }
            else -> result.text = "Todavía no conozco ese comando. Prueba: inicio, atrás o ajustes."
        }
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
