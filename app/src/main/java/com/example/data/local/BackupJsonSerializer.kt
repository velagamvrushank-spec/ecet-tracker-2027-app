package com.example.data.local

import com.example.data.model.Subject
import com.example.data.model.ThemeMode
import com.example.data.model.TopicPriority
import com.example.data.model.TopicStatus
import com.example.data.model.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAtMs: Long,
    val preferences: UserPreferences,
    val topics: List<SyllabusTopicEntity>,
    val studySessions: List<StudySessionEntity>,
    val dailyTasks: List<DailyTaskEntity>,
    val mcqSessions: List<McqSessionEntity>,
    val mockTests: List<MockTestEntity>,
    val aiSummaries: List<AiSummaryEntity> = emptyList()
)

object BackupJsonSerializer {
    private const val CURRENT_SCHEMA_VERSION = 2

    fun exportToJson(
        preferences: UserPreferences,
        topics: List<SyllabusTopicEntity>,
        studySessions: List<StudySessionEntity>,
        dailyTasks: List<DailyTaskEntity>,
        mcqSessions: List<McqSessionEntity>,
        mockTests: List<MockTestEntity>,
        aiSummaries: List<AiSummaryEntity> = emptyList()
    ): String {
        val root = JSONObject().apply {
            put("app", "ECET Tracker 2027")
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("exportedAtMs", System.currentTimeMillis())

            put("preferences", JSONObject().apply {
                put("studentName", preferences.studentName)
                put("branch", preferences.branch)
                put("targetExam", preferences.targetExam)
                if (preferences.examDateEpochDay != null) {
                    put("examDateEpochDay", preferences.examDateEpochDay)
                } else {
                    put("examDateEpochDay", JSONObject.NULL)
                }
                put("dailyStudyTargetMinutes", preferences.dailyStudyTargetMinutes)
                put("themeMode", preferences.themeMode.name)
                put("remindersEnabled", preferences.remindersEnabled)
                put("pomodoroAlertsEnabled", preferences.pomodoroAlertsEnabled)
                put("reminderHour", preferences.reminderHour)
                put("reminderMinute", preferences.reminderMinute)
                put("pomodoroFocusMinutes", preferences.pomodoroFocusMinutes)
                put("pomodoroShortBreakMinutes", preferences.pomodoroShortBreakMinutes)
                put("pomodoroLongBreakMinutes", preferences.pomodoroLongBreakMinutes)
            })

            put("syllabusTopics", JSONArray().apply {
                topics.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("subject", t.subject)
                        put("title", t.title)
                        put("status", t.status)
                        put("priority", t.priority)
                        put("notes", t.notes)
                        if (t.targetDateEpochDay != null) put("targetDateEpochDay", t.targetDateEpochDay)
                        put("createdAtMs", t.createdAtMs)
                        put("updatedAtMs", t.updatedAtMs)
                    })
                }
            })

            put("studySessions", JSONArray().apply {
                studySessions.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("subject", s.subject)
                        if (s.topicId != null) put("topicId", s.topicId)
                        if (s.topicTitle != null) put("topicTitle", s.topicTitle)
                        put("mode", s.mode)
                        put("durationSeconds", s.durationSeconds)
                        put("startedAtMs", s.startedAtMs)
                        put("endedAtMs", s.endedAtMs)
                        put("dateEpochDay", s.dateEpochDay)
                    })
                }
            })

            put("dailyTasks", JSONArray().apply {
                dailyTasks.forEach { task ->
                    put(JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        if (task.subject != null) put("subject", task.subject)
                        put("priority", task.priority)
                        put("dateEpochDay", task.dateEpochDay)
                        put("isCompleted", task.isCompleted)
                        put("createdAtMs", task.createdAtMs)
                    })
                }
            })

            put("mcqSessions", JSONArray().apply {
                mcqSessions.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("subject", m.subject)
                        put("topicName", m.topicName)
                        put("attempted", m.attempted)
                        put("correct", m.correct)
                        put("incorrect", m.incorrect)
                        put("unattempted", m.unattempted)
                        put("timeTakenMinutes", m.timeTakenMinutes)
                        put("accuracyPercent", m.accuracyPercent.toDouble())
                        put("dateEpochDay", m.dateEpochDay)
                        put("createdAtMs", m.createdAtMs)
                    })
                }
            })

            put("mockTests", JSONArray().apply {
                mockTests.forEach { mock ->
                    put(JSONObject().apply {
                        put("id", mock.id)
                        put("title", mock.title)
                        put("testDateEpochDay", mock.testDateEpochDay)
                        put("mathMarks", mock.mathMarks.toDouble())
                        put("mathMaxMarks", mock.mathMaxMarks.toDouble())
                        put("physicsMarks", mock.physicsMarks.toDouble())
                        put("physicsMaxMarks", mock.physicsMaxMarks.toDouble())
                        put("chemistryMarks", mock.chemistryMarks.toDouble())
                        put("chemistryMaxMarks", mock.chemistryMaxMarks.toDouble())
                        put("eceMarks", mock.eceMarks.toDouble())
                        put("eceMaxMarks", mock.eceMaxMarks.toDouble())
                        put("totalMarks", mock.totalMarks.toDouble())
                        put("maxTotalMarks", mock.maxTotalMarks.toDouble())
                        put("attempted", mock.attempted)
                        put("correct", mock.correct)
                        put("incorrect", mock.incorrect)
                        put("notes", mock.notes)
                        put("createdAtMs", mock.createdAtMs)
                    })
                }
            })

            put("aiSummaries", JSONArray().apply {
                aiSummaries.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("subject", s.subject)
                        put("sourceTitle", s.sourceTitle)
                        put("sourceType", s.sourceType)
                        put("summaryMarkdown", s.summaryMarkdown)
                        put("createdAtMs", s.createdAtMs)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun importFromJson(rawJson: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(rawJson.trim())
        val schemaVersion = root.optInt("schemaVersion", 1)
        require(schemaVersion >= 1) { "Unsupported backup schema version" }

        val prefObj = root.optJSONObject("preferences")
        val preferences = if (prefObj != null) {
            UserPreferences(
                studentName = prefObj.optString("studentName", "ECET Aspirant").ifBlank { "ECET Aspirant" },
                branch = prefObj.optString("branch", "ECE (Electronics & Communication)"),
                targetExam = prefObj.optString("targetExam", "TG ECET 2027"),
                examDateEpochDay = if (prefObj.has("examDateEpochDay") && !prefObj.isNull("examDateEpochDay")) {
                    prefObj.getLong("examDateEpochDay")
                } else null,
                dailyStudyTargetMinutes = prefObj.optInt("dailyStudyTargetMinutes", 240).coerceIn(15, 960),
                themeMode = ThemeMode.fromName(prefObj.optString("themeMode", "LIGHT")),
                remindersEnabled = prefObj.optBoolean("remindersEnabled", false),
                pomodoroAlertsEnabled = prefObj.optBoolean("pomodoroAlertsEnabled", true),
                reminderHour = prefObj.optInt("reminderHour", 19).coerceIn(0, 23),
                reminderMinute = prefObj.optInt("reminderMinute", 0).coerceIn(0, 59),
                pomodoroFocusMinutes = prefObj.optInt("pomodoroFocusMinutes", 25).coerceIn(1, 180),
                pomodoroShortBreakMinutes = prefObj.optInt("pomodoroShortBreakMinutes", 5).coerceIn(1, 60),
                pomodoroLongBreakMinutes = prefObj.optInt("pomodoroLongBreakMinutes", 15).coerceIn(1, 90)
            )
        } else {
            UserPreferences()
        }

        val topics = mutableListOf<SyllabusTopicEntity>()
        val topicsArr = root.optJSONArray("syllabusTopics") ?: JSONArray()
        for (i in 0 until topicsArr.length()) {
            val obj = topicsArr.getJSONObject(i)
            val title = obj.optString("title").trim()
            if (title.isNotEmpty()) {
                topics.add(
                    SyllabusTopicEntity(
                        id = obj.optLong("id", 0L),
                        subject = Subject.fromName(obj.optString("subject")).name,
                        title = title,
                        status = TopicStatus.fromName(obj.optString("status")).name,
                        priority = TopicPriority.fromName(obj.optString("priority")).name,
                        notes = obj.optString("notes", ""),
                        targetDateEpochDay = if (obj.has("targetDateEpochDay") && !obj.isNull("targetDateEpochDay")) {
                            obj.getLong("targetDateEpochDay")
                        } else null,
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                        updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
                    )
                )
            }
        }

        val sessions = mutableListOf<StudySessionEntity>()
        val sessionsArr = root.optJSONArray("studySessions") ?: JSONArray()
        for (i in 0 until sessionsArr.length()) {
            val obj = sessionsArr.getJSONObject(i)
            sessions.add(
                StudySessionEntity(
                    id = obj.optLong("id", 0L),
                    subject = Subject.fromName(obj.optString("subject")).name,
                    topicId = if (obj.has("topicId") && !obj.isNull("topicId")) obj.getLong("topicId") else null,
                    topicTitle = if (obj.has("topicTitle") && !obj.isNull("topicTitle")) obj.getString("topicTitle") else null,
                    mode = obj.optString("mode", "STOPWATCH"),
                    durationSeconds = obj.optLong("durationSeconds", 0L).coerceAtLeast(0L),
                    startedAtMs = obj.optLong("startedAtMs", System.currentTimeMillis()),
                    endedAtMs = obj.optLong("endedAtMs", System.currentTimeMillis()),
                    dateEpochDay = obj.optLong("dateEpochDay", 0L)
                )
            )
        }

        val tasks = mutableListOf<DailyTaskEntity>()
        val tasksArr = root.optJSONArray("dailyTasks") ?: JSONArray()
        for (i in 0 until tasksArr.length()) {
            val obj = tasksArr.getJSONObject(i)
            val title = obj.optString("title").trim()
            if (title.isNotEmpty()) {
                tasks.add(
                    DailyTaskEntity(
                        id = obj.optLong("id", 0L),
                        title = title,
                        subject = if (obj.has("subject") && !obj.isNull("subject")) Subject.fromName(obj.getString("subject")).name else null,
                        priority = TopicPriority.fromName(obj.optString("priority")).name,
                        dateEpochDay = obj.optLong("dateEpochDay", 0L),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
        }

        val mcqs = mutableListOf<McqSessionEntity>()
        val mcqsArr = root.optJSONArray("mcqSessions") ?: JSONArray()
        for (i in 0 until mcqsArr.length()) {
            val obj = mcqsArr.getJSONObject(i)
            val attempted = obj.optInt("attempted", 0).coerceAtLeast(0)
            val correct = obj.optInt("correct", 0).coerceIn(0, attempted)
            val accuracy = if (attempted > 0) (correct.toFloat() / attempted) * 100f else 0f
            mcqs.add(
                McqSessionEntity(
                    id = obj.optLong("id", 0L),
                    subject = Subject.fromName(obj.optString("subject")).name,
                    topicName = obj.optString("topicName", "General Practice").ifBlank { "General Practice" },
                    attempted = attempted,
                    correct = correct,
                    incorrect = obj.optInt("incorrect", (attempted - correct)).coerceAtLeast(0),
                    unattempted = obj.optInt("unattempted", 0).coerceAtLeast(0),
                    timeTakenMinutes = obj.optInt("timeTakenMinutes", 0).coerceAtLeast(0),
                    accuracyPercent = accuracy,
                    dateEpochDay = obj.optLong("dateEpochDay", 0L),
                    createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                )
            )
        }

        val mocks = mutableListOf<MockTestEntity>()
        val mocksArr = root.optJSONArray("mockTests") ?: JSONArray()
        for (i in 0 until mocksArr.length()) {
            val obj = mocksArr.getJSONObject(i)
            mocks.add(
                MockTestEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.optString("title", "Mock Test").ifBlank { "Mock Test" },
                    testDateEpochDay = obj.optLong("testDateEpochDay", 0L),
                    mathMarks = obj.optDouble("mathMarks", 0.0).toFloat(),
                    mathMaxMarks = obj.optDouble("mathMaxMarks", 50.0).toFloat().coerceAtLeast(1f),
                    physicsMarks = obj.optDouble("physicsMarks", 0.0).toFloat(),
                    physicsMaxMarks = obj.optDouble("physicsMaxMarks", 25.0).toFloat().coerceAtLeast(1f),
                    chemistryMarks = obj.optDouble("chemistryMarks", 0.0).toFloat(),
                    chemistryMaxMarks = obj.optDouble("chemistryMaxMarks", 25.0).toFloat().coerceAtLeast(1f),
                    eceMarks = obj.optDouble("eceMarks", 0.0).toFloat(),
                    eceMaxMarks = obj.optDouble("eceMaxMarks", 100.0).toFloat().coerceAtLeast(1f),
                    totalMarks = obj.optDouble("totalMarks", 0.0).toFloat(),
                    maxTotalMarks = obj.optDouble("maxTotalMarks", 200.0).toFloat().coerceAtLeast(1f),
                    attempted = obj.optInt("attempted", 0).coerceAtLeast(0),
                    correct = obj.optInt("correct", 0).coerceAtLeast(0),
                    incorrect = obj.optInt("incorrect", 0).coerceAtLeast(0),
                    notes = obj.optString("notes", ""),
                    createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                )
            )
        }

        val summaries = mutableListOf<AiSummaryEntity>()
        val summariesArr = root.optJSONArray("aiSummaries") ?: JSONArray()
        for (i in 0 until summariesArr.length()) {
            val obj = summariesArr.getJSONObject(i)
            val sourceTitle = obj.optString("sourceTitle").trim()
            val summaryMarkdown = obj.optString("summaryMarkdown").trim()
            if (sourceTitle.isNotEmpty() && summaryMarkdown.isNotEmpty()) {
                summaries.add(
                    AiSummaryEntity(
                        id = obj.optLong("id", 0L),
                        subject = Subject.fromName(obj.optString("subject")).name,
                        sourceTitle = sourceTitle,
                        sourceType = obj.optString("sourceType", "TOPIC").ifBlank { "TOPIC" },
                        summaryMarkdown = summaryMarkdown,
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
        }

        BackupPayload(
            schemaVersion = schemaVersion,
            exportedAtMs = root.optLong("exportedAtMs", System.currentTimeMillis()),
            preferences = preferences,
            topics = topics,
            studySessions = sessions,
            dailyTasks = tasks,
            mcqSessions = mcqs,
            mockTests = mocks,
            aiSummaries = summaries
        )
    }

    fun parseSyllabusImportInput(
        rawInput: String,
        fallbackSubject: Subject
    ): List<SyllabusTopicEntity> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Try JSON array or JSON object with "topics" / "syllabusTopics"
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            val parsed = runCatching {
                val arr = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("syllabusTopics")
                        ?: obj.optJSONArray("topics")
                        ?: JSONArray()
                }
                val list = mutableListOf<SyllabusTopicEntity>()
                for (i in 0 until arr.length()) {
                    when (val item = arr.get(i)) {
                        is String -> {
                            val title = item.trim()
                            if (title.isNotEmpty()) {
                                list.add(
                                    SyllabusTopicEntity(
                                        subject = fallbackSubject.name,
                                        title = title,
                                        status = TopicStatus.NOT_STARTED.name,
                                        priority = TopicPriority.MEDIUM.name
                                    )
                                )
                            }
                        }
                        is JSONObject -> {
                            val title = item.optString("title").trim()
                            if (title.isNotEmpty()) {
                                list.add(
                                    SyllabusTopicEntity(
                                        subject = if (item.has("subject")) {
                                            Subject.fromName(item.optString("subject")).name
                                        } else fallbackSubject.name,
                                        title = title,
                                        status = TopicStatus.fromName(item.optString("status")).name,
                                        priority = TopicPriority.fromName(item.optString("priority")).name,
                                        notes = item.optString("notes", "")
                                    )
                                )
                            }
                        }
                    }
                }
                list
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        // Plain newline-separated lines (strip optional bullet/number prefixes)
        return trimmed.lines()
            .map { line ->
                line.trim()
                    .replace(Regex("^[-*•]+\\s*"), "")
                    .replace(Regex("^\\d+[.)]\\s*"), "")
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { title ->
                SyllabusTopicEntity(
                    subject = fallbackSubject.name,
                    title = title,
                    status = TopicStatus.NOT_STARTED.name,
                    priority = TopicPriority.MEDIUM.name
                )
            }
    }
}
