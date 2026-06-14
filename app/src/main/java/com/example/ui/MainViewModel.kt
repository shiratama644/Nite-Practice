package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.receiver.ReminderReceiver
import com.example.ui.components.DailyProgressPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    GAME, PROGRESS, TUTOR, SETTINGS
}

enum class ClefPreference {
    ALL, TREBLE, BASS;

    fun toClef(): Clef? = when (this) {
        ALL -> null
        TREBLE -> Clef.TREBLE
        BASS -> Clef.BASS
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = LogsRepository(database.logsDao())

    // Tabs
    val currentTab = MutableStateFlow(AppTab.GAME)

    // User settings and preferences
    val clefPreference = MutableStateFlow(ClefPreference.ALL)
    val reviewModeEnabled = MutableStateFlow(false)

    // Game states
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _reactionStartTime = MutableStateFlow(0L)
    val reactionStartTime: StateFlow<Long> = _reactionStartTime.asStateFlow()

    private val _feedbackText = MutableStateFlow<String?>(null)
    val feedbackText: StateFlow<String?> = _feedbackText.asStateFlow()

    private val _feedbackIsCorrect = MutableStateFlow<Boolean?>(null)
    val feedbackIsCorrect: StateFlow<Boolean?> = _feedbackIsCorrect.asStateFlow()

    // Database updates combined reactively
    val sessionLogs: StateFlow<List<DailySessionLog>> = repository.allSessionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakeLogs: StateFlow<List<MistakeLog>> = repository.allMistakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregate daily stats points for progress lines
    val dailyProgressPoints: StateFlow<List<DailyProgressPoint>> = repository.allSessionLogs
        .map { logs ->
            logs.groupBy { it.dateString }
                .map { (date, dailyList) ->
                    val avgTime = dailyList.map { it.responseTimeMs }.average().toFloat()
                    val correctCount = dailyList.count { it.isCorrect }
                    val accuracy = (correctCount.toFloat() / dailyList.size) * 100f
                    DailyProgressPoint(
                        dateString = date,
                        avgResponseTimeMs = avgTime,
                        accuracyPercentage = accuracy,
                        totalAttempts = dailyList.size
                    )
                }
                .sortedBy { it.dateString } // chronological order for line transitions
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Tutor state
    val tutorExplanation = MutableStateFlow<String?>(null)
    val tutorLoading = MutableStateFlow(false)

    // Alarm hour/minute presets
    val alarmHour = MutableStateFlow(20)
    val alarmMinute = MutableStateFlow(0)
    val isReminderEnabled = MutableStateFlow(true)

    init {
        // Initialize first note question
        nextQuestion()

        // Read saved reminder hour from shared preferences if needed
        val prefs = application.getSharedPreferences("note_trainer_prefs", Context.MODE_PRIVATE)
        alarmHour.value = prefs.getInt("alarm_hour", 20)
        alarmMinute.value = prefs.getInt("alarm_minute", 0)
        isReminderEnabled.value = prefs.getBoolean("alarm_enabled", true)
        
        // Schedule if enabled
        if (isReminderEnabled.value) {
            ReminderReceiver.setDailyAlarm(application, alarmHour.value, alarmMinute.value)
        }
    }

    fun selectTab(tab: AppTab) {
        currentTab.value = tab
    }

    fun setClefPreference(pref: ClefPreference) {
        clefPreference.value = pref
        nextQuestion()
    }

    fun toggleReviewMode(enabled: Boolean) {
        reviewModeEnabled.value = enabled
        nextQuestion()
    }

    fun nextQuestion() {
        // Choose note pool
        val pool = when (clefPreference.value) {
            ClefPreference.ALL -> NotePool.allNotes
            ClefPreference.TREBLE -> NotePool.trebleNotes
            ClefPreference.BASS -> NotePool.bassNotes
        }

        val missed = mistakeLogs.value
        // Filters mistakes that belong to current clef preference
        val filteredMissed = missed.filter { log ->
            clefPreference.value == ClefPreference.ALL || log.clef == clefPreference.value.name
        }

        val isReview = reviewModeEnabled.value && filteredMissed.isNotEmpty()

        // 60% weight to pick from mistakes in Review Mode
        val nextNote = if (isReview && Math.random() < 0.60) {
            val pickedLog = filteredMissed.shuffled().first()
            NotePool.getNoteById(pickedLog.noteKey) ?: pool.random()
        } else {
            pool.random()
        }

        _currentNote.value = nextNote
        _reactionStartTime.value = System.currentTimeMillis()
        _feedbackText.value = null
        _feedbackIsCorrect.value = null
    }

    fun submitAnswer(answeredLetter: Letter) {
        val note = _currentNote.value ?: return
        if (_feedbackIsCorrect.value != null) return // Ignore double clicks before nextQuestion

        val elapsed = System.currentTimeMillis() - _reactionStartTime.value
        val isCorrect = note.letter == answeredLetter

        _feedbackIsCorrect.value = isCorrect

        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        val dateStr = sdf.format(Date())

        viewModelScope.launch {
            // Write session log to Room
            val sessionLog = DailySessionLog(
                dateString = dateStr,
                responseTimeMs = elapsed,
                isCorrect = isCorrect,
                clef = note.clef.name,
                noteName = note.displayPitch
            )
            repository.insertSessionLog(sessionLog)

            if (isCorrect) {
                // Remove mistake from list upon correct answer (Reward Mastery!)
                repository.removeMistake("${note.clef.name}_${note.displayPitch}")
                _feedbackText.value = "正解です！ お見事！（${elapsed}ms）"
            } else {
                // Record/increment mistake
                repository.recordMistake(note.clef.name, note.displayPitch)
                _feedbackText.value = "違います！ 正解は「${note.japaneseName} (${note.englishName})」でした（${elapsed}ms）"
            }
        }
    }

    // AI Tutor explainer query
    fun askTutor() {
        val note = _currentNote.value ?: return
        tutorExplanation.value = null
        tutorLoading.value = true
        
        // Jump tab to tutor to view response beautifully
        currentTab.value = AppTab.TUTOR

        viewModelScope.launch {
            val explanation = GeminiTutor.fetchExplanation(
                clef = if (note.clef == Clef.TREBLE) "ト音記号" else "ヘ音記号",
                pitchName = note.displayPitch,
                japaneseName = note.japaneseName
            )
            tutorExplanation.value = explanation
            tutorLoading.value = false
        }
    }

    // Alarm scheduler
    fun updateReminderTime(hour: Int, minute: Int) {
        alarmHour.value = hour
        alarmMinute.value = minute
        isReminderEnabled.value = true

        val application = getApplication<Application>()
        val prefs = application.getSharedPreferences("note_trainer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("alarm_hour", hour)
            putInt("alarm_minute", minute)
            putBoolean("alarm_enabled", true)
            apply()
        }

        ReminderReceiver.setDailyAlarm(application, hour, minute)
    }

    fun disableReminder() {
        isReminderEnabled.value = false
        val application = getApplication<Application>()
        val prefs = application.getSharedPreferences("note_trainer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("alarm_enabled", false).apply()

        ReminderReceiver.cancelAlarm(application)
    }

    fun deleteMistake(noteKey: String) {
        viewModelScope.launch {
            repository.removeMistake(noteKey)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            nextQuestion()
        }
    }
}
