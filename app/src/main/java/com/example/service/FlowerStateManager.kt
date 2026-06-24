package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlowerStateManager {
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenPhrase = MutableStateFlow("")
    val lastSpokenPhrase: StateFlow<String> = _lastSpokenPhrase.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    fun setSpeaking(speaking: Boolean) {
        _isSpeaking.value = speaking
    }

    fun setLastSpokenPhrase(phrase: String) {
        _lastSpokenPhrase.value = phrase
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }
}
