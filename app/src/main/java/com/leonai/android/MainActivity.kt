package com.leonai.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.io.File
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
    private val modelRequest = 44
    private lateinit var history: TextView
    private lateinit var modelStatus: TextView
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
        modelStatus = TextView(this).apply {
            text = if (File(filesDir, "leon-model.gguf").exists()) "Modelo GGUF importado; motor de inferencia pendiente" else "Sin modelo local instalado"
        }
        val importModel = Button(this).apply {
            text = "Importar modelo GGUF"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(intent, modelRequest)
            }
        }
        val clearHistory = Button(this).apply { text = "Borrar conversación local"; setOnClickListener { prefs.edit().remove("history").apply(); history.text = ""; result.text = "Conversación borrada de este dispositivo" } }
        val send = Button(this).apply { text = "Ejecutar comando"; setOnClickListener { execute(command.text.toString()) } }
        val voice = Button(this).apply { text = "Hablar"; setOnClickListener { startVoice() } }
        val permissions = Button(this).apply {
            text = "Habilitar accesibilidad"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        listOf(title, modelStatus, importModel, command, send, voice, permissions, result, clearHistory, historyScroll).forEach(layout::addView)
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
        if (requestCode == modelRequest && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            result.text = "Importando modelo en almacenamiento privado..."
            Thread {
                try {
                    val target = File(filesDir, "leon-model.gguf")
                    val temporary = File(filesDir, "leon-model.gguf.tmp")
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            temporary.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("No se pudo leer el archivo")
                        if (temporary.length() < 1024 * 1024) error("El archivo no parece ser un modelo GGUF válido")
                        temporary.inputStream().use { stream ->
                            val magic = ByteArray(4)
                            if (stream.read(magic) != 4 || !magic.contentEquals(byteArrayOf(71, 71, 85, 70))) error("Formato GGUF inválido")
                        }
                        if (target.exists() && !target.delete()) error("No se pudo reemplazar el modelo anterior")
                        if (!temporary.renameTo(target)) error("No se pudo guardar el modelo")
                        runOnUiThread {
                            modelStatus.text = "Modelo GGUF importado; motor de inferencia pendiente"
                            result.text = "Modelo guardado localmente. Aún no puede generar respuestas."
                        }
                    } finally { temporary.delete() }
                } catch (error: Exception) {
                    runOnUiThread { result.text = "No se pudo importar el modelo: ${error.message}" }
                }
            }.start()
            return
        }
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
            command.setText(spoken)
            execute(spoken)
        }
    }
}
