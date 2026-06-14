package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(log: DailySessionLog)

    // UPSERT directly in SQLite for atomicity
    @Query("""
        INSERT INTO mistake_logs (noteKey, clef, noteName, mistakeCount, lastMistakeTime) 
        VALUES (:noteKey, :clef, :noteName, 1, :time) 
        ON CONFLICT(noteKey) DO UPDATE SET 
            mistakeCount = mistakeCount + 1, 
            lastMistakeTime = :time
    """)
    suspend fun recordMistake(noteKey: String, clef: String, noteName: String, time: Long)

    @Query("DELETE FROM mistake_logs WHERE noteKey = :noteKey")
    suspend fun removeMistake(noteKey: String)

    @Query("SELECT * FROM mistake_logs ORDER BY mistakeCount DESC")
    fun getAllMistakes(): Flow<List<MistakeLog>>

    @Query("SELECT * FROM daily_session_logs ORDER BY timestamp DESC")
    fun getAllSessionLogs(): Flow<List<DailySessionLog>>

    @Query("DELETE FROM daily_session_logs")
    suspend fun clearSessionLogs()

    @Query("DELETE FROM mistake_logs")
    suspend fun clearMistakeLogs()
}
