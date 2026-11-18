package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.data.model.StudyMode
import com.example.data.model.Subject
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ecetDataStore: DataStore<Preferences> by preferencesDataStore(name = "ecet_tracker_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val BRANCH = stringPreferencesKey("branch")
        val TARGET_EXAM = stringPreferencesKey("target_exam")
        val EXAM_DATE_EPOCH_DAY = longPreferencesKey("exam_date_epoch_day")
        val DAILY_TARGET_MINUTES = intPreferencesKey("daily_target_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val POMODORO_ALERTS_ENABLED = booleanPreferencesKey("pomodoro_alerts_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_SHORT_BREAK_MINUTES = intPreferencesKey("pomodoro_short_break_minutes")
        val POMODORO_LONG_BREAK_MINUTES = intPreferencesKey("pomodoro_long_break_minutes")
        val SYLLABUS_PHOTO_PATH = stringPreferencesKey("syllabus_photo_path")
        val SYLLABUS_PHOTO_ROTATION_DEG = intPreferencesKey("syllabus_photo_rotation_deg")

        // Timer persistence
        val TIMER_RUNNING = booleanPreferencesKey("timer_running")
        val TIMER_MODE = stringPreferencesKey("timer_mode")
        val TIMER_PHASE = stringPreferencesKey("timer_phase")
        val TIMER_START_WALL_MS = longPreferencesKey("timer_start_wall_ms")
        val TIMER_ACCUMULATED_MS = longPreferencesKey("timer_accumulated_ms")
        val TIMER_SUBJECT = stringPreferencesKey("timer_subject")
        val TIMER_TOPIC_ID = longPreferencesKey("timer_topic_id")
        val TIMER_TOPIC_TITLE = stringPreferencesKey("timer_topic_title")
        val TIMER_POMODORO_ROUND = intPreferencesKey("timer_pomodoro_round")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.ecetDataStore.data.map { prefs ->
        UserPreferences(
            studentName = prefs[Keys.STUDENT_NAME] ?: "ECET Aspirant",
            branch = prefs[Keys.BRANCH] ?: "ECE (Electronics & Communication)",
            targetExam = prefs[Keys.TARGET_EXAM] ?: "TG ECET 2027",
            examDateEpochDay = prefs[Keys.EXAM_DATE_EPOCH_DAY],
            dailyStudyTargetMinutes = prefs[Keys.DAILY_TARGET_MINUTES] ?: 240,
            themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            pomodoroAlertsEnabled = prefs[Keys.POMODORO_ALERTS_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 19,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            pomodoroFocusMinutes = (prefs[Keys.POMODORO_FOCUS_MINUTES] ?: 25).coerceIn(1, 180),
            pomodoroShortBreakMinutes = (prefs[Keys.POMODORO_SHORT_BREAK_MINUTES] ?: 5).coerceIn(1, 60),
            pomodoroLongBreakMinutes = (prefs[Keys.POMODORO_LONG_BREAK_MINUTES] ?: 15).coerceIn(1, 90),
            syllabusPhotoPath = prefs[Keys.SYLLABUS_PHOTO_PATH],
            syllabusPhotoRotationDeg = (((prefs[Keys.SYLLABUS_PHOTO_ROTATION_DEG] ?: 0) % 360) + 360) % 360
        )
    }

    val timerSnapshotFlow: Flow<PersistedTimerSnapshot> = context.ecetDataStore.data.map { prefs ->
        PersistedTimerSnapshot(
            isRunning = prefs[Keys.TIMER_RUNNING] ?: false,
            mode = StudyMode.fromName(prefs[Keys.TIMER_MODE]),
            phase = PomodoroPhase.fromName(prefs[Keys.TIMER_PHASE]),
            startWallMs = prefs[Keys.TIMER_START_WALL_MS] ?: 0L,
            accumulatedMs = prefs[Keys.TIMER_ACCUMULATED_MS] ?: 0L,
            subject = Subject.fromName(prefs[Keys.TIMER_SUBJECT]),
            topicId = prefs[Keys.TIMER_TOPIC_ID],
            topicTitle = prefs[Keys.TIMER_TOPIC_TITLE],
            pomodoroRound = (prefs[Keys.TIMER_POMODORO_ROUND] ?: 1).coerceAtLeast(1)
        )
    }

    suspend fun updateProfile(
        studentName: String,
        branch: String,
        targetExam: String,
        dailyStudyTargetMinutes: Int
    ) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = studentName.trim().ifEmpty { "ECET Aspirant" }
            prefs[Keys.BRANCH] = branch.trim().ifEmpty { "ECE (Electronics & Communication)" }
            prefs[Keys.TARGET_EXAM] = targetExam.trim().ifEmpty { "TG ECET 2027" }
            prefs[Keys.DAILY_TARGET_MINUTES] = dailyStudyTargetMinutes.coerceIn(15, 960)
        }
    }

    suspend fun updateExamDate(epochDay: Long?) {
        context.ecetDataStore.edit { prefs ->
            if (epochDay == null) {
                prefs.remove(Keys.EXAM_DATE_EPOCH_DAY)
            } else {
                prefs[Keys.EXAM_DATE_EPOCH_DAY] = epochDay
            }
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateNotificationPreferences(
        remindersEnabled: Boolean,
        pomodoroAlertsEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.REMINDERS_ENABLED] = remindersEnabled
            prefs[Keys.POMODORO_ALERTS_ENABLED] = pomodoroAlertsEnabled
            prefs[Keys.REMINDER_HOUR] = reminderHour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = reminderMinute.coerceIn(0, 59)
        }
    }

    suspend fun updatePomodoroDurations(focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.POMODORO_FOCUS_MINUTES] = focusMinutes.coerceIn(1, 180)
            prefs[Keys.POMODORO_SHORT_BREAK_MINUTES] = shortBreakMinutes.coerceIn(1, 60)
            prefs[Keys.POMODORO_LONG_BREAK_MINUTES] = longBreakMinutes.coerceIn(1, 90)
        }
    }

    suspend fun updateSyllabusPhoto(path: String?, rotationDeg: Int) {
        val normalizedRotation = ((rotationDeg % 360) + 360) % 360
        context.ecetDataStore.edit { prefs ->
            if (path.isNullOrBlank()) {
                prefs.remove(Keys.SYLLABUS_PHOTO_PATH)
                prefs[Keys.SYLLABUS_PHOTO_ROTATION_DEG] = 0
            } else {
                prefs[Keys.SYLLABUS_PHOTO_PATH] = path
                prefs[Keys.SYLLABUS_PHOTO_ROTATION_DEG] = normalizedRotation
            }
        }
    }

    suspend fun saveTimerSnapshot(snapshot: PersistedTimerSnapshot) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.TIMER_RUNNING] = snapshot.isRunning
            prefs[Keys.TIMER_MODE] = snapshot.mode.name
            prefs[Keys.TIMER_PHASE] = snapshot.phase.name
            prefs[Keys.TIMER_START_WALL_MS] = snapshot.startWallMs
            prefs[Keys.TIMER_ACCUMULATED_MS] = snapshot.accumulatedMs
            prefs[Keys.TIMER_SUBJECT] = snapshot.subject.name
            if (snapshot.topicId != null) {
                prefs[Keys.TIMER_TOPIC_ID] = snapshot.topicId
            } else {
                prefs.remove(Keys.TIMER_TOPIC_ID)
            }
            if (!snapshot.topicTitle.isNullOrBlank()) {
                prefs[Keys.TIMER_TOPIC_TITLE] = snapshot.topicTitle
            } else {
                prefs.remove(Keys.TIMER_TOPIC_TITLE)
            }
            prefs[Keys.TIMER_POMODORO_ROUND] = snapshot.pomodoroRound
        }
    }

    suspend fun restorePreferences(preferences: UserPreferences) {
        context.ecetDataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = preferences.studentName
            prefs[Keys.BRANCH] = preferences.branch
            prefs[Keys.TARGET_EXAM] = preferences.targetExam
            if (preferences.examDateEpochDay != null) {
                prefs[Keys.EXAM_DATE_EPOCH_DAY] = preferences.examDateEpochDay
            } else {
                prefs.remove(Keys.EXAM_DATE_EPOCH_DAY)
            }
            prefs[Keys.DAILY_TARGET_MINUTES] = preferences.dailyStudyTargetMinutes
            prefs[Keys.THEME_MODE] = preferences.themeMode.name
            prefs[Keys.REMINDERS_ENABLED] = preferences.remindersEnabled
            prefs[Keys.POMODORO_ALERTS_ENABLED] = preferences.pomodoroAlertsEnabled
            prefs[Keys.REMINDER_HOUR] = preferences.reminderHour
            prefs[Keys.REMINDER_MINUTE] = preferences.reminderMinute
            prefs[Keys.POMODORO_FOCUS_MINUTES] = preferences.pomodoroFocusMinutes
            prefs[Keys.POMODORO_SHORT_BREAK_MINUTES] = preferences.pomodoroShortBreakMinutes
            prefs[Keys.POMODORO_LONG_BREAK_MINUTES] = preferences.pomodoroLongBreakMinutes
        }
    }

    suspend fun resetAllPreferences() {
        context.ecetDataStore.edit { it.clear() }
    }
}
