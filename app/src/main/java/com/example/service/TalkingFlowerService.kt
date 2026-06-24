package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.PhraseRepository
import com.example.service.FlowerStateManager
import com.example.tts.FlowerSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

class TalkingFlowerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var speechEngine: FlowerSpeechEngine? = null
    private lateinit var repository: PhraseRepository

    companion object {
        const val CHANNEL_ID = "talking_flower_channel"
        const val NOTIFICATION_ID = 4821
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TRIGGER_SPEAK = "ACTION_TRIGGER_SPEAK"

        fun startService(context: Context) {
            val intent = Intent(context, TalkingFlowerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TalkingFlowerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TalkingFlowerService", "Service created")
        createNotificationChannel()

        val database = AppDatabase.getDatabase(this)
        repository = PhraseRepository(database.phraseDao())

        speechEngine = FlowerSpeechEngine(this) {
            Log.d("TalkingFlowerService", "Speech engine initialized in service")
        }

        speechEngine?.setSpeechStateListener { isSpeaking ->
            FlowerStateManager.setSpeaking(isSpeaking)
        }

        FlowerStateManager.setServiceActive(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                restartTimer()
            }
            ACTION_STOP -> {
                stopTimer()
                stopSelf()
            }
            ACTION_TRIGGER_SPEAK -> {
                serviceScope.launch {
                    speakRandomPhrase()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification("I'm awake! Tap to talk with me.")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Talking Flower companion")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // fallback system icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Talking Flower Activity",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Talking Flower talking in the background."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun restartTimer() {
        timerJob?.cancel()
        val sharedPrefs = getSharedPreferences("flower_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = sharedPrefs.getInt("auto_talk_interval", 5)
        val intervalMs = intervalMinutes * 60 * 1000L

        Log.d("TalkingFlowerService", "Starting background timer with interval of $intervalMinutes mins")

        timerJob = serviceScope.launch {
            // Wait slightly before the very first background speak, or speak immediately?
            // Let's wait for the interval to pass, so it speaks every 5 minutes in background.
            while (isActive) {
                delay(intervalMs)
                speakRandomPhrase()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun speakRandomPhrase() {
        val sharedPrefs = getSharedPreferences("flower_prefs", Context.MODE_PRIVATE)
        val isGeminiEnabled = sharedPrefs.getBoolean("gemini_brain_enabled", false)
        val geminiApiKey = sharedPrefs.getString("gemini_api_key", "") ?: ""
        val customPitch = sharedPrefs.getFloat("flower_pitch", 1.4f)
        val customSpeed = sharedPrefs.getFloat("flower_speed", 1.05f)

        speechEngine?.updateVoiceSettings(customPitch, customSpeed)

        var phraseText = ""

        if (isGeminiEnabled && geminiApiKey.isNotEmpty()) {
            phraseText = generateGeminiPhrase(geminiApiKey)
        }

        if (phraseText.isEmpty() || phraseText.startsWith("Error")) {
            // Fallback to local Room database
            val phrases = repository.getEnabledPhrases()
            if (phrases.isNotEmpty()) {
                phraseText = phrases.random().text
            } else {
                phraseText = "I believe in you!" // ultimate fallback
            }
        }

        // Record it in log
        repository.addSpokenLog(phraseText)
        FlowerStateManager.setLastSpokenPhrase(phraseText)

        // Speak it
        withContext(Dispatchers.Main) {
            speechEngine?.speak(phraseText)
            updateNotification("Last said: \"$phraseText\"")
        }
    }

    private suspend fun generateGeminiPhrase(apiKey: String): String = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = "You are the Talking Flower companion from Super Mario Bros Wonder. " +
                    "You say short, silly, cheerful, or slightly sarcastic things to the player. " +
                    "Keep your phrases strictly under 10 words, snappy, warm, and highly engaging. " +
                    "Examples: 'I wonder what Goombas taste like...', 'I believe in you!', 'Wow, you're tall today!', 'Ouch! Watch it!'"

            val sharedPrefs = getSharedPreferences("flower_prefs", Context.MODE_PRIVATE)
            val customPrompt = sharedPrefs.getString("gemini_prompt_theme", "motivational") ?: "motivational"

            val prompt = "Say something extremely funny, charming, or encouraging that fits a '$customPrompt' theme!"

            // Direct REST API as specified in gemini_api SKILL.md
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

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseText)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            text.trim().trim('"', '\'')
        } catch (e: Exception) {
            Log.e("TalkingFlowerService", "Error calling Gemini API: ${e.message}", e)
            ""
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("TalkingFlowerService", "Service destroyed")
        stopTimer()
        speechEngine?.stop()
        speechEngine?.shutdown()
        FlowerStateManager.setServiceActive(false)
        FlowerStateManager.setSpeaking(false)
        super.onDestroy()
    }
}
