package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DailyTaskEntity
import com.example.data.model.StudySubTab
import com.example.data.model.TimerUiState
import com.example.ui.components.EcetCard
import com.example.ui.components.EcetDatePickerDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: EcetUiState,
    timerState: TimerUiState,
    onSetExamDate: (Long?) -> Unit,
    onQuickStartTimer: () -> Unit,
    onQuickAddTopic: () -> Unit,
    onQuickAddTask: () -> Unit,
    onQuickRecordMcq: () -> Unit,
    onQuickRecordMock: () -> Unit,
    onToggleTask: (DailyTaskEntity) -> Unit,
    onOpenSyllabus: () -> Unit,
    onOpenStudyTab: (StudySubTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExamDatePicker by remember { mutableStateOf(false) }

    val prefs = uiState.preferences
    val todayTasks = remember(uiState.dailyTasks, uiState.todayEpochDay) {
        uiState.dailyTasks.filter { it.dateEpochDay == uiState.todayEpochDay }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("home_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prefs.targetExam.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Welcome, ${prefs.studentName}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = prefs.branch,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (uiState.analytics.currentStreakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = AmberWarning.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Study streak",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.analytics.currentStreakDays}d",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }

            // Active Timer Live Banner (if running)
            if (timerState.isRunning || timerState.elapsedMs > 0L) {
                item {
                    Surface(
                        onClick = onQuickStartTimer,
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "Active study timer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (timerState.isRunning) {
                                            "Studying ${timerState.subject.shortName} · ${timerState.mode.label}"
                                        } else {
                                            "Paused · ${timerState.subject.shortName}"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (!timerState.topicTitle.isNullOrBlank()) {
                                        Text(
                                            text = timerState.topicTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Text(
                                text = DateUtils.formatDurationHms(
                                    if (timerState.mode == com.example.data.model.StudyMode.POMODORO) {
                                        timerState.remainingMs
                                    } else {
                                        timerState.elapsedMs
                                    }
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // A. ECET Countdown Card
            item {
                val examDay = prefs.examDateEpochDay
                val daysRemaining = examDay?.let { it - uiState.todayEpochDay }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "ECET Countdown",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TG ECET 2027 COUNTDOWN",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { showExamDatePicker = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("set_exam_date_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Set or edit exam date",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (examDay == null) "Set Date" else "Update")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (examDay == null || daysRemaining == null) {
                                Text(
                                    text = "Exam date not set",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Set your official TG ECET 2027 exam date once announced to track your daily countdown.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = when {
                                            daysRemaining > 0 -> daysRemaining.toString()
                                            daysRemaining == 0L -> "TODAY"
                                            else -> "0"
                                        },
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Text(
                                            text = when {
                                                daysRemaining == 1L -> "day remaining"
                                                daysRemaining > 1L -> "days remaining"
                                                daysRemaining == 0L -> "Exam Day"
                                                else -> "Exam date passed (${-daysRemaining}d ago)"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Target: ${DateUtils.formatEpochDay(examDay)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // D. Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Quick Actions",
                        subtitle = "Jump straight into study & tracking"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Rounded.PlayArrow,
                            label = "Start Timer",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onQuickStartTimer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_timer")
                        )
                        QuickActionTile(
                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            label = "Add Topic",
                            accent = VioletSecondary,
                            onClick = onQuickAddTopic,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_topic")
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.AddTask,
                            label = "Add Task",
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onQuickAddTask,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_task")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Rounded.Quiz,
                            label = "Record MCQ Practice",
                            accent = AmberWarning,
                            onClick = onQuickRecordMcq,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_mcq")
                        )
                        QuickActionTile(
                            icon = Icons.AutoMirrored.Rounded.Assignment,
                            label = "Record Mock Test",
                            accent = CobaltPrimary,
                            onClick = onQuickRecordMock,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_mock")
                        )
                    }
                }
            }

            // B. Preparation Overview
            item {
                val ov = uiState.overview
                EcetCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Preparation Overview",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Syllabus completion across 4 subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onOpenSyllabus) {
                            Text("View Syllabus")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overall Completion",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", ov.completionPercent),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (ov.completionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniBox(
                            label = "Total",
                            value = ov.totalTopics.toString(),
                            accent = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "Completed",
                            value = ov.completedTopics.toString(),
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "In Progress",
                            value = ov.inProgressTopics.toString(),
                            accent = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "Pending",
                            value = ov.pendingTopics.toString(),
                            accent = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (ov.totalTopics == 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No syllabus topics added yet. Add topics or import your official 2027 syllabus from the Syllabus tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ov.subjectSummaries.forEach { summary ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = summary.subject.accentColor.copy(alpha = 0.1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = summary.subject.shortName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = summary.subject.accentColor
                                        )
                                        Text(
                                            text = "${summary.completedTopics}/${summary.totalTopics}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Study Statistics
            item {
                val stats = uiState.studyStats
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Study Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onOpenStudyTab(StudySubTab.TIMER) }) {
                            Text("Open Timer")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniBox(
                            label = "Today",
                            value = DateUtils.formatHoursDecimal(stats.todayStudySeconds),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "This Week",
                            value = DateUtils.formatHoursDecimal(stats.weeklyStudySeconds),
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "Total",
                            value = DateUtils.formatHoursDecimal(stats.totalStudySeconds),
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Goal (${DateUtils.formatHoursCompact(stats.dailyTargetMinutes)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f%%", stats.dailyTargetPercent),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = if (stats.dailyTargetPercent >= 100f) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (stats.dailyTargetPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = if (stats.dailyTargetPercent >= 100f) EmeraldSuccess else MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            // Today's Tasks Quick Preview
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val doneCount = todayTasks.count { it.isCompleted }
                        Column {
                            Text(
                                text = "Today's Tasks",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (todayTasks.isEmpty()) "No tasks scheduled for today"
                                else "$doneCount of ${todayTasks.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenStudyTab(StudySubTab.TASKS) }) {
                            Text("Manage")
                        }
                    }

                    if (todayTasks.isEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onQuickAddTask,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddTask,
                                contentDescription = "Add today's task",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Plan Today's First Task")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        todayTasks.take(4).forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleTask(task) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) {
                                            Icons.Rounded.CheckCircle
                                        } else {
                                            Icons.Rounded.RadioButtonUnchecked
                                        },
                                        contentDescription = if (task.isCompleted) "Mark pending" else "Mark completed",
                                        tint = if (task.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (task.subject != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SubjectBadge(com.example.data.model.Subject.fromName(task.subject))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showExamDatePicker) {
        EcetDatePickerDialog(
            initialEpochDay = prefs.examDateEpochDay,
            onDismiss = { showExamDatePicker = false },
            onDateSelected = { epochDay -> onSetExamDate(epochDay) },
            onClearDate = { onSetExamDate(null) }
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                color = accent,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
