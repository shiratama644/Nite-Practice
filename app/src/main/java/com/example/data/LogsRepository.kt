package com.example.data

import kotlinx.coroutines.flow.Flow

class LogsRepository(private val logsDao: LogsDao) {

    val allSessionLogs: Flow<List<DailySessionLog>> = logsDao.getAllSessionLogs()
    val allMistakes: Flow<List<MistakeLog>> = logsDao.getAllMistakes()

    suspend fun insertSessionLog(log: DailySessionLog) {
        logsDao.insertSessionLog(log)
    }

    suspend fun recordMistake(clef: String, noteName: String) {
        val noteKey = "${clef}_${noteName}"
        val time = System.currentTimeMillis()
        logsDao.recordMistake(noteKey, clef, noteName, time)
    }

    suspend fun removeMistake(noteKey: String) {
        logsDao.removeMistake(noteKey)
    }

    suspend fun clearAll() {
        logsDao.clearSessionLogs()
        logsDao.clearMistakeLogs()
    }
}
