package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AiSummaryEntity
import com.example.data.local.DailyTaskEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.AiChatMessage
import com.example.data.model.AiSummaryUiState
import com.example.data.model.AttachedStudyFile
import com.example.data.model.PomodoroPhase
import com.example.data.model.StudyMode
import com.example.data.model.StudySubTab
import com.example.data.model.Subject
import com.example.data.model.TimerUiState
import com.example.data.model.TopicPriority
import com.example.ui.viewmodel.EcetUiState

@Composable
fun StudyHubScreen(
    selectedSubTab: StudySubTab,
    onSelectSubTab: (StudySubTab) -> Unit,
    uiState: EcetUiState,
    timerState: TimerUiState,
    shouldOpenAddTask: Boolean,
    onConsumeOpenAddTask: () -> Unit,
    shouldOpenAddMcq: Boolean,
    onConsumeOpenAddMcq: () -> Unit,
    shouldOpenAddMock: Boolean,
    onConsumeOpenAddMock: () -> Unit,
    onStartOrResumeTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinishAndSaveSession: () -> Unit,
    onSetTimerMode: (StudyMode) -> Unit,
    onSetPomodoroPhase: (PomodoroPhase) -> Unit,
    onSelectTimerSubject: (Subject) -> Unit,
    onSelectTimerTopic: (SyllabusTopicEntity?) -> Unit,
    onUpdatePomodoroDurations: (Int, Int, Int) -> Unit,
    onDeleteStudySession: (Long) -> Unit,
    onSaveDailyTask: (Long, String, Subject?, TopicPriority, Long, Boolean) -> Unit,
    onToggleDailyTask: (DailyTaskEntity) -> Unit,
    onDeleteDailyTask: (Long) -> Unit,
    onAddMcqSession: (Subject, String, Int, Int, Int, Int, Int, Long) -> Unit,
    onDeleteMcqSession: (Long) -> Unit,
    onSaveMockTest: (Long, String, Long, Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, String) -> Unit,
    onDeleteMockTest: (Long) -> Unit,
    summarySubject: Subject,
    summaryTopicInput: String,
    attachedStudyFile: AttachedStudyFile?,
    summaryUiState: AiSummaryUiState,
    qaMessages: List<AiChatMessage>,
    qaError: String?,
    isQaLoading: Boolean,
    onSelectSummarySubject: (Subject) -> Unit,
    onSummaryTopicInputChange: (String) -> Unit,
    onAttachSummaryFileUri: (Uri) -> Unit,
    onClearAttachedSummaryFile: () -> Unit,
    onGenerateSummary: () -> Unit,
    onAskSummaryQuestion: (String) -> Unit,
    onSaveCurrentSummary: () -> Unit,
    onLoadSavedSummary: (AiSummaryEntity) -> Unit,
    onDeleteSavedSummary: (Long) -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Segmented Sub-Tab Switcher
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    StudySubTab.entries.forEach { tab ->
                        val isSelected = selectedSubTab == tab
                        Surface(
                            onClick = { onSelectSubTab(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("study_subtab_${tab.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            when (selectedSubTab) {
                StudySubTab.TIMER -> StudyTimerSection(
                    uiState = uiState,
                    timerState = timerState,
                    onStartOrResume = onStartOrResumeTimer,
                    onPause = onPauseTimer,
                    onReset = onResetTimer,
                    onFinishAndSave = onFinishAndSaveSession,
                    onSetMode = onSetTimerMode,
                    onSetPhase = onSetPomodoroPhase,
                    onSelectSubject = onSelectTimerSubject,
                    onSelectTopic = onSelectTimerTopic,
                    onUpdatePomodoroDurations = onUpdatePomodoroDurations,
                    onDeleteSession = onDeleteStudySession
                )

                StudySubTab.TASKS -> DailyTasksSection(
                    uiState = uiState,
                    shouldOpenAddTask = shouldOpenAddTask,
                    onConsumeOpenAddTask = onConsumeOpenAddTask,
                    onSaveTask = onSaveDailyTask,
                    onToggleTask = onToggleDailyTask,
                    onDeleteTask = onDeleteDailyTask
                )

                StudySubTab.MCQ -> McqTrackerSection(
                    uiState = uiState,
                    shouldOpenAddMcq = shouldOpenAddMcq,
                    onConsumeOpenAddMcq = onConsumeOpenAddMcq,
                    onAddMcqSession = onAddMcqSession,
                    onDeleteMcqSession = onDeleteMcqSession
                )

                StudySubTab.MOCKS -> MockTestSection(
                    uiState = uiState,
                    shouldOpenAddMock = shouldOpenAddMock,
                    onConsumeOpenAddMock = onConsumeOpenAddMock,
                    onSaveMockTest = onSaveMockTest,
                    onDeleteMockTest = onDeleteMockTest
                )

                StudySubTab.SUMMARY -> AiSummarySection(
                    uiState = uiState,
                    selectedSubject = summarySubject,
                    topicInput = summaryTopicInput,
                    attachedFile = attachedStudyFile,
                    summaryUiState = summaryUiState,
                    qaMessages = qaMessages,
                    qaError = qaError,
                    isQaLoading = isQaLoading,
                    onSelectSubject = onSelectSummarySubject,
                    onTopicInputChange = onSummaryTopicInputChange,
                    onAttachFileUri = onAttachSummaryFileUri,
                    onClearAttachedFile = onClearAttachedSummaryFile,
                    onGenerateSummary = onGenerateSummary,
                    onAskQuestion = onAskSummaryQuestion,
                    onSaveSummary = onSaveCurrentSummary,
                    onLoadSavedSummary = onLoadSavedSummary,
                    onDeleteSavedSummary = onDeleteSavedSummary,
                    onEmitMessage = onEmitMessage
                )
            }
        }
    }
}
