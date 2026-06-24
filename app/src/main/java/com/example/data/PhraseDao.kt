package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY isCustom DESC, text ASC")
    fun getAllPhrases(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE isEnabled = 1")
    suspend fun getEnabledPhrases(): List<PhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity)

    @Query("UPDATE phrases SET isEnabled = :enabled WHERE id = :id")
    suspend fun updatePhraseEnabled(id: Int, enabled: Boolean)

    @Query("DELETE FROM phrases WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomPhrase(id: Int)

    // Spoken Logs
    @Query("SELECT * FROM spoken_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllSpokenLogs(): Flow<List<SpokenLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpokenLog(log: SpokenLogEntity)

    @Query("DELETE FROM spoken_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun getPhraseCount(): Int
}
