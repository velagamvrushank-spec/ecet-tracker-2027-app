package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.local.AiSummaryEntity
import com.example.data.local.BackupJsonSerializer
import com.example.data.local.DailyTaskEntity
import com.example.data.local.EcetDatabase
import com.example.data.local.McqSessionEntity
import com.example.data.local.MockTestEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.Subject
import com.example.data.model.ThemeMode
import com.example.data.model.TopicPriority
import com.example.data.model.TopicStatus
import com.example.data.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameMatchesEcetTracker2027() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ECET Tracker 2027", appName)
    }

    @Test
    fun bottomNavigationSwitchesAllFiveScreens() {
        composeRule.onNodeWithText("Exam date not set").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_syllabus").performClick()
        composeRule.onNodeWithText("Syllabus Tracker").assertIsDisplayed()
        composeRule.onNodeWithTag("syllabus_photo_box").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_study").performClick()
        composeRule.onNodeWithTag("study_subtab_timer").assertIsDisplayed()
        composeRule.onNodeWithTag("study_subtab_tasks").performClick()
        composeRule.onNodeWithText("Daily Progress").assertIsDisplayed()

        composeRule.onNodeWithTag("study_subtab_mcq").performClick()
        composeRule.onNodeWithText("MCQ Practice Overview").assertIsDisplayed()

        composeRule.onNodeWithTag("study_subtab_mocks").performClick()
        composeRule.onNodeWithText("Mock Test Performance").assertIsDisplayed()

        composeRule.onNodeWithTag("study_subtab_summary").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Electronics AI Summary").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_summary_list")
            .performScrollToNode(hasText("Ask Electronics Questions"))
        composeRule.onNodeWithText("Ask Electronics Questions").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_analytics").performClick()
        composeRule.onNodeWithText("Analytics & Reports").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.onNodeWithText("Profile & Settings").assertIsDisplayed()
    }

    @Test
    fun addTopicUpdatesSyllabusAndOverview() {
        composeRule.onNodeWithTag("nav_syllabus").performClick()
        composeRule.onNodeWithTag("add_topic_button").performClick()
        composeRule.onNodeWithTag("topic_title_input").performTextInput("Laplace Transforms")
        composeRule.onNodeWithTag("save_topic_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("0/1 done · 1 pending").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("syllabus_list")
            .performScrollToNode(hasText("Laplace Transforms"))
        composeRule.onNodeWithText("Laplace Transforms").assertIsDisplayed()
    }

    @Test
    fun homeQuickActionAddTopicOpensDialogOnFirstTap() {
        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("quick_action_topic"))
        composeRule.onNodeWithTag("quick_action_topic").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Add Syllabus Topic").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Add Syllabus Topic").assertIsDisplayed()
    }

    @Test
    fun mcqPracticeCalculatesAccuracyAndSaves() {
        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("quick_action_mcq"))
        composeRule.onNodeWithTag("quick_action_mcq").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Attempted *").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("mcq_topic_input").performTextInput("K-Maps")
        composeRule.onNodeWithTag("mcq_attempted_input").performTextInput("20")
        composeRule.onNodeWithTag("mcq_correct_input").performTextInput("15")
        composeRule.onNodeWithText("75.0%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save_mcq_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("75.0%").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("75.0%").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("mcq_list")
            .performScrollToNode(hasText("K-Maps"))
        composeRule.onAllNodesWithText("K-Maps").onFirst().assertIsDisplayed()
    }

    @Test
    fun parseSyllabusPlainLinesAndJson() {
        val linesResult = BackupJsonSerializer.parseSyllabusImportInput(
            "1. Matrices\n- Laplace Transforms\nMatrices",
            Subject.MATHEMATICS
        )
        assertEquals(2, linesResult.size)
        assertEquals("Matrices", linesResult[0].title)
        assertEquals("Laplace Transforms", linesResult[1].title)

        val jsonResult = BackupJsonSerializer.parseSyllabusImportInput(
            """[{"title":"Op-Amps","subject":"ECE","priority":"HIGH"}]""",
            Subject.PHYSICS
        )
        assertEquals(1, jsonResult.size)
        assertEquals("Op-Amps", jsonResult[0].title)
        assertEquals(Subject.ECE.name, jsonResult[0].subject)
        assertEquals(TopicPriority.HIGH.name, jsonResult[0].priority)
    }

    @Test
    fun backupExportImportRoundTripPreservesAllData() {
        val prefs = UserPreferences(
            studentName = "Karthik",
            branch = "ECE",
            targetExam = "TG ECET 2027",
            examDateEpochDay = 20950L,
            dailyStudyTargetMinutes = 300,
            themeMode = ThemeMode.DARK
        )
        val topics = listOf(
            SyllabusTopicEntity(
                id = 1,
                subject = Subject.ECE.name,
                title = "Digital Electronics",
                status = TopicStatus.IN_PROGRESS.name,
                priority = TopicPriority.HIGH.name,
                notes = "K-maps"
            )
        )
        val sessions = listOf(
            StudySessionEntity(
                id = 1,
                subject = Subject.ECE.name,
                topicId = 1,
                topicTitle = "Digital Electronics",
                mode = "POMODORO",
                durationSeconds = 1500,
                startedAtMs = 1000L,
                endedAtMs = 1501000L,
                dateEpochDay = 20700L
            )
        )
        val tasks = listOf(
            DailyTaskEntity(
                id = 1,
                title = "Solve 40 MCQs",
                subject = Subject.MATHEMATICS.name,
                priority = TopicPriority.HIGH.name,
                dateEpochDay = 20700L,
                isCompleted = true
            )
        )
        val mcqs = listOf(
            McqSessionEntity(
                id = 1,
                subject = Subject.ECE.name,
                topicName = "Digital Electronics",
                attempted = 20,
                correct = 16,
                incorrect = 4,
                unattempted = 0,
                timeTakenMinutes = 20,
                accuracyPercent = 80f,
                dateEpochDay = 20700L
            )
        )
        val mocks = listOf(
            MockTestEntity(
                id = 1,
                title = "Mock #1",
                testDateEpochDay = 20700L,
                mathMarks = 40f,
                mathMaxMarks = 50f,
                physicsMarks = 20f,
                physicsMaxMarks = 25f,
                chemistryMarks = 18f,
                chemistryMaxMarks = 25f,
                eceMarks = 75f,
                eceMaxMarks = 100f,
                totalMarks = 153f,
                maxTotalMarks = 200f,
                attempted = 180,
                correct = 153,
                incorrect = 27
            )
        )

        val summaries = listOf(
            AiSummaryEntity(
                id = 1,
                subject = Subject.ECE.name,
                sourceTitle = "BJT CE Amplifier",
                sourceType = "TOPIC",
                summaryMarkdown = "## Overview\nProvides 180-degree phase shift.",
                createdAtMs = 20700L
            )
        )

        val json = BackupJsonSerializer.exportToJson(prefs, topics, sessions, tasks, mcqs, mocks, summaries)
        val restored = BackupJsonSerializer.importFromJson(json)
        assertTrue(restored.isSuccess)
        val payload = restored.getOrThrow()
        assertEquals(2, payload.schemaVersion)
        assertEquals("Karthik", payload.preferences.studentName)
        assertEquals(20950L, payload.preferences.examDateEpochDay)
        assertEquals(1, payload.topics.size)
        assertEquals(1, payload.studySessions.size)
        assertEquals(1, payload.dailyTasks.size)
        assertEquals(1, payload.mcqSessions.size)
        assertEquals(1, payload.mockTests.size)
        assertEquals(1, payload.aiSummaries.size)
        assertEquals("BJT CE Amplifier", payload.aiSummaries[0].sourceTitle)
        assertEquals(80f, payload.mcqSessions[0].accuracyPercent, 0.01f)
        assertEquals(153f, payload.mockTests[0].totalMarks, 0.01f)
    }

    @Test
    fun roomMigration1To2PreservesTopicsAndCreatesAiSummaries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory SQLite db
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `syllabus_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `priority` TEXT NOT NULL, `notes` TEXT NOT NULL, `targetDateEpochDay` INTEGER, `createdAtMs` INTEGER NOT NULL, `updatedAtMs` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO `syllabus_topics` (`subject`, `title`, `status`, `priority`, `notes`, `targetDateEpochDay`, `createdAtMs`, `updatedAtMs`) VALUES ('ECE', 'Op-Amp Characteristics', 'IN_PROGRESS', 'HIGH', 'CMRR & slew rate', NULL, 1000, 1000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        EcetDatabase.MIGRATION_1_2.migrate(db)

        db.query("SELECT title, status FROM syllabus_topics").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Op-Amp Characteristics", cursor.getString(0))
            assertEquals("IN_PROGRESS", cursor.getString(1))
        }

        db.execSQL(
            "INSERT INTO `ai_summaries` (`subject`, `sourceTitle`, `sourceType`, `summaryMarkdown`, `createdAtMs`) VALUES ('ECE', 'Op-Amp', 'TOPIC', 'High CMRR summary', 2000)"
        )
        db.query("SELECT sourceTitle, summaryMarkdown FROM ai_summaries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Op-Amp", cursor.getString(0))
            assertEquals("High CMRR summary", cursor.getString(1))
        }
        helper.close()
    }
}
