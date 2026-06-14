package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_session_logs")
data class DailySessionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,      // Format: YYYY-MM-DD
    val responseTimeMs: Long,
    val isCorrect: Boolean,
    val clef: String,            // "TREBLE" or "BASS"
    val noteName: String,        // e.g. "C4", "E5"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistake_logs")
data class MistakeLog(
    @PrimaryKey val noteKey: String, // format: "CLEF_NOTENAME" (e.g. "TREBLE_C4")
    val clef: String,
    val noteName: String,
    val mistakeCount: Int,
    val lastMistakeTime: Long = System.currentTimeMillis()
)
