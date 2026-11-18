package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "syllabus_topics")
data class SyllabusTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val title: String,
    val status: String,
    val priority: String,
    val notes: String = "",
    val targetDateEpochDay: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topicId: Long? = null,
    val topicTitle: String? = null,
    val mode: String,
    val durationSeconds: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val dateEpochDay: Long
)

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String? = null,
    val priority: String,
    val dateEpochDay: Long,
    val isCompleted: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mcq_sessions")
data class McqSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topicName: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val timeTakenMinutes: Int,
    val accuracyPercent: Float,
    val dateEpochDay: Long,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_tests")
data class MockTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val testDateEpochDay: Long,
    val mathMarks: Float,
    val mathMaxMarks: Float,
    val physicsMarks: Float,
    val physicsMaxMarks: Float,
    val chemistryMarks: Float,
    val chemistryMaxMarks: Float,
    val eceMarks: Float,
    val eceMaxMarks: Float,
    val totalMarks: Float,
    val maxTotalMarks: Float,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val notes: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_summaries")
data class AiSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val sourceTitle: String,
    val sourceType: String,
    val summaryMarkdown: String,
    val createdAtMs: Long = System.currentTimeMillis()
)
