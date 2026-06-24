package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PhraseEntity
import com.example.data.PhraseRepository
import com.example.data.SpokenLogEntity
import com.example.service.FlowerStateManager
import com.example.service.TalkingFlowerService
import com.example.tts.FlowerSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class FlowerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = PhraseRepository(database.phraseDao())

    // SharedPreferences for settings persistence
    private val sharedPrefs = context.getSharedPreferences("flower_prefs", Context.MODE_PRIVATE)

    // Room Data flows
    val phrases: StateFlow<List<PhraseEntity>> = repository.allPhrases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<SpokenLogEntity>> = repository.allSpokenLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unified Service state flows from our FlowerStateManager
    val isSpeaking = FlowerStateManager.isSpeaking
    val lastSpokenPhrase = FlowerStateManager.lastSpokenPhrase
    val isServiceActive = FlowerStateManager.isServiceActive

    // Local Speech Engine for when background service is not running
    private var localSpeechEngine: FlowerSpeechEngine? = null

    // Setting State flows (bound to SharedPreferences)
    private val _autoTalkInterval = MutableStateFlow(sharedPrefs.getInt("auto_talk_interval", 5))
    val autoTalkInterval = _autoTalkInterval.asStateFlow()

    private val _flowerColor = MutableStateFlow(sharedPrefs.getString("flower_color_hex", "#FFEB3B") ?: "#FFEB3B")
    val flowerColor = _flowerColor.asStateFlow()

    private val _voicePitch = MutableStateFlow(sharedPrefs.getFloat("flower_pitch", 1.4f))
    val voicePitch = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(sharedPrefs.getFloat("flower_speed", 1.05f))
    val voiceSpeed = _voiceSpeed.asStateFlow()

    private val _geminiBrainEnabled = MutableStateFlow(sharedPrefs.getBoolean("gemini_brain_enabled", false))
    val geminiBrainEnabled = _geminiBrainEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey = _geminiApiKey.asStateFlow()

    private val _geminiTheme = MutableStateFlow(sharedPrefs.getString("gemini_prompt_theme", "motivational") ?: "motivational")
    val geminiTheme = _geminiTheme.asStateFlow()

    private val _isGeneratingAI = MutableStateFlow(false)
    val isGeneratingAI = _isGeneratingAI.asStateFlow()

    init {
        viewModelScope.launch {
            repository.populateDefaultsIfEmpty()
        }

        // Initialize local speech engine
        localSpeechEngine = FlowerSpeechEngine(context) {
            Log.d("FlowerViewModel", "Local speech engine initialized")
        }

        localSpeechEngine?.setSpeechStateListener { speaking ->
            if (!isServiceActive.value) {
                FlowerStateManager.setSpeaking(speaking)
            }
        }
    }

    fun triggerSpeech() {
        viewModelScope.launch {
            if (isServiceActive.value) {
                // If service is active, trigger speech through the service
                val intent = Intent(context, TalkingFlowerService::class.java).apply {
                    action = TalkingFlowerService.ACTION_TRIGGER_SPEAK
                }
                context.startService(intent)
            } else {
                // Otherwise, speak using the local engine
                speakRandomPhraseLocally()
            }
        }
    }

    private suspend fun speakRandomPhraseLocally() {
        localSpeechEngine?.updateVoiceSettings(_voicePitch.value, _voiceSpeed.value)
        _isGeneratingAI.value = true

        var phraseText = ""

        if (_geminiBrainEnabled.value && _geminiApiKey.value.isNotEmpty()) {
            phraseText = generateGeminiPhrase(_geminiApiKey.value)
        }

        _isGeneratingAI.value = false

        if (phraseText.isEmpty() || phraseText.startsWith("Error")) {
            val enabledPhrases = repository.getEnabledPhrases()
            if (enabledPhrases.isNotEmpty()) {
                phraseText = enabledPhrases.random().text
            } else {
                phraseText = "I believe in you!"
            }
        }

        repository.addSpokenLog(phraseText)
        FlowerStateManager.setLastSpokenPhrase(phraseText)

        withContext(Dispatchers.Main) {
            localSpeechEngine?.speak(phraseText)
        }
    }

    private suspend fun generateGeminiPhrase(apiKey: String): String = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = "You are the Talking Flower companion from Super Mario Bros Wonder. " +
                    "You say short, silly, cheerful, or slightly sarcastic things to the player. " +
                    "Keep your phrases strictly under 10 words, snappy, warm, and highly engaging. " +
                    "Examples: 'I wonder what Goombas taste like...', 'I believe in you!', 'Wow, you're tall today!', 'Ouch! Watch it!'"

            val prompt = "Say something extremely funny, charming, or encouraging that fits a '${_geminiTheme.value}' theme!"

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.9)
                })
            }

            conn.outputStream.use { os ->
                val input = requestBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")
                text.trim().trim('"', '\'')
            } else {
                Log.e("FlowerViewModel", "Gemini API HTTP Error: ${conn.responseCode}")
                ""
            }
        } catch (e: Exception) {
            Log.e("FlowerViewModel", "Error generating Gemini phrase: ${e.message}", e)
            ""
        }
    }

    // Toggle Background Service
    fun toggleBackgroundService() {
        if (isServiceActive.value) {
            TalkingFlowerService.stopService(context)
        } else {
            TalkingFlowerService.startService(context)
        }
    }

    // Interval settings
    fun setAutoTalkInterval(minutes: Int) {
        _autoTalkInterval.value = minutes
        sharedPrefs.edit().putInt("auto_talk_interval", minutes).apply()
        // If background service is active, restart it to apply new timer interval
        if (isServiceActive.value) {
            TalkingFlowerService.startService(context)
        }
    }

    // Aesthetic customizations
    fun setFlowerColor(colorHex: String) {
        _flowerColor.value = colorHex
        sharedPrefs.edit().putString("flower_color_hex", colorHex).apply()
    }

    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
        sharedPrefs.edit().putFloat("flower_pitch", pitch).apply()
    }

    fun setVoiceSpeed(speed: Float) {
        _voiceSpeed.value = speed
        sharedPrefs.edit().putFloat("flower_speed", speed).apply()
    }

    // Gemini settings
    fun setGeminiBrainEnabled(enabled: Boolean) {
        _geminiBrainEnabled.value = enabled
        sharedPrefs.edit().putBoolean("gemini_brain_enabled", enabled).apply()
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        sharedPrefs.edit().putString("gemini_api_key", key).apply()
    }

    fun setGeminiTheme(theme: String) {
        _geminiTheme.value = theme
        sharedPrefs.edit().putString("gemini_prompt_theme", theme).apply()
    }

    // Room operations
    fun addCustomPhrase(text: String) {
        viewModelScope.launch {
            if (text.isNotBlank()) {
                repository.insertPhrase(text, "custom")
            }
        }
    }

    fun togglePhrase(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.setPhraseEnabled(id, enabled)
        }
    }

    fun deletePhrase(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomPhrase(id)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    override fun onCleared() {
        localSpeechEngine?.stop()
        localSpeechEngine?.shutdown()
        super.onCleared()
    }
}
