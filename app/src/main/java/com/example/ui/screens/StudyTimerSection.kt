package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.StudySessionEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.PomodoroPhase
import com.example.data.model.StudyMode
import com.example.data.model.Subject
import com.example.data.model.TimerUiState
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils

@Composable
fun StudyTimerSection(
    uiState: EcetUiState,
    timerState: TimerUiState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onFinishAndSave: () -> Unit,
    onSetMode: (StudyMode) -> Unit,
    onSetPhase: (PomodoroPhase) -> Unit,
    onSelectSubject: (Subject) -> Unit,
    onSelectTopic: (SyllabusTopicEntity?) -> Unit,
    onUpdatePomodoroDurations: (Int, Int, Int) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    var showTopicMenu by remember { mutableStateOf(false) }
    var showPomodoroSettings by remember { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<StudySessionEntity?>(null) }

    val subjectTopics = remember(uiState.topics, timerState.subject) {
        uiState.topics.filter { it.subject == timerState.subject.name }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Switcher + Pomodoro Settings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyMode.entries.forEach { mode ->
                        FilterChip(
                            selected = timerState.mode == mode,
                            onClick = { onSetMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("timer_mode_${mode.name.lowercase()}")
                        )
                    }
                }
                if (timerState.mode == StudyMode.POMODORO) {
                    IconButton(
                        onClick = { showPomodoroSettings = true },
                        modifier = Modifier.testTag("pomodoro_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Customize Pomodoro durations"
                        )
                    }
                }
            }
        }

        // Pomodoro Phases
        if (timerState.mode == StudyMode.POMODORO) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PomodoroPhase.entries.forEach { phase ->
                        val mins = when (phase) {
                            PomodoroPhase.FOCUS -> timerState.focusMinutes
                            PomodoroPhase.SHORT_BREAK -> timerState.shortBreakMinutes
                            PomodoroPhase.LONG_BREAK -> timerState.longBreakMinutes
                        }
                        FilterChip(
                            selected = timerState.phase == phase,
                            onClick = { onSetPhase(phase) },
                            label = { Text("${phase.label} (${mins}m)") }
                        )
                    }
                }
            }
        }

        // Subject & Topic Picker
        item {
            EcetCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Study Subject & Topic",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Subject.entries.forEach { subject ->
                        FilterChip(
                            selected = timerState.subject == subject,
                            onClick = { onSelectSubject(subject) },
                            label = { Text(subject.shortName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { showTopicMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = timerState.topicTitle ?: "Topic: General ${timerState.subject.shortName} Study",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select topic")
                    }
                    DropdownMenu(
                        expanded = showTopicMenu,
                        onDismissRequest = { showTopicMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General ${timerState.subject.shortName} Study") },
                            onClick = {
                                onSelectTopic(null)
                                showTopicMenu = false
                            }
                        )
                        subjectTopics.forEach { topic ->
                            DropdownMenuItem(
                                text = { Text(topic.title) },
                                onClick = {
                                    onSelectTopic(topic)
                                    showTopicMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Main Timer Dial & Controls
        item {
            EcetCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val accent = if (timerState.mode == StudyMode.POMODORO && timerState.phase != PomodoroPhase.FOCUS) {
                        EmeraldSuccess
                    } else {
                        timerState.subject.accentColor
                    }
                    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

                    val progress = if (timerState.mode == StudyMode.POMODORO && timerState.targetDurationMs > 0L) {
                        (timerState.elapsedMs.toFloat() / timerState.targetDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        ((timerState.elapsedMs % 3_600_000L) / 3_600_000f).coerceIn(0f, 1f)
                    }

                    Box(
                        modifier = Modifier.size(216.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(216.dp)) {
                            val stroke = 12.dp.toPx()
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            val topLeft = Offset(stroke / 2f, stroke / 2f)
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = accent,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (timerState.mode == StudyMode.POMODORO) {
                                    "${timerState.phase.label} · #${timerState.pomodoroRound}"
                                } else {
                                    "Focus Stopwatch"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayMs = if (timerState.mode == StudyMode.POMODORO) {
                                timerState.remainingMs
                            } else {
                                timerState.elapsedMs
                            }
                            Text(
                                text = DateUtils.formatDurationHms(displayMs),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (timerState.isRunning) "Running (background-safe)" else if (timerState.elapsedMs > 0L) "Paused" else "Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Controls: Start/Resume, Pause, Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (timerState.isRunning) {
                            Button(
                                onClick = onPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_pause_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause timer")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }
                        } else {
                            Button(
                                onClick = onStartOrResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_start_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Start or resume timer")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (timerState.elapsedMs > 0L) "Resume" else "Start")
                            }
                        }

                        OutlinedButton(
                            onClick = onReset,
                            enabled = timerState.elapsedMs > 0L || timerState.isRunning,
                            modifier = Modifier.testTag("timer_reset_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }

                    if (timerState.elapsedMs >= 10_000L && (timerState.mode == StudyMode.STOPWATCH || timerState.phase == PomodoroPhase.FOCUS)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = onFinishAndSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timer_save_session_button")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "Finish and save study session")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finish & Log Session (${DateUtils.formatDurationHms(timerState.elapsedMs)})")
                        }
                    }
                }
            }
        }

        // Daily & Weekly Totals Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Today's Study", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = DateUtils.formatHoursDecimal(uiState.studyStats.todayStudySeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Weekly Study", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = DateUtils.formatHoursDecimal(uiState.studyStats.weeklyStudySeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Session History
        item {
            Text(
                text = "Session History (${uiState.studySessions.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.studySessions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.History,
                    title = "No study sessions logged yet",
                    description = "Start the Stopwatch or Pomodoro timer and save your session to build your study log."
                )
            }
        } else {
            items(uiState.studySessions.take(30), key = { it.id }) { session ->
                val subject = Subject.fromName(session.subject)
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SubjectBadge(subject)
                                Text(
                                    text = StudyMode.fromName(session.mode).label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = session.topicTitle ?: "General ${subject.shortName} Study",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${DateUtils.formatEpochDay(session.dateEpochDay)} · ${DateUtils.formatTimeOfDay(session.endedAtMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = DateUtils.formatDurationHms(session.durationSeconds * 1000L),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            IconButton(onClick = { sessionPendingDelete = session }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete session",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showPomodoroSettings) {
        PomodoroSettingsDialog(
            focusMinutes = timerState.focusMinutes,
            shortBreakMinutes = timerState.shortBreakMinutes,
            longBreakMinutes = timerState.longBreakMinutes,
            onDismiss = { showPomodoroSettings = false },
            onSave = { focus, shortB, longB ->
                onUpdatePomodoroDurations(focus, shortB, longB)
                showPomodoroSettings = false
            }
        )
    }

    sessionPendingDelete?.let { session ->
        ConfirmActionDialog(
            title = "Delete Study Session",
            message = "Remove this ${DateUtils.formatDurationHms(session.durationSeconds * 1000L)} session from history?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteSession(session.id) },
            onDismiss = { sessionPendingDelete = null }
        )
    }
}

@Composable
private fun PomodoroSettingsDialog(
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var focusText by remember { mutableStateOf(focusMinutes.toString()) }
    var shortText by remember { mutableStateOf(shortBreakMinutes.toString()) }
    var longText by remember { mutableStateOf(longBreakMinutes.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Pomodoro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it; errorMsg = null },
                    label = { Text("Focus Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortText,
                    onValueChange = { shortText = it; errorMsg = null },
                    label = { Text("Short Break (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longText,
                    onValueChange = { longText = it; errorMsg = null },
                    label = { Text("Long Break (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = focusText.toIntOrNull()
                    val s = shortText.toIntOrNull()
                    val l = longText.toIntOrNull()
                    if (f == null || f !in 1..180 || s == null || s !in 1..60 || l == null || l !in 1..90) {
                        errorMsg = "Enter valid durations (Focus 1–180m, Breaks 1–90m)"
                    } else {
                        onSave(f, s, l)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
