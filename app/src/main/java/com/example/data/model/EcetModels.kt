package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanPhysics
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.VioletSecondary

enum class Subject(
    val displayName: String,
    val shortName: String,
    val accentColor: Color,
    val defaultMockMaxMarks: Int
) {
    MATHEMATICS("Mathematics", "Math", CobaltPrimary, 50),
    PHYSICS("Physics", "Physics", CyanPhysics, 25),
    CHEMISTRY("Chemistry", "Chem", EmeraldSuccess, 25),
    ECE("Electronics & Communication (ECE)", "ECE", VioletSecondary, 100);

    companion object {
        fun fromName(value: String?): Subject =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ECE
    }
}

enum class TopicStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    fun next(): TopicStatus = when (this) {
        NOT_STARTED -> IN_PROGRESS
        IN_PROGRESS -> COMPLETED
        COMPLETED -> NOT_STARTED
    }

    fun color(): Color = when (this) {
        NOT_STARTED -> SlateTextSecondary
        IN_PROGRESS -> AmberWarning
        COMPLETED -> EmeraldSuccess
    }

    companion object {
        fun fromName(value: String?): TopicStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NOT_STARTED
    }
}

enum class TopicPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    fun color(): Color = when (this) {
        HIGH -> CoralError
        MEDIUM -> AmberWarning
        LOW -> CobaltPrimary
    }

    companion object {
        fun fromName(value: String?): TopicPriority =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

enum class StudyMode(val label: String) {
    STOPWATCH("Stopwatch"),
    POMODORO("Pomodoro");

    companion object {
        fun fromName(value: String?): StudyMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STOPWATCH
    }
}

enum class PomodoroPhase(val label: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break");

    companion object {
        fun fromName(value: String?): PomodoroPhase =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FOCUS
    }
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System");

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DARK
    }
}

enum class StudySubTab(val label: String) {
    TIMER("Timer"),
    TASKS("Tasks"),
    MCQ("MCQ"),
    MOCKS("Mocks")
}

data class UserPreferences(
    val studentName: String = "ECET Aspirant",
    val branch: String = "ECE (Electronics & Communication)",
    val targetExam: String = "TG ECET 2027",
    val examDateEpochDay: Long? = null,
    val dailyStudyTargetMinutes: Int = 240,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val remindersEnabled: Boolean = false,
    val pomodoroAlertsEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15
)

data class PersistedTimerSnapshot(
    val isRunning: Boolean = false,
    val mode: StudyMode = StudyMode.STOPWATCH,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val startWallMs: Long = 0L,
    val accumulatedMs: Long = 0L,
    val subject: Subject = Subject.ECE,
    val topicId: Long? = null,
    val topicTitle: String? = null,
    val pomodoroRound: Int = 1
)

data class TimerUiState(
    val isRunning: Boolean = false,
    val mode: StudyMode = StudyMode.STOPWATCH,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val elapsedMs: Long = 0L,
    val remainingMs: Long = 25 * 60_000L,
    val targetDurationMs: Long = 25 * 60_000L,
    val subject: Subject = Subject.ECE,
    val topicId: Long? = null,
    val topicTitle: String? = null,
    val pomodoroRound: Int = 1,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
)

data class SubjectSummary(
    val subject: Subject,
    val totalTopics: Int,
    val completedTopics: Int,
    val inProgressTopics: Int,
    val notStartedTopics: Int,
    val pendingTopics: Int,
    val completionPercent: Float
)

data class PreparationOverview(
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val inProgressTopics: Int = 0,
    val pendingTopics: Int = 0,
    val completionPercent: Float = 0f,
    val subjectSummaries: List<SubjectSummary> = emptyList()
)

data class StudyStats(
    val totalStudySeconds: Long = 0L,
    val todayStudySeconds: Long = 0L,
    val weeklyStudySeconds: Long = 0L,
    val dailyTargetMinutes: Int = 240,
    val dailyTargetPercent: Float = 0f
)

data class SubjectMcqPerformance(
    val subject: Subject,
    val sessionsCount: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val accuracyPercent: Float
)

data class TopicMcqPerformance(
    val subject: Subject,
    val topicName: String,
    val sessionsCount: Int,
    val attempted: Int,
    val correct: Int,
    val accuracyPercent: Float
)

data class DayStudyMetric(
    val epochDay: Long,
    val label: String,
    val studyHours: Float,
    val studyMinutes: Int,
    val tasksCompleted: Int
)

data class AnalyticsState(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val activeDaysLast30: Int = 0,
    val weeklyStudyDays: List<DayStudyMetric> = emptyList(),
    val monthlyStudyBuckets: List<DayStudyMetric> = emptyList(),
    val heatmapDays: List<DayStudyMetric> = emptyList(),
    val subjectCompletions: List<SubjectSummary> = emptyList(),
    val mcqAccuracySeries: List<Pair<String, Float>> = emptyList(),
    val mockScoreSeries: List<Pair<String, Float>> = emptyList()
)
