package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.McqSessionEntity
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.Subject
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EcetDatePickerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun McqTrackerSection(
    uiState: EcetUiState,
    shouldOpenAddMcq: Boolean,
    onConsumeOpenAddMcq: () -> Unit,
    onAddMcqSession: (Subject, String, Int, Int, Int, Int, Int, Long) -> Unit,
    onDeleteMcqSession: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<McqSessionEntity?>(null) }

    LaunchedEffect(shouldOpenAddMcq) {
        if (shouldOpenAddMcq) {
            showAddDialog = true
            onConsumeOpenAddMcq()
        }
    }

    val totalAttempted = uiState.mcqSessions.sumOf { it.attempted }
    val totalCorrect = uiState.mcqSessions.sumOf { it.correct }
    val overallAccuracy = if (totalAttempted > 0) (totalCorrect * 100f) / totalAttempted else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mcq_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Summary Card
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MCQ Practice Overview", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Self-logged practice sessions · ${uiState.mcqSessions.size} logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f%%", overallAccuracy),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = accuracyColor(overallAccuracy)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Attempted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = totalAttempted.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Correct", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = totalCorrect.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = EmeraldSuccess
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Incorrect", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = (totalAttempted - totalCorrect).coerceAtLeast(0).toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = CoralError
                                )
                            }
                        }
                    }
                }
            }

            // Subject-wise Performance
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Subject-Wise Accuracy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.subjectMcqPerformance.forEach { perf ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${perf.subject.shortName} (${perf.correct}/${perf.attempted})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (perf.attempted > 0) {
                                            String.format(Locale.US, "%.1f%%", perf.accuracyPercent)
                                        } else "—",
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = if (perf.attempted > 0) perf.subject.accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (perf.accuracyPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = perf.subject.accentColor,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // Topic-wise Performance
            if (uiState.topicMcqPerformance.isNotEmpty()) {
                item {
                    EcetCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Topic-Wise Breakdown", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.topicMcqPerformance.take(8).forEach { topicPerf ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectBadge(topicPerf.subject)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = topicPerf.topicName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${topicPerf.correct}/${topicPerf.attempted} (${String.format(Locale.US, "%.0f%%", topicPerf.accuracyPercent)})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = accuracyColor(topicPerf.accuracyPercent)
                                )
                            }
                        }
                    }
                }
            }

            // Practice Log
            item {
                Text(
                    text = "Practice History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.mcqSessions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Quiz,
                        title = "No MCQ practice logged yet",
                        description = "Record your question sets to automatically compute topic-wise and subject-wise accuracy.",
                        actionLabel = "Record MCQ Session",
                        onActionClick = { showAddDialog = true },
                        actionTestTag = "empty_add_mcq_button"
                    )
                }
            } else {
                items(uiState.mcqSessions, key = { it.id }) { session ->
                    val subject = Subject.fromName(session.subject)
                    EcetCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SubjectBadge(subject)
                                    Text(
                                        text = DateUtils.formatEpochDay(session.dateEpochDay),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = session.topicName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Attempted: ${session.attempted} · Correct: ${session.correct} · Wrong: ${session.incorrect} · Unattempted: ${session.unattempted} · ${session.timeTakenMinutes}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", session.accuracyPercent),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = accuracyColor(session.accuracyPercent)
                                )
                                IconButton(
                                    onClick = { sessionPendingDelete = session },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete MCQ log",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_mcq_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Record MCQ practice")
        }
    }

    if (showAddDialog) {
        McqEntryDialog(
            topics = uiState.topics,
            todayEpochDay = uiState.todayEpochDay,
            onDismiss = { showAddDialog = false },
            onSave = { subject, topicName, attempted, correct, incorrect, unattempted, timeMins, dateDay ->
                onAddMcqSession(subject, topicName, attempted, correct, incorrect, unattempted, timeMins, dateDay)
                showAddDialog = false
            }
        )
    }

    sessionPendingDelete?.let { session ->
        ConfirmActionDialog(
            title = "Delete MCQ Record",
            message = "Delete the MCQ log for \"${session.topicName}\"?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteMcqSession(session.id) },
            onDismiss = { sessionPendingDelete = null }
        )
    }
}

private fun accuracyColor(accuracy: Float) = when {
    accuracy >= 75f -> EmeraldSuccess
    accuracy >= 50f -> AmberWarning
    else -> CoralError
}

