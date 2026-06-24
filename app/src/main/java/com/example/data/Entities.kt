package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val category: String = "classic",
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false
)

@Entity(tableName = "spoken_logs")
data class SpokenLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
