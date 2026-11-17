package com.example.data.repository

import com.example.data.local.BackupJsonSerializer
import com.example.data.local.DailyTaskEntity
import com.example.data.local.EcetDao
import com.example.data.local.McqSessionEntity
import com.example.data.local.MockTestEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.StudySessionEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.Subject
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EcetRepository(
    private val dao: EcetDao,
    private val preferencesManager: PreferencesManager
) {
    val topicsFlow: Flow<List<SyllabusTopicEntity>> = dao.observeTopics()
    val studySessionsFlow: Flow<List<StudySessionEntity>> = dao.observeStudySessions()
    val dailyTasksFlow: Flow<List<DailyTaskEntity>> = dao.observeDailyTasks()
    val mcqSessionsFlow: Flow<List<McqSessionEntity>> = dao.observeMcqSessions()
    val mockTestsFlow: Flow<List<MockTestEntity>> = dao.observeMockTests()
    val preferencesFlow: Flow<UserPreferences> = preferencesManager.userPreferencesFlow
    val timerSnapshotFlow: Flow<PersistedTimerSnapshot> = preferencesManager.timerSnapshotFlow

    // Syllabus Topics
    suspend fun saveTopic(topic: SyllabusTopicEntity): Long {
        return if (topic.id == 0L) {
            dao.insertTopic(topic)
        } else {
            dao.updateTopic(topic.copy(updatedAtMs = System.currentTimeMillis()))
            topic.id
        }
    }

    suspend fun importSyllabusTopics(rawInput: String, fallbackSubject: Subject): Int {
        val parsed = BackupJsonSerializer.parseSyllabusImportInput(rawInput, fallbackSubject)
        if (parsed.isNotEmpty()) {
            dao.insertTopics(parsed)
        }
        return parsed.size
    }

    suspend fun deleteTopic(id: Long) = dao.deleteTopicById(id)

    // Study Sessions
    suspend fun insertStudySession(session: StudySessionEntity): Long = dao.insertStudySession(session)

    suspend fun deleteStudySession(id: Long) = dao.deleteStudySessionById(id)

    // Daily Tasks
    suspend fun saveDailyTask(task: DailyTaskEntity): Long {
        return if (task.id == 0L) {
            dao.insertDailyTask(task)
        } else {
            dao.updateDailyTask(task)
            task.id
        }
    }

    suspend fun deleteDailyTask(id: Long) = dao.deleteDailyTaskById(id)

    // MCQ Sessions
    suspend fun insertMcqSession(session: McqSessionEntity): Long = dao.insertMcqSession(session)

    suspend fun deleteMcqSession(id: Long) = dao.deleteMcqSessionById(id)

    // Mock Tests
    suspend fun saveMockTest(test: MockTestEntity): Long {
        return if (test.id == 0L) {
            dao.insertMockTest(test)
        } else {
            dao.updateMockTest(test)
            test.id
        }
    }

    suspend fun deleteMockTest(id: Long) = dao.deleteMockTestById(id)

    // Preferences & Timer
    suspend fun updateProfile(
        studentName: String,
        branch: String,
        targetExam: String,
        dailyStudyTargetMinutes: Int
    ) = preferencesManager.updateProfile(studentName, branch, targetExam, dailyStudyTargetMinutes)

    suspend fun updateExamDate(epochDay: Long?) = preferencesManager.updateExamDate(epochDay)

    suspend fun updateThemeMode(themeMode: ThemeMode) = preferencesManager.updateThemeMode(themeMode)

    suspend fun updateNotificationPreferences(
        remindersEnabled: Boolean,
        pomodoroAlertsEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) = preferencesManager.updateNotificationPreferences(
        remindersEnabled,
        pomodoroAlertsEnabled,
        reminderHour,
        reminderMinute
    )

    suspend fun updatePomodoroDurations(focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) =
        preferencesManager.updatePomodoroDurations(focusMinutes, shortBreakMinutes, longBreakMinutes)

    suspend fun saveTimerSnapshot(snapshot: PersistedTimerSnapshot) =
        preferencesManager.saveTimerSnapshot(snapshot)

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val prefs = preferencesFlow.first()
        val topics = topicsFlow.first()
        val sessions = studySessionsFlow.first()
        val tasks = dailyTasksFlow.first()
        val mcqs = mcqSessionsFlow.first()
        val mocks = mockTestsFlow.first()
        return BackupJsonSerializer.exportToJson(prefs, topics, sessions, tasks, mcqs, mocks)
    }

    suspend fun restoreBackupJson(rawJson: String): Result<String> {
        val parsed = BackupJsonSerializer.importFromJson(rawJson)
        return parsed.map { payload ->
            dao.replaceAllData(
                topics = payload.topics,
                sessions = payload.studySessions,
                tasks = payload.dailyTasks,
                mcqs = payload.mcqSessions,
                mocks = payload.mockTests
            )
            preferencesManager.restorePreferences(payload.preferences)
            "Restored ${payload.topics.size} topics, ${payload.studySessions.size} sessions, ${payload.dailyTasks.size} tasks, ${payload.mcqSessions.size} MCQ logs, and ${payload.mockTests.size} mock tests."
        }
    }

    suspend fun resetAllData() {
        dao.clearAllTables()
        preferencesManager.resetAllPreferences()
    }
}
