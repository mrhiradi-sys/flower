package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class FlowerSpeechEngine(
    context: Context,
    private val onInitComplete: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var onSpeechStateChanged: (Boolean) -> Unit = {}

    private var pitchValue: Float = 1.4f
    private var speedValue: Float = 1.05f

    init {
        setupListener()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("FlowerSpeechEngine", "Language is not supported or missing data")
            } else {
                isInitialized = true
                applySettings()
                onInitComplete()
            }
        } else {
            Log.e("FlowerSpeechEngine", "TTS Initialization Failed")
        }
    }

    fun setSpeechStateListener(listener: (Boolean) -> Unit) {
        onSpeechStateChanged = listener
    }

    fun updateVoiceSettings(pitch: Float, speed: Float) {
        pitchValue = pitch
        speedValue = speed
        applySettings()
    }

    private fun applySettings() {
        if (isInitialized) {
            tts?.setPitch(pitchValue)
            tts?.setSpeechRate(speedValue)
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeechStateChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeechStateChanged(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onSpeechStateChanged(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeechStateChanged(false)
            }
        })
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("FlowerSpeechEngine", "TTS is not initialized yet")
            return
        }
        applySettings()
        val utteranceId = System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeechStateChanged(false)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
