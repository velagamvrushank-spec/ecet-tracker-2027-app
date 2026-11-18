package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EcetDao {
    // Syllabus Topics
    @Query("SELECT * FROM syllabus_topics ORDER BY createdAtMs DESC, id DESC")
    fun observeTopics(): Flow<List<SyllabusTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: SyllabusTopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<SyllabusTopicEntity>)

    @Update
    suspend fun updateTopic(topic: SyllabusTopicEntity)

    @Query("DELETE FROM syllabus_topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    // Study Sessions
    @Query("SELECT * FROM study_sessions ORDER BY endedAtMs DESC, id DESC")
    fun observeStudySessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteStudySessionById(id: Long)

    // Daily Tasks
    @Query("SELECT * FROM daily_tasks ORDER BY isCompleted ASC, createdAtMs DESC, id DESC")
    fun observeDailyTasks(): Flow<List<DailyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTask(task: DailyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTasks(tasks: List<DailyTaskEntity>)

    @Update
    suspend fun updateDailyTask(task: DailyTaskEntity)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun deleteDailyTaskById(id: Long)

    // MCQ Sessions
    @Query("SELECT * FROM mcq_sessions ORDER BY dateEpochDay DESC, createdAtMs DESC, id DESC")
    fun observeMcqSessions(): Flow<List<McqSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcqSession(session: McqSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcqSessions(sessions: List<McqSessionEntity>)

    @Query("DELETE FROM mcq_sessions WHERE id = :id")
    suspend fun deleteMcqSessionById(id: Long)

    // Mock Tests
    @Query("SELECT * FROM mock_tests ORDER BY testDateEpochDay ASC, createdAtMs ASC, id ASC")
    fun observeMockTests(): Flow<List<MockTestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(test: MockTestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTests(tests: List<MockTestEntity>)

    @Update
    suspend fun updateMockTest(test: MockTestEntity)

    @Query("DELETE FROM mock_tests WHERE id = :id")
    suspend fun deleteMockTestById(id: Long)

    // AI Summaries
    @Query("SELECT * FROM ai_summaries ORDER BY createdAtMs DESC, id DESC")
    fun observeAiSummaries(): Flow<List<AiSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSummary(summary: AiSummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSummaries(summaries: List<AiSummaryEntity>)

    @Query("DELETE FROM ai_summaries WHERE id = :id")
    suspend fun deleteAiSummaryById(id: Long)

    // Bulk Clear & Restore
    @Query("DELETE FROM syllabus_topics")
    suspend fun clearTopics()

    @Query("DELETE FROM study_sessions")
    suspend fun clearStudySessions()

    @Query("DELETE FROM daily_tasks")
    suspend fun clearDailyTasks()

    @Query("DELETE FROM mcq_sessions")
    suspend fun clearMcqSessions()

    @Query("DELETE FROM mock_tests")
    suspend fun clearMockTests()

    @Query("DELETE FROM ai_summaries")
    suspend fun clearAiSummaries()

    @Transaction
    suspend fun clearAllTables() {
        clearTopics()
        clearStudySessions()
        clearDailyTasks()
        clearMcqSessions()
        clearMockTests()
        clearAiSummaries()
    }

    @Transaction
    suspend fun replaceAllData(
        topics: List<SyllabusTopicEntity>,
        sessions: List<StudySessionEntity>,
        tasks: List<DailyTaskEntity>,
        mcqs: List<McqSessionEntity>,
        mocks: List<MockTestEntity>,
        aiSummaries: List<AiSummaryEntity> = emptyList()
    ) {
        clearAllTables()
        if (topics.isNotEmpty()) insertTopics(topics)
        if (sessions.isNotEmpty()) insertStudySessions(sessions)
        if (tasks.isNotEmpty()) insertDailyTasks(tasks)
        if (mcqs.isNotEmpty()) insertMcqSessions(mcqs)
        if (mocks.isNotEmpty()) insertMockTests(mocks)
        if (aiSummaries.isNotEmpty()) insertAiSummaries(aiSummaries)
    }
}