@Composable
private fun McqEntryDialog(
    topics: List<SyllabusTopicEntity>,
    todayEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (Subject, String, Int, Int, Int, Int, Int, Long) -> Unit
) {
    var subject by remember { mutableStateOf(Subject.ECE) }
    var topicName by remember { mutableStateOf("") }
    var attemptedText by remember { mutableStateOf("") }
    var correctText by remember { mutableStateOf("") }
    var incorrectText by remember { mutableStateOf("") }
    var unattemptedText by remember { mutableStateOf("0") }
    var timeText by remember { mutableStateOf("30") }
    var dateEpochDay by remember { mutableLongStateOf(todayEpochDay) }
    var showTopicDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val subjectTopics = remember(topics, subject) {
        topics.filter { it.subject == subject.name }
    }

    val attemptedPreview = attemptedText.toIntOrNull() ?: 0
    val correctPreview = correctText.toIntOrNull() ?: 0
    val liveAccuracy = if (attemptedPreview > 0 && correctPreview in 0..attemptedPreview) {
        (correctPreview.toFloat() / attemptedPreview.toFloat()) * 100f
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record MCQ Practice") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Subject.entries.forEach { item ->
                        FilterChip(
                            selected = subject == item,
                            onClick = { subject = item },
                            label = { Text(item.shortName) }
                        )
                    }
                }

                Box {
                    OutlinedTextField(
                        value = topicName,
                        onValueChange = { topicName = it; validationError = null },
                        label = { Text("Topic Name *") },
                        trailingIcon = if (subjectTopics.isNotEmpty()) {
                            {
                                IconButton(onClick = { showTopicDropdown = true }) {
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pick saved topic")
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mcq_topic_input")
                    )
                    DropdownMenu(
                        expanded = showTopicDropdown,
                        onDismissRequest = { showTopicDropdown = false }
                    ) {
                        subjectTopics.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.title) },
                                onClick = {
                                    topicName = t.title
                                    showTopicDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attemptedText,
                        onValueChange = { value ->
                            attemptedText = value.filter { it.isDigit() }
                            val att = attemptedText.toIntOrNull()
                            val cor = correctText.toIntOrNull()
                            if (att != null && cor != null && cor <= att) {
                                incorrectText = (att - cor).toString()
                            }
                            validationError = null
                        },
                        label = { Text("Attempted *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mcq_attempted_input")
                    )
                    OutlinedTextField(
                        value = correctText,
                        onValueChange = { value ->
                            correctText = value.filter { it.isDigit() }
                            val att = attemptedText.toIntOrNull()
                            val cor = correctText.toIntOrNull()
                            if (att != null && cor != null && cor <= att) {
                                incorrectText = (att - cor).toString()
                            }
                            validationError = null
                        },
                        label = { Text("Correct *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mcq_correct_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = incorrectText,
                        onValueChange = {
                            incorrectText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Incorrect") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unattemptedText,
                        onValueChange = {
                            unattemptedText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Unattempted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = {
                            timeText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Time (mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = "Date", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateUtils.formatShortDate(dateEpochDay), maxLines = 1)
                    }
                }

                if (liveAccuracy != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calculated Accuracy", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = String.format(Locale.US, "%.1f%%", liveAccuracy),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = accuracyColor(liveAccuracy)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val att = attemptedText.toIntOrNull()
                    val cor = correctText.toIntOrNull()
                    val inc = incorrectText.toIntOrNull() ?: (if (att != null && cor != null) att - cor else null)
                    val unatt = unattemptedText.toIntOrNull() ?: 0
                    val mins = timeText.toIntOrNull() ?: 0

                    when {
                        topicName.trim().isEmpty() -> validationError = "Enter a topic name"
                        att == null || att <= 0 -> validationError = "Attempted questions must be > 0"
                        cor == null || cor < 0 || cor > att -> validationError = "Correct answers must be between 0 and $att"
                        inc == null || inc < 0 || cor + inc != att -> validationError = "Correct + Incorrect must equal Attempted ($att)"
                        unatt < 0 || mins < 0 -> validationError = "Values cannot be negative"
                        else -> onSave(subject, topicName, att, cor, inc, unatt, mins, dateEpochDay)
                    }
                },
                modifier = Modifier.testTag("save_mcq_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        EcetDatePickerDialog(
            initialEpochDay = dateEpochDay,
            onDismiss = { showDatePicker = false },
            onDateSelected = { dateEpochDay = it }
        )
    }
}
