package com.example.data

import kotlinx.coroutines.flow.Flow

class PhraseRepository(private val phraseDao: PhraseDao) {

    val allPhrases: Flow<List<PhraseEntity>> = phraseDao.getAllPhrases()
    val allSpokenLogs: Flow<List<SpokenLogEntity>> = phraseDao.getAllSpokenLogs()

    suspend fun populateDefaultsIfEmpty() {
        if (phraseDao.getPhraseCount() == 0) {
            val defaults = listOf(
                "Onward and upward!",
                "I believe in you!",
                "Go get 'em!",
                "Is anyone there?",
                "Ouch! Watch it!",
                "You can do it!",
                "I wonder what Goombas taste like...",
                "Good job!",
                "Well, that was something.",
                "Wait, did you hear that?",
                "Are you sure about this?",
                "I'm rooting for you!",
                "That was a close one!",
                "Keep going!",
                "Awesome!",
                "Wow, you make it look easy!",
                "Hahaha, that tickles!",
                "I'm a flower!",
                "Hmm, what should I eat for dinner?",
                "I believe you can fly!",
                "Wow, you're tall today!",
                "Is it hot in here, or is it just me?",
                "What a gorgeous view!",
                "Please water me later!",
                "Did you forget something?"
            )
            defaults.forEach { text ->
                phraseDao.insertPhrase(
                    PhraseEntity(
                        text = text,
                        category = "classic",
                        isEnabled = true,
                        isCustom = false
                    )
                )
            }
        }
    }

    suspend fun getEnabledPhrases(): List<PhraseEntity> {
        return phraseDao.getEnabledPhrases()
    }

    suspend fun insertPhrase(text: String, category: String) {
        phraseDao.insertPhrase(
            PhraseEntity(
                text = text,
                category = category,
                isEnabled = true,
                isCustom = true
            )
        )
    }

    suspend fun setPhraseEnabled(id: Int, enabled: Boolean) {
        phraseDao.updatePhraseEnabled(id, enabled)
    }

    suspend fun deleteCustomPhrase(id: Int) {
        phraseDao.deleteCustomPhrase(id)
    }

    suspend fun addSpokenLog(text: String) {
        phraseDao.insertSpokenLog(SpokenLogEntity(text = text))
    }

    suspend fun clearAllLogs() {
        phraseDao.clearAllLogs()
    }
}
