package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DailyTaskEntity
import com.example.data.model.Subject
import com.example.data.model.TopicPriority
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun DailyTasksSection(
    uiState: EcetUiState,
    shouldOpenAddTask: Boolean,
    onConsumeOpenAddTask: () -> Unit,
    onSaveTask: (Long, String, Subject?, TopicPriority, Long, Boolean) -> Unit,
    onToggleTask: (DailyTaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(uiState.todayEpochDay) }
    var showMonthCalendar by remember { mutableStateOf(false) }

    val initialCivil = remember(selectedDate) { DateUtils.epochDayToCivil(selectedDate) }
    var calendarYear by remember { mutableIntStateOf(initialCivil.year) }
    var calendarMonth by remember { mutableIntStateOf(initialCivil.month) }

    var editingTask by remember { mutableStateOf<DailyTaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<DailyTaskEntity?>(null) }

    LaunchedEffect(shouldOpenAddTask) {
        if (shouldOpenAddTask) {
            selectedDate = uiState.todayEpochDay
            editingTask = null
            showTaskDialog = true
            onConsumeOpenAddTask()
        }
    }

    val tasksForSelectedDate = remember(uiState.dailyTasks, selectedDate) {
        uiState.dailyTasks.filter { it.dateEpochDay == selectedDate }
    }
    val completedCount = tasksForSelectedDate.count { it.isCompleted }
    val pendingCount = tasksForSelectedDate.size - completedCount

    val studySecondsForSelectedDate = remember(uiState.studySessions, selectedDate) {
        uiState.studySessions.filter { it.dateEpochDay == selectedDate }.sumOf { it.durationSeconds }
    }
    val targetSeconds = (uiState.preferences.dailyStudyTargetMinutes * 60f).coerceAtLeast(60f)
    val dailyTargetPct = (studySecondsForSelectedDate / targetSeconds) * 100f

    val taskDatesSet = remember(uiState.dailyTasks) {
        uiState.dailyTasks.map { it.dateEpochDay }.toSet()
    }
    val studyDatesSet = remember(uiState.studySessions) {
        uiState.studySessions.map { it.dateEpochDay }.toSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calendar Header + Week / Month Switcher
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = DateUtils.formatEpochDay(selectedDate),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedDate == uiState.todayEpochDay) "Today" else DateUtils.formatWeekdayShort(selectedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row {
                            if (selectedDate != uiState.todayEpochDay) {
                                TextButton(onClick = {
                                    selectedDate = uiState.todayEpochDay
                                    val c = DateUtils.epochDayToCivil(uiState.todayEpochDay)
                                    calendarYear = c.year
                                    calendarMonth = c.month
                                }) {
                                    Text("Today")
                                }
                            }
                            IconButton(
                                onClick = { showMonthCalendar = !showMonthCalendar },
                                modifier = Modifier.testTag("toggle_month_calendar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Toggle month calendar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!showMonthCalendar) {
                        // 7-day strip centered around selectedDate
                        val stripDays = (-3..3).map { selectedDate + it }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            stripDays.forEach { day ->
                                val isSelected = day == selectedDate
                                val hasDot = taskDatesSet.contains(day) || studyDatesSet.contains(day)
                                val civil = DateUtils.epochDayToCivil(day)
                                Surface(
                                    onClick = { selectedDate = day },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (day == uiState.todayEpochDay && !isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = DateUtils.formatWeekdayShort(day).take(2),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = civil.day.toString(),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        !hasDot -> androidx.compose.ui.graphics.Color.Transparent
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        else -> EmeraldSuccess
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Full Month Calendar History View
                        MonthCalendarGrid(
                            year = calendarYear,
                            month = calendarMonth,
                            selectedEpochDay = selectedDate,
                            todayEpochDay = uiState.todayEpochDay,
                            taskDates = taskDatesSet,
                            studyDates = studyDatesSet,
                            onPreviousMonth = {
                                if (calendarMonth == 1) {
                                    calendarMonth = 12
                                    calendarYear -= 1
                                } else {
                                    calendarMonth -= 1
                                }
                            },
                            onNextMonth = {
                                if (calendarMonth == 12) {
                                    calendarMonth = 1
                                    calendarYear += 1
                                } else {
                                    calendarMonth += 1
                                }
                            },
                            onSelectDay = { selectedDate = it }
                        )
                    }
                }
            }

            // Daily Progress & Counts Card
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Progress", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Pending: $pendingCount · Completed: $completedCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${DateUtils.formatHoursDecimal(studySecondsForSelectedDate)} / ${DateUtils.formatHoursCompact(uiState.preferences.dailyStudyTargetMinutes)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (dailyTargetPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = if (dailyTargetPct >= 100f) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                }
            }

            if (tasksForSelectedDate.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.TaskAlt,
                        title = "No tasks for ${DateUtils.formatShortDate(selectedDate)}",
                        description = "Add study goals, problem sets, or revision tasks for this date.",
                        actionLabel = "Add Task",
                        onActionClick = {
                            editingTask = null
                            showTaskDialog = true
                        },
                        actionTestTag = "empty_add_task_button"
                    )
                }
            } else {
                items(tasksForSelectedDate, key = { it.id }) { task ->
                    val subject = task.subject?.let { Subject.fromName(it) }
                    val priority = TopicPriority.fromName(task.priority)
                    EcetCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleTask(task) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.RadioButtonUnchecked
                                    },
                                    contentDescription = "Toggle task completed",
                                    tint = if (task.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PriorityBadge(priority)
                                    if (subject != null) SubjectBadge(subject)
                                }
                            }
                            IconButton(
                                onClick = {
                                    editingTask = task
                                    showTaskDialog = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit task",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { taskPendingDelete = task },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete task",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingTask = null
                showTaskDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_daily_task_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add daily task")
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            initialTask = editingTask,
            defaultEpochDay = selectedDate,
            onDismiss = { showTaskDialog = false },
            onSave = { id, title, subject, priority, dateDay, completed ->
                onSaveTask(id, title, subject, priority, dateDay, completed)
                showTaskDialog = false
            }
        )
    }

    taskPendingDelete?.let { task ->
        ConfirmActionDialog(
            title = "Delete Task",
            message = "Remove \"${task.title}\"?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteTask(task.id) },
            onDismiss = { taskPendingDelete = null }
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    year: Int,
    month: Int,
    selectedEpochDay: Long,
    todayEpochDay: Long,
    taskDates: Set<Long>,
    studyDates: Set<Long>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit
) {
    val firstDay = DateUtils.civilToEpochDay(year, month, 1)
    val startOffset = DateUtils.dayOfWeekIndexMondayStart(firstDay)
    val daysCount = DateUtils.daysInMonth(year, month)
    val totalCells = ((startOffset + daysCount + 6) / 7) * 7
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = DateUtils.formatMonthYear(year, month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        for (week in 0 until (totalCells / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (dow in 0 until 7) {
                    val cellIndex = week * 7 + dow
                    val dayNum = cellIndex - startOffset + 1
                    if (dayNum in 1..daysCount) {
                        val epochDay = DateUtils.civilToEpochDay(year, month, dayNum)
                        val isSelected = epochDay == selectedEpochDay
                        val hasActivity = taskDates.contains(epochDay) || studyDates.contains(epochDay)
                        Surface(
                            onClick = { onSelectDay(epochDay) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (epochDay == todayEpochDay && !isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasActivity) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else EmeraldSuccess
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    initialTask: DailyTaskEntity?,
    defaultEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String, Subject?, TopicPriority, Long, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var subject by remember {
        mutableStateOf(initialTask?.subject?.let { Subject.fromName(it) })
    }
    var priority by remember {
        mutableStateOf(initialTask?.let { TopicPriority.fromName(it.priority) } ?: TopicPriority.MEDIUM)
    }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTask == null) "Add Daily Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Task Description *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Please enter a task description") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Text("Subject Tag (Optional)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = subject == null,
                        onClick = { subject = null },
                        label = { Text("General") }
                    )
                    Subject.entries.forEach { item ->
                        FilterChip(
                            selected = subject == item,
                            onClick = { subject = item },
                            label = { Text(item.shortName) }
                        )
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TopicPriority.entries.forEach { item ->
                        FilterChip(
                            selected = priority == item,
                            onClick = { priority = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        titleError = true
                    } else {
                        onSave(
                            initialTask?.id ?: 0L,
                            title,
                            subject,
                            priority,
                            initialTask?.dateEpochDay ?: defaultEpochDay,
                            initialTask?.isCompleted ?: false
                        )
                    }
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
