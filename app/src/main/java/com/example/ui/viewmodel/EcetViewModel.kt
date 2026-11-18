package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ElectronicsAiService
import com.example.data.local.AiSummaryEntity
import com.example.data.local.DailyTaskEntity
import com.example.data.local.EcetDatabase
import com.example.data.local.McqSessionEntity
import com.example.data.local.MockTestEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.StudySessionEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.AiChatMessage
import com.example.data.model.AiSummaryUiState
import com.example.data.model.AnalyticsState
import com.example.data.model.AttachedStudyFile
import com.example.data.model.DayStudyMetric
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.data.model.PreparationOverview
import com.example.data.model.StudyMode
import com.example.data.model.StudyStats
import com.example.data.model.Subject
import com.example.data.model.SubjectMcqPerformance
import com.example.data.model.SubjectSummary
import com.example.data.model.ThemeMode
import com.example.data.model.TimerUiState
import com.example.data.model.TopicMcqPerformance
import com.example.data.model.TopicPriority
import com.example.data.model.TopicStatus
import com.example.data.model.UserPreferences
import com.example.data.repository.EcetRepository
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val VIEWMODEL_TAG = "EcetViewModel"
private const val MAX_ATTACHMENT_BYTES = 18 * 1024 * 1024L

data class EcetUiState(
    val preferences: UserPreferences = UserPreferences(),
    val topics: List<SyllabusTopicEntity> = emptyList(),
    val studySessions: List<StudySessionEntity> = emptyList(),
    val dailyTasks: List<DailyTaskEntity> = emptyList(),
    val mcqSessions: List<McqSessionEntity> = emptyList(),
    val mockTests: List<MockTestEntity> = emptyList(),
    val aiSummaries: List<AiSummaryEntity> = emptyList(),
    val overview: PreparationOverview = PreparationOverview(),
    val studyStats: StudyStats = StudyStats(),
    val subjectMcqPerformance: List<SubjectMcqPerformance> = emptyList(),
    val topicMcqPerformance: List<TopicMcqPerformance> = emptyList(),
    val analytics: AnalyticsState = AnalyticsState(),
    val todayEpochDay: Long = DateUtils.todayEpochDay()
)

class EcetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = EcetDatabase.getInstance(application)
    private val prefsManager = PreferencesManager(application)
    private val repository = EcetRepository(database.ecetDao(), prefsManager)
    private val electronicsAiService = ElectronicsAiService()

    private val _timerUiState = MutableStateFlow(TimerUiState())
    val timerUiState: StateFlow<TimerUiState> = _timerUiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    // AI Summary & Q&A state
    private val _summaryUiState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val summaryUiState: StateFlow<AiSummaryUiState> = _summaryUiState.asStateFlow()

    private val _summarySubject = MutableStateFlow(Subject.ECE)
    val summarySubject: StateFlow<Subject> = _summarySubject.asStateFlow()

    private val _summaryTopicInput = MutableStateFlow("")
    val summaryTopicInput: StateFlow<String> = _summaryTopicInput.asStateFlow()

    private val _attachedStudyFile = MutableStateFlow<AttachedStudyFile?>(null)
    val attachedStudyFile: StateFlow<AttachedStudyFile?> = _attachedStudyFile.asStateFlow()

    private val _qaMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val qaMessages: StateFlow<List<AiChatMessage>> = _qaMessages.asStateFlow()

    private val _qaError = MutableStateFlow<String?>(null)
    val qaError: StateFlow<String?> = _qaError.asStateFlow()

    private val _isQaLoading = MutableStateFlow(false)
    val isQaLoading: StateFlow<Boolean> = _isQaLoading.asStateFlow()

    private var summaryJob: Job? = null
    private var qaJob: Job? = null

    private val timerMutex = Mutex()
    private var latestSnapshot = PersistedTimerSnapshot()
    private var latestPrefs = UserPreferences()

    private val baseDataFlow = combine(
        repository.preferencesFlow,
        repository.topicsFlow,
        repository.studySessionsFlow,
        repository.dailyTasksFlow,
        repository.mcqSessionsFlow
    ) { prefs, topics, sessions, tasks, mcqs ->
        BaseTuple(prefs, topics, sessions, tasks, mcqs)
    }

    val uiState: StateFlow<EcetUiState> = combine(
        baseDataFlow,
        repository.mockTestsFlow,
        repository.aiSummariesFlow
    ) { base, mocks, summaries ->
        buildUiState(
            prefs = base.prefs,
            topics = base.topics,
            sessions = base.sessions,
            tasks = base.tasks,
            mcqs = base.mcqs,
            mocks = mocks,
            summaries = summaries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EcetUiState()
    )

    init {
        viewModelScope.launch {
            combine(
                repository.timerSnapshotFlow,
                repository.preferencesFlow
            ) { snapshot, prefs -> snapshot to prefs }
                .collect { (snapshot, prefs) ->
                    latestSnapshot = snapshot
                    latestPrefs = prefs
                    syncTimerUiFromSnapshot(snapshot, prefs)
                }
        }

        viewModelScope.launch {
            while (isActive) {
                if (latestSnapshot.isRunning) {
                    evaluateRunningTimer()
                }
                delay(250L)
            }
        }
    }

    private fun targetMinutesForPhase(phase: PomodoroPhase, prefs: UserPreferences): Int = when (phase) {
        PomodoroPhase.FOCUS -> prefs.pomodoroFocusMinutes
        PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes
        PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes
    }

    private fun syncTimerUiFromSnapshot(snapshot: PersistedTimerSnapshot, prefs: UserPreferences) {
        val now = System.currentTimeMillis()
        val elapsedMs = if (snapshot.isRunning) {
            (snapshot.accumulatedMs + (now - snapshot.startWallMs).coerceAtLeast(0L)).coerceAtLeast(0L)
        } else {
            snapshot.accumulatedMs.coerceAtLeast(0L)
        }
        val targetMs = targetMinutesForPhase(snapshot.phase, prefs) * 60_000L
        val remainingMs = (targetMs - elapsedMs).coerceAtLeast(0L)

        _timerUiState.value = TimerUiState(
            isRunning = snapshot.isRunning,
            mode = snapshot.mode,
            phase = snapshot.phase,
            elapsedMs = elapsedMs,
            remainingMs = remainingMs,
            targetDurationMs = targetMs,
            subject = snapshot.subject,
            topicId = snapshot.topicId,
            topicTitle = snapshot.topicTitle,
            pomodoroRound = snapshot.pomodoroRound,
            focusMinutes = prefs.pomodoroFocusMinutes,
            shortBreakMinutes = prefs.pomodoroShortBreakMinutes,
            longBreakMinutes = prefs.pomodoroLongBreakMinutes
        )
    }

    private suspend fun evaluateRunningTimer() {
        timerMutex.withLock {
            val snap = latestSnapshot
            val prefs = latestPrefs
            if (!snap.isRunning) return

            val now = System.currentTimeMillis()
            val elapsedMs = snap.accumulatedMs + (now - snap.startWallMs).coerceAtLeast(0L)

            if (snap.mode == StudyMode.POMODORO) {
                val targetMs = targetMinutesForPhase(snap.phase, prefs) * 60_000L
                if (elapsedMs >= targetMs) {
                    if (snap.phase == PomodoroPhase.FOCUS) {
                        val endWall = (snap.startWallMs + (targetMs - snap.accumulatedMs)).coerceAtMost(now)
                        val durationSec = (targetMs / 1000L).coerceAtLeast(1L)
                        repository.insertStudySession(
                            StudySessionEntity(
                                subject = snap.subject.name,
                                topicId = snap.topicId,
                                topicTitle = snap.topicTitle,
                                mode = StudyMode.POMODORO.name,
                                durationSeconds = durationSec,
                                startedAtMs = endWall - targetMs,
                                endedAtMs = endWall,
                                dateEpochDay = DateUtils.epochMillisToLocalEpochDay(endWall)
                            )
                        )
                        val nextPhase = if (snap.pomodoroRound % 4 == 0) {
                            PomodoroPhase.LONG_BREAK
                        } else {
                            PomodoroPhase.SHORT_BREAK
                        }
                        val nextSnap = snap.copy(
                            isRunning = false,
                            phase = nextPhase,
                            startWallMs = 0L,
                            accumulatedMs = 0L
                        )
                        latestSnapshot = nextSnap
                        repository.saveTimerSnapshot(nextSnap)
                        syncTimerUiFromSnapshot(nextSnap, prefs)

                        val msg = "${snap.subject.shortName} focus session (${prefs.pomodoroFocusMinutes}m) logged! Ready for ${nextPhase.label}."
                        _snackbarEvents.tryEmit(msg)
                        if (prefs.pomodoroAlertsEnabled) {
                            NotificationHelper.showTimerAlert(
                                getApplication(),
                                "Focus Session Complete",
                                msg
                            )
                        }
                    } else {
                        val nextRound = if (snap.phase == PomodoroPhase.LONG_BREAK) 1 else snap.pomodoroRound + 1
                        val nextSnap = snap.copy(
                            isRunning = false,
                            phase = PomodoroPhase.FOCUS,
                            startWallMs = 0L,
                            accumulatedMs = 0L,
                            pomodoroRound = nextRound
                        )
                        latestSnapshot = nextSnap
                        repository.saveTimerSnapshot(nextSnap)
                        syncTimerUiFromSnapshot(nextSnap, prefs)

                        val msg = "${snap.phase.label} complete! Ready for Focus Round #$nextRound."
                        _snackbarEvents.tryEmit(msg)
                        if (prefs.pomodoroAlertsEnabled) {
                            NotificationHelper.showTimerAlert(
                                getApplication(),
                                "Break Complete",
                                msg
                            )
                        }
                    }
                    return
                }
            }
            syncTimerUiFromSnapshot(snap, prefs)
        }
    }

    fun startOrResumeTimer() {
        viewModelScope.launch {
            timerMutex.withLock {
                if (latestSnapshot.isRunning) return@withLock
                val next = latestSnapshot.copy(
                    isRunning = true,
                    startWallMs = System.currentTimeMillis()
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            timerMutex.withLock {
                val snap = latestSnapshot
                if (!snap.isRunning) return@withLock
                val now = System.currentTimeMillis()
                val totalElapsed = snap.accumulatedMs + (now - snap.startWallMs).coerceAtLeast(0L)
                val next = snap.copy(
                    isRunning = false,
                    startWallMs = 0L,
                    accumulatedMs = totalElapsed
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            timerMutex.withLock {
                val next = latestSnapshot.copy(
                    isRunning = false,
                    startWallMs = 0L,
                    accumulatedMs = 0L
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun finishAndSaveSession() {
        viewModelScope.launch {
            timerMutex.withLock {
                val snap = latestSnapshot
                val now = System.currentTimeMillis()
                val totalElapsedMs = if (snap.isRunning) {
                    snap.accumulatedMs + (now - snap.startWallMs).coerceAtLeast(0L)
                } else {
                    snap.accumulatedMs
                }
                val durationSeconds = totalElapsedMs / 1000L
                if (durationSeconds < 10L) {
                    _snackbarEvents.tryEmit("Study at least 10 seconds to record a session.")
                    return@withLock
                }
                if (snap.mode == StudyMode.POMODORO && snap.phase != PomodoroPhase.FOCUS) {
                    _snackbarEvents.tryEmit("Break timers aren't logged as study hours. Switch to Focus or Stopwatch.")
                    return@withLock
                }

                repository.insertStudySession(
                    StudySessionEntity(
                        subject = snap.subject.name,
                        topicId = snap.topicId,
                        topicTitle = snap.topicTitle,
                        mode = snap.mode.name,
                        durationSeconds = durationSeconds,
                        startedAtMs = now - totalElapsedMs,
                        endedAtMs = now,
                        dateEpochDay = DateUtils.epochMillisToLocalEpochDay(now)
                    )
                )
                val next = snap.copy(
                    isRunning = false,
                    startWallMs = 0L,
                    accumulatedMs = 0L
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
                _snackbarEvents.tryEmit(
                    "Saved ${DateUtils.formatDurationHms(totalElapsedMs)} for ${snap.subject.shortName}."
                )
            }
        }
    }

    fun setTimerMode(mode: StudyMode) {
        viewModelScope.launch {
            timerMutex.withLock {
                if (latestSnapshot.mode == mode) return@withLock
                val next = latestSnapshot.copy(
                    isRunning = false,
                    mode = mode,
                    phase = PomodoroPhase.FOCUS,
                    startWallMs = 0L,
                    accumulatedMs = 0L
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun setPomodoroPhase(phase: PomodoroPhase) {
        viewModelScope.launch {
            timerMutex.withLock {
                val next = latestSnapshot.copy(
                    isRunning = false,
                    mode = StudyMode.POMODORO,
                    phase = phase,
                    startWallMs = 0L,
                    accumulatedMs = 0L
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun selectTimerSubject(subject: Subject) {
        viewModelScope.launch {
            timerMutex.withLock {
                val next = latestSnapshot.copy(
                    subject = subject,
                    topicId = null,
                    topicTitle = null
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun selectTimerTopic(topic: SyllabusTopicEntity?) {
        viewModelScope.launch {
            timerMutex.withLock {
                val next = latestSnapshot.copy(
                    topicId = topic?.id,
                    topicTitle = topic?.title
                )
                latestSnapshot = next
                repository.saveTimerSnapshot(next)
                syncTimerUiFromSnapshot(next, latestPrefs)
            }
        }
    }

    fun updatePomodoroDurations(focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) {
        viewModelScope.launch {
            repository.updatePomodoroDurations(focusMinutes, shortBreakMinutes, longBreakMinutes)
            _snackbarEvents.tryEmit("Pomodoro durations updated.")
        }
    }

    fun deleteStudySession(id: Long) {
        viewModelScope.launch {
            repository.deleteStudySession(id)
            _snackbarEvents.tryEmit("Study session removed.")
        }
    }

    // Syllabus CRUD
    fun saveTopic(
        id: Long = 0L,
        subject: Subject,
        title: String,
        status: TopicStatus,
        priority: TopicPriority,
        notes: String,
        targetDateEpochDay: Long?
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            val existing = uiState.value.topics.firstOrNull { it.id == id }
            repository.saveTopic(
                SyllabusTopicEntity(
                    id = id,
                    subject = subject.name,
                    title = cleanTitle,
                    status = status.name,
                    priority = priority.name,
                    notes = notes.trim(),
                    targetDateEpochDay = targetDateEpochDay,
                    createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis(),
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            _snackbarEvents.tryEmit(if (id == 0L) "Topic added to ${subject.shortName}." else "Topic updated.")
        }
    }

    fun updateTopicStatus(topic: SyllabusTopicEntity, newStatus: TopicStatus) {
        viewModelScope.launch {
            repository.saveTopic(topic.copy(status = newStatus.name))
        }
    }

    fun deleteTopic(id: Long) {
        viewModelScope.launch {
            repository.deleteTopic(id)
            _snackbarEvents.tryEmit("Topic deleted.")
        }
    }

    fun importSyllabus(rawInput: String, fallbackSubject: Subject) {
        viewModelScope.launch {
            val count = repository.importSyllabusTopics(rawInput, fallbackSubject)
            if (count > 0) {
                _snackbarEvents.tryEmit("Imported $count topics.")
            } else {
                _snackbarEvents.tryEmit("No valid topics found to import.")
            }
        }
    }

    // Daily Tasks CRUD
    fun saveDailyTask(
        id: Long = 0L,
        title: String,
        subject: Subject?,
        priority: TopicPriority,
        dateEpochDay: Long,
        isCompleted: Boolean = false
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            val existing = uiState.value.dailyTasks.firstOrNull { it.id == id }
            repository.saveDailyTask(
                DailyTaskEntity(
                    id = id,
                    title = cleanTitle,
                    subject = subject?.name,
                    priority = priority.name,
                    dateEpochDay = dateEpochDay,
                    isCompleted = isCompleted,
                    createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()
                )
            )
            _snackbarEvents.tryEmit(if (id == 0L) "Task added." else "Task updated.")
        }
    }

    fun toggleDailyTask(task: DailyTaskEntity) {
        viewModelScope.launch {
            repository.saveDailyTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteDailyTask(id: Long) {
        viewModelScope.launch {
            repository.deleteDailyTask(id)
            _snackbarEvents.tryEmit("Task deleted.")
        }
    }

    // MCQ Practice CRUD
    fun addMcqSession(
        subject: Subject,
        topicName: String,
        attempted: Int,
        correct: Int,
        incorrect: Int,
        unattempted: Int,
        timeTakenMinutes: Int,
        dateEpochDay: Long
    ) {
        if (attempted <= 0) return
        val accuracy = (correct.toFloat() / attempted.toFloat()) * 100f
        viewModelScope.launch {
            repository.insertMcqSession(
                McqSessionEntity(
                    subject = subject.name,
                    topicName = topicName.trim().ifEmpty { "General Practice" },
                    attempted = attempted,
                    correct = correct,
                    incorrect = incorrect,
                    unattempted = unattempted,
                    timeTakenMinutes = timeTakenMinutes,
                    accuracyPercent = accuracy,
                    dateEpochDay = dateEpochDay
                )
            )
            _snackbarEvents.tryEmit("MCQ practice logged (${String.format("%.1f", accuracy)}% accuracy).")
        }
    }

    fun deleteMcqSession(id: Long) {
        viewModelScope.launch {
            repository.deleteMcqSession(id)
            _snackbarEvents.tryEmit("MCQ record deleted.")
        }
    }

    // Mock Test CRUD
    fun saveMockTest(
        id: Long = 0L,
        title: String,
        testDateEpochDay: Long,
        mathMarks: Float,
        mathMaxMarks: Float,
        physicsMarks: Float,
        physicsMaxMarks: Float,
        chemistryMarks: Float,
        chemistryMaxMarks: Float,
        eceMarks: Float,
        eceMaxMarks: Float,
        attempted: Int,
        correct: Int,
        incorrect: Int,
        notes: String
    ) {
        val total = mathMarks + physicsMarks + chemistryMarks + eceMarks
        val maxTotal = (mathMaxMarks + physicsMaxMarks + chemistryMaxMarks + eceMaxMarks).coerceAtLeast(1f)
        viewModelScope.launch {
            val existing = uiState.value.mockTests.firstOrNull { it.id == id }
            repository.saveMockTest(
                MockTestEntity(
                    id = id,
                    title = title.trim().ifEmpty { "Mock Test" },
                    testDateEpochDay = testDateEpochDay,
                    mathMarks = mathMarks,
                    mathMaxMarks = mathMaxMarks.coerceAtLeast(1f),
                    physicsMarks = physicsMarks,
                    physicsMaxMarks = physicsMaxMarks.coerceAtLeast(1f),
                    chemistryMarks = chemistryMarks,
                    chemistryMaxMarks = chemistryMaxMarks.coerceAtLeast(1f),
                    eceMarks = eceMarks,
                    eceMaxMarks = eceMaxMarks.coerceAtLeast(1f),
                    totalMarks = total,
                    maxTotalMarks = maxTotal,
                    attempted = attempted,
                    correct = correct,
                    incorrect = incorrect,
                    notes = notes.trim(),
                    createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()
                )
            )
            _snackbarEvents.tryEmit(if (id == 0L) "Mock test saved." else "Mock test updated.")
        }
    }

    fun deleteMockTest(id: Long) {
        viewModelScope.launch {
            repository.deleteMockTest(id)
            _snackbarEvents.tryEmit("Mock test deleted.")
        }
    }

    // Profile & Preferences
    fun updateProfile(name: String, branch: String, targetExam: String, dailyTargetMinutes: Int) {
        viewModelScope.launch {
            repository.updateProfile(name, branch, targetExam, dailyTargetMinutes)
            _snackbarEvents.tryEmit("Profile saved.")
        }
    }

    fun updateExamDate(epochDay: Long?) {
        viewModelScope.launch {
            repository.updateExamDate(epochDay)
            _snackbarEvents.tryEmit(
                if (epochDay != null) "Exam date set to ${DateUtils.formatEpochDay(epochDay)}."
                else "Exam date cleared."
            )
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }

    fun updateNotificationPreferences(
        remindersEnabled: Boolean,
        pomodoroAlertsEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(
                remindersEnabled,
                pomodoroAlertsEnabled,
                reminderHour,
                reminderMinute
            )
            _snackbarEvents.tryEmit("Notification preferences updated.")
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(rawJson)
            result.onSuccess { message ->
                _snackbarEvents.tryEmit(message)
            }.onFailure { err ->
                _snackbarEvents.tryEmit("Restore failed: ${err.message ?: "Invalid JSON format"}")
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            timerMutex.withLock {
                latestSnapshot = PersistedTimerSnapshot()
                repository.resetAllData()
                syncTimerUiFromSnapshot(latestSnapshot, UserPreferences())
                _snackbarEvents.tryEmit("All study data has been reset.")
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    // Syllabus Square Photo Box
    fun setSyllabusPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val destPath = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val outFile = File(app.filesDir, "syllabus_photo_${System.currentTimeMillis()}.jpg")
                    app.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("syllabus_photo_") && file.absolutePath != outFile.absolutePath) {
                            file.delete()
                        }
                    }
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Could not open selected image.")
                    outFile.absolutePath
                }
                repository.updateSyllabusPhoto(destPath, 0)
                _snackbarEvents.tryEmit("Syllabus photo updated.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VIEWMODEL_TAG, "Failed to save syllabus photo", e)
                _snackbarEvents.tryEmit("Could not load photo: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun rotateSyllabusPhoto() {
        val currentPath = latestPrefs.syllabusPhotoPath ?: return
        val nextRotation = (latestPrefs.syllabusPhotoRotationDeg + 90) % 360
        viewModelScope.launch {
            repository.updateSyllabusPhoto(currentPath, nextRotation)
        }
    }

    fun removeSyllabusPhoto() {
        val currentPath = latestPrefs.syllabusPhotoPath
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (!currentPath.isNullOrBlank()) {
                    runCatching { File(currentPath).delete() }
                }
            }
            repository.updateSyllabusPhoto(null, 0)
            _snackbarEvents.tryEmit("Syllabus photo removed.")
        }
    }

    // AI Summary & Follow-up Q&A
    fun setSummarySubject(subject: Subject) {
        _summarySubject.value = subject
    }

    fun setSummaryTopicInput(input: String) {
        _summaryTopicInput.value = input
    }

    fun prefillSummaryTopic(subject: Subject, topicTitle: String) {
        _summarySubject.value = subject
        _summaryTopicInput.value = topicTitle
    }

    fun clearAttachedStudyFile() {
        _attachedStudyFile.value = null
    }

    fun attachStudyFileFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val attached = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    var displayName = "study_file"
                    var reportedSize = -1L
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) reportedSize = cursor.getLong(sizeIdx)
                        }
                    }
                    if (reportedSize > MAX_ATTACHMENT_BYTES) {
                        throw IllegalArgumentException("File is larger than 18 MB. Please pick a smaller PDF, image, or note.")
                    }
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Unable to read selected file.")
                    if (bytes.size.toLong() > MAX_ATTACHMENT_BYTES) {
                        throw IllegalArgumentException("File exceeds 18 MB size limit.")
                    }

                    val rawMime = resolver.getType(uri).orEmpty().ifBlank {
                        guessMimeTypeFromName(displayName)
                    }
                    val textContent = if (
                        rawMime.startsWith("text/") ||
                        rawMime == "application/json" ||
                        displayName.endsWith(".txt", ignoreCase = true) ||
                        displayName.endsWith(".md", ignoreCase = true) ||
                        displayName.endsWith(".csv", ignoreCase = true) ||
                        displayName.endsWith(".json", ignoreCase = true)
                    ) {
                        bytes.decodeToString()
                    } else {
                        null
                    }
                    AttachedStudyFile(
                        displayName = displayName,
                        mimeType = rawMime,
                        sizeBytes = bytes.size.toLong(),
                        bytes = bytes,
                        textContent = textContent
                    )
                }
                _attachedStudyFile.value = attached
                _snackbarEvents.tryEmit("Attached ${attached.displayName}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VIEWMODEL_TAG, "Failed to attach file", e)
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to attach file.")
            }
        }
    }

    private fun guessMimeTypeFromName(name: String): String {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".txt") || lower.endsWith(".md") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".json") -> "application/json"
            else -> "application/octet-stream"
        }
    }

    fun generateElectronicsSummary() {
        val topicPrompt = _summaryTopicInput.value.trim()
        val file = _attachedStudyFile.value
        val subject = _summarySubject.value
        if (topicPrompt.isEmpty() && file == null) {
            _summaryUiState.value = AiSummaryUiState.Error("Enter an electronics topic or attach a PDF, image, or text note first.")
            return
        }

        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _summaryUiState.value = AiSummaryUiState.Loading
            _qaMessages.value = emptyList()
            _qaError.value = null
            var latestText = ""
            try {
                electronicsAiService.summarizeStream(
                    subject = subject,
                    topicPrompt = topicPrompt,
                    attachedFile = file
                ).collect { streamedText ->
                    latestText = streamedText
                    _summaryUiState.value = AiSummaryUiState.Streaming(streamedText)
                }
                if (latestText.isNotBlank()) {
                    _summaryUiState.value = AiSummaryUiState.Success(latestText)
                } else {
                    _summaryUiState.value = AiSummaryUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VIEWMODEL_TAG, "Summary generation timed out", e)
                _summaryUiState.value = AiSummaryUiState.Error("Request timed out after 60 seconds. Check your internet connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VIEWMODEL_TAG, "Summary generation failed", e)
                _summaryUiState.value = AiSummaryUiState.Error(
                    e.localizedMessage ?: "Gemini summary failed. Verify network connection and Firebase App Check."
                )
            }
        }
    }

    fun askElectronicsQuestion(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty()) return

        val subject = _summarySubject.value
        val file = _attachedStudyFile.value
        val contextTitle = buildString {
            val t = _summaryTopicInput.value.trim()
            if (t.isNotEmpty()) append(t)
            if (file != null) {
                if (isNotEmpty()) append(" · ")
                append(file.displayName)
            }
        }
        val summaryText = when (val s = _summaryUiState.value) {
            is AiSummaryUiState.Success -> s.text
            is AiSummaryUiState.Streaming -> s.text
            else -> ""
        }

        val prior = _qaMessages.value
        val userMsgId = "u_${System.currentTimeMillis()}"
        val modelMsgId = "m_${System.currentTimeMillis() + 1}"
        _qaError.value = null
        _isQaLoading.value = true
        _qaMessages.value = prior +
            AiChatMessage(id = userMsgId, isUser = true, text = cleanQuestion) +
            AiChatMessage(id = modelMsgId, isUser = false, text = "", isStreaming = true)

        qaJob?.cancel()
        qaJob = viewModelScope.launch {
            var latestAnswer = ""
            try {
                electronicsAiService.askFollowUpStream(
                    subject = subject,
                    contextTitle = contextTitle,
                    contextSummary = summaryText,
                    attachedFile = file,
                    priorMessages = prior,
                    question = cleanQuestion
                ).collect { chunk ->
                    latestAnswer = chunk
                    _qaMessages.value = _qaMessages.value.map { msg ->
                        if (msg.id == modelMsgId) msg.copy(text = chunk, isStreaming = true) else msg
                    }
                }
                _qaMessages.value = _qaMessages.value.map { msg ->
                    if (msg.id == modelMsgId) msg.copy(text = latestAnswer, isStreaming = false) else msg
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VIEWMODEL_TAG, "Q&A timed out", e)
                _qaMessages.value = _qaMessages.value.filterNot { it.id == modelMsgId && it.text.isBlank() }
                _qaError.value = "Question timed out after 60 seconds. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VIEWMODEL_TAG, "Q&A failed", e)
                _qaMessages.value = _qaMessages.value.filterNot { it.id == modelMsgId && it.text.isBlank() }
                _qaError.value = e.localizedMessage ?: "Failed to get answer from Gemini."
            } finally {
                _isQaLoading.value = false
            }
        }
    }

    fun saveCurrentSummaryToLibrary() {
        val markdown = when (val s = _summaryUiState.value) {
            is AiSummaryUiState.Success -> s.text
            else -> return
        }.trim()
        if (markdown.isEmpty()) return

        val file = _attachedStudyFile.value
        val topicText = _summaryTopicInput.value.trim()
        val sourceTitle = when {
            topicText.isNotEmpty() && file != null -> "$topicText (${file.displayName})"
            topicText.isNotEmpty() -> topicText
            file != null -> file.displayName
            else -> "Electronics Note"
        }
        val sourceType = if (file != null) "FILE" else "TOPIC"

        viewModelScope.launch {
            repository.saveAiSummary(
                AiSummaryEntity(
                    subject = _summarySubject.value.name,
                    sourceTitle = sourceTitle,
                    sourceType = sourceType,
                    summaryMarkdown = markdown
                )
            )
            _snackbarEvents.tryEmit("Summary saved offline.")
        }
    }

    fun loadSavedSummary(entity: AiSummaryEntity) {
        _summarySubject.value = Subject.fromName(entity.subject)
        _summaryTopicInput.value = entity.sourceTitle
        _attachedStudyFile.value = null
        _summaryUiState.value = AiSummaryUiState.Success(entity.summaryMarkdown)
        _qaMessages.value = emptyList()
        _qaError.value = null
    }

    fun deleteAiSummary(id: Long) {
        viewModelScope.launch {
            repository.deleteAiSummary(id)
            _snackbarEvents.tryEmit("Saved summary deleted.")
        }
    }

    private fun buildUiState(
        prefs: UserPreferences,
        topics: List<SyllabusTopicEntity>,
        sessions: List<StudySessionEntity>,
        tasks: List<DailyTaskEntity>,
        mcqs: List<McqSessionEntity>,
        mocks: List<MockTestEntity>,
        summaries: List<AiSummaryEntity>
    ): EcetUiState {
        val today = DateUtils.todayEpochDay()

        val subjectSummaries = Subject.entries.map { subject ->
            val subTopics = topics.filter { it.subject == subject.name }
            val completed = subTopics.count { it.status == TopicStatus.COMPLETED.name }
            val inProgress = subTopics.count { it.status == TopicStatus.IN_PROGRESS.name }
            val notStarted = subTopics.count { it.status == TopicStatus.NOT_STARTED.name }
            val total = subTopics.size
            val pending = inProgress + notStarted
            val pct = if (total > 0) (completed * 100f) / total else 0f
            SubjectSummary(
                subject = subject,
                totalTopics = total,
                completedTopics = completed,
                inProgressTopics = inProgress,
                notStartedTopics = notStarted,
                pendingTopics = pending,
                completionPercent = pct
            )
        }

        val totalTopics = topics.size
        val completedTopics = topics.count { it.status == TopicStatus.COMPLETED.name }
        val inProgressTopics = topics.count { it.status == TopicStatus.IN_PROGRESS.name }
        val pendingTopics = totalTopics - completedTopics
        val overallPct = if (totalTopics > 0) (completedTopics * 100f) / totalTopics else 0f

        val overview = PreparationOverview(
            totalTopics = totalTopics,
            completedTopics = completedTopics,
            inProgressTopics = inProgressTopics,
            pendingTopics = pendingTopics,
            completionPercent = overallPct,
            subjectSummaries = subjectSummaries
        )

        val totalSec = sessions.sumOf { it.durationSeconds }
        val todaySec = sessions.filter { it.dateEpochDay == today }.sumOf { it.durationSeconds }
        val weekStart = today - 6L
        val weeklySec = sessions.filter { it.dateEpochDay in weekStart..today }.sumOf { it.durationSeconds }
        val targetSec = (prefs.dailyStudyTargetMinutes * 60f).coerceAtLeast(60f)
        val dailyTargetPct = (todaySec / targetSec) * 100f

        val studyStats = StudyStats(
            totalStudySeconds = totalSec,
            todayStudySeconds = todaySec,
            weeklyStudySeconds = weeklySec,
            dailyTargetMinutes = prefs.dailyStudyTargetMinutes,
            dailyTargetPercent = dailyTargetPct
        )

        val subjectMcqs = Subject.entries.map { subject ->
            val list = mcqs.filter { it.subject == subject.name }
            val attempted = list.sumOf { it.attempted }
            val correct = list.sumOf { it.correct }
            val incorrect = list.sumOf { it.incorrect }
            val unattempted = list.sumOf { it.unattempted }
            val acc = if (attempted > 0) (correct * 100f) / attempted else 0f
            SubjectMcqPerformance(
                subject = subject,
                sessionsCount = list.size,
                attempted = attempted,
                correct = correct,
                incorrect = incorrect,
                unattempted = unattempted,
                accuracyPercent = acc
            )
        }

        val topicMcqs = mcqs
            .groupBy { Subject.fromName(it.subject) to it.topicName.trim() }
            .map { (key, list) ->
                val attempted = list.sumOf { it.attempted }
                val correct = list.sumOf { it.correct }
                val acc = if (attempted > 0) (correct * 100f) / attempted else 0f
                TopicMcqPerformance(
                    subject = key.first,
                    topicName = key.second,
                    sessionsCount = list.size,
                    attempted = attempted,
                    correct = correct,
                    accuracyPercent = acc
                )
            }
            .sortedByDescending { it.attempted }

        // Analytics calculations
        val sessionsByDay = sessions.groupBy { it.dateEpochDay }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.durationSeconds } }
        val completedTasksByDay = tasks.filter { it.isCompleted }.groupBy { it.dateEpochDay }
            .mapValues { (_, dayTasks) -> dayTasks.size }

        val activeDays = (sessionsByDay.filterValues { it > 0L }.keys + completedTasksByDay.keys).toSortedSet()

        // Streak calculation
        var currentStreak = 0
        var checkDay = if (activeDays.contains(today)) today else today - 1L
        while (activeDays.contains(checkDay)) {
            currentStreak++
            checkDay--
        }

        var bestStreak = 0
        var running = 0
        var prevDay: Long? = null
        activeDays.forEach { day ->
            running = if (prevDay != null && day == prevDay!! + 1L) running + 1 else 1
            if (running > bestStreak) bestStreak = running
            prevDay = day
        }

        val activeLast30 = ((today - 29L)..today).count { activeDays.contains(it) }

        val weeklyStudyDays = DateUtils.lastNDays(7, today).map { day ->
            val sec = sessionsByDay[day] ?: 0L
            DayStudyMetric(
                epochDay = day,
                label = DateUtils.formatWeekdayShort(day).take(3),
                studyHours = sec / 3600f,
                studyMinutes = (sec / 60L).toInt(),
                tasksCompleted = completedTasksByDay[day] ?: 0
            )
        }

        // 4 weekly buckets over last 28 days for Monthly Study Hours chart
        val monthlyBuckets = (0..3).map { weekOffset ->
            val endDay = today - ((3 - weekOffset) * 7L)
            val startDay = endDay - 6L
            val sec = (startDay..endDay).sumOf { sessionsByDay[it] ?: 0L }
            val completedCount = (startDay..endDay).sumOf { completedTasksByDay[it] ?: 0 }
            val label = if (weekOffset == 3) "This Wk" else DateUtils.formatShortDate(startDay)
            DayStudyMetric(
                epochDay = startDay,
                label = label,
                studyHours = sec / 3600f,
                studyMinutes = (sec / 60L).toInt(),
                tasksCompleted = completedCount
            )
        }

        val heatmapDays = DateUtils.lastNDays(84, today).map { day ->
            val sec = sessionsByDay[day] ?: 0L
            DayStudyMetric(
                epochDay = day,
                label = DateUtils.formatShortDate(day),
                studyHours = sec / 3600f,
                studyMinutes = (sec / 60L).toInt(),
                tasksCompleted = completedTasksByDay[day] ?: 0
            )
        }

        val mcqAccuracySeries = mcqs
            .sortedWith(compareBy<McqSessionEntity> { it.dateEpochDay }.thenBy { it.createdAtMs })
            .takeLast(12)
            .map { DateUtils.formatShortDate(it.dateEpochDay) to it.accuracyPercent }

        val mockScoreSeries = mocks
            .sortedWith(compareBy<MockTestEntity> { it.testDateEpochDay }.thenBy { it.createdAtMs })
            .takeLast(12)
            .map {
                val pct = if (it.maxTotalMarks > 0f) (it.totalMarks / it.maxTotalMarks) * 100f else 0f
                DateUtils.formatShortDate(it.testDateEpochDay) to pct
            }

        val analytics = AnalyticsState(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            activeDaysLast30 = activeLast30,
            weeklyStudyDays = weeklyStudyDays,
            monthlyStudyBuckets = monthlyBuckets,
            heatmapDays = heatmapDays,
            subjectCompletions = subjectSummaries,
            mcqAccuracySeries = mcqAccuracySeries,
            mockScoreSeries = mockScoreSeries
        )

        return EcetUiState(
            preferences = prefs,
            topics = topics,
            studySessions = sessions,
            dailyTasks = tasks,
            mcqSessions = mcqs,
            mockTests = mocks,
            aiSummaries = summaries,
            overview = overview,
            studyStats = studyStats,
            subjectMcqPerformance = subjectMcqs,
            topicMcqPerformance = topicMcqs,
            analytics = analytics,
            todayEpochDay = today
        )
    }

    private data class BaseTuple(
        val prefs: UserPreferences,
        val topics: List<SyllabusTopicEntity>,
        val sessions: List<StudySessionEntity>,
        val tasks: List<DailyTaskEntity>,
        val mcqs: List<McqSessionEntity>
    )
}
