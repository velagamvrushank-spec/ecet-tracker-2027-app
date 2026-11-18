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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SyllabusTopicEntity
import com.example.data.model.Subject
import com.example.data.model.SubjectSummary
import com.example.data.model.TopicPriority
import com.example.data.model.TopicStatus
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EcetDatePickerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun SyllabusScreen(
    uiState: EcetUiState,
    shouldOpenAddTopic: Boolean,
    onConsumeOpenAddTopic: () -> Unit,
    onSaveTopic: (Long, Subject, String, TopicStatus, TopicPriority, String, Long?) -> Unit,
    onUpdateStatus: (SyllabusTopicEntity, TopicStatus) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onImportSyllabus: (String, Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<TopicStatus?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<TopicPriority?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var editingTopic by remember { mutableStateOf<SyllabusTopicEntity?>(null) }
    var showTopicEditor by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var topicPendingDelete by remember { mutableStateOf<SyllabusTopicEntity?>(null) }

    LaunchedEffect(shouldOpenAddTopic) {
        if (shouldOpenAddTopic) {
            editingTopic = null
            showTopicEditor = true
            onConsumeOpenAddTopic()
        }
    }

    val filteredTopics = remember(
        uiState.topics,
        selectedSubject,
        selectedStatusFilter,
        selectedPriorityFilter,
        searchQuery
    ) {
        uiState.topics.filter { topic ->
            val matchesSubject = selectedSubject == null || topic.subject == selectedSubject!!.name
            val matchesStatus = selectedStatusFilter == null || topic.status == selectedStatusFilter!!.name
            val matchesPriority = selectedPriorityFilter == null || topic.priority == selectedPriorityFilter!!.name
            val matchesQuery = searchQuery.isBlank() ||
                topic.title.contains(searchQuery, ignoreCase = true) ||
                topic.notes.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesStatus && matchesPriority && matchesQuery
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("syllabus_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Syllabus Tracker",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Editable 4-subject structure · Import official 2027 topics anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("import_syllabus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = "Import syllabus",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }

            // Subject Section Summary Cards (4 Subjects)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.overview.subjectSummaries.take(2).forEach { summary ->
                            SubjectSummaryCard(
                                summary = summary,
                                isSelected = selectedSubject == summary.subject,
                                onClick = {
                                    selectedSubject = if (selectedSubject == summary.subject) null else summary.subject
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.overview.subjectSummaries.drop(2).forEach { summary ->
                            SubjectSummaryCard(
                                summary = summary,
                                isSelected = selectedSubject == summary.subject,
                                onClick = {
                                    selectedSubject = if (selectedSubject == summary.subject) null else summary.subject
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search topics or notes...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search topics")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("syllabus_search_input")
                )
            }

            // Filter Chips (Status + Priority)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedSubject != null) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedSubject = null },
                            label = { Text("Subject: ${selectedSubject!!.shortName} ×") }
                        )
                    }
                    TopicStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == status) null else status
                            },
                            label = { Text(status.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = status.color().copy(alpha = 0.2f),
                                selectedLabelColor = status.color()
                            )
                        )
                    }
                    TopicPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriorityFilter == priority,
                            onClick = {
                                selectedPriorityFilter = if (selectedPriorityFilter == priority) null else priority
                            },
                            label = { Text("${priority.label} Priority") }
                        )
                    }
                }
            }

            // Empty State or Topic List
            if (filteredTopics.isEmpty()) {
                item {
                    val hasAnyTopics = uiState.topics.isNotEmpty()
                    EmptyStateCard(
                        icon = Icons.Rounded.Book,
                        title = if (hasAnyTopics) "No matching topics" else "Your editable syllabus is empty",
                        description = if (hasAnyTopics) {
                            "Try clearing your search or status/subject filters."
                        } else {
                            "We don't invent unverified 2027 topics. Add your own topics or paste/import the official TG ECET 2027 syllabus."
                        },
                        actionLabel = if (hasAnyTopics) "Clear Filters" else "Add First Topic",
                        onActionClick = {
                            if (hasAnyTopics) {
                                selectedSubject = null
                                selectedStatusFilter = null
                                selectedPriorityFilter = null
                                searchQuery = ""
                            } else {
                                editingTopic = null
                                showTopicEditor = true
                            }
                        },
                        actionTestTag = "empty_syllabus_action"
                    )
                }
            } else {
                items(filteredTopics, key = { it.id }) { topic ->
                    TopicRowCard(
                        topic = topic,
                        onChangeStatus = { newStatus -> onUpdateStatus(topic, newStatus) },
                        onEdit = {
                            editingTopic = topic
                            showTopicEditor = true
                        },
                        onDelete = { topicPendingDelete = topic }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingTopic = null
                showTopicEditor = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_topic_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add topic")
        }
    }

    if (showTopicEditor) {
        TopicEditorDialog(
            initialTopic = editingTopic,
            defaultSubject = selectedSubject ?: Subject.ECE,
            onDismiss = { showTopicEditor = false },
            onSave = { id, subject, title, status, priority, notes, targetDay ->
                onSaveTopic(id, subject, title, status, priority, notes, targetDay)
                showTopicEditor = false
            }
        )
    }

    if (showImportDialog) {
        SyllabusImportDialog(
            defaultSubject = selectedSubject ?: Subject.ECE,
            onDismiss = { showImportDialog = false },
            onImport = { rawInput, subject ->
                onImportSyllabus(rawInput, subject)
                showImportDialog = false
            }
        )
    }

    topicPendingDelete?.let { topic ->
        ConfirmActionDialog(
            title = "Delete Topic",
            message = "Remove \"${topic.title}\" from your syllabus?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteTopic(topic.id) },
            onDismiss = { topicPendingDelete = null }
        )
    }
}

@Composable
private fun SubjectSummaryCard(
    summary: SubjectSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = summary.subject.accentColor
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.subject.shortName,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", summary.completionPercent),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (summary.completionPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${summary.completedTopics}/${summary.totalTopics} done · ${summary.pendingTopics} pending",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopicRowCard(
    topic: SyllabusTopicEntity,
    onChangeStatus: (TopicStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subject = Subject.fromName(topic.subject)
    val status = TopicStatus.fromName(topic.status)
    val priority = TopicPriority.fromName(topic.priority)
    var showStatusMenu by remember { mutableStateOf(false) }

    EcetCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = {
                    val next = if (status == TopicStatus.COMPLETED) {
                        TopicStatus.NOT_STARTED
                    } else {
                        TopicStatus.COMPLETED
                    }
                    onChangeStatus(next)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (status == TopicStatus.COMPLETED) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = "Toggle completed",
                    tint = if (status == TopicStatus.COMPLETED) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubjectBadge(subject)
                    PriorityBadge(priority)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (topic.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topic.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (topic.targetDateEpochDay != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Target date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Target: ${DateUtils.formatEpochDay(topic.targetDateEpochDay)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        StatusPill(
                            status = status,
                            onClick = { showStatusMenu = true }
                        )
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            TopicStatus.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        onChangeStatus(option)
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit topic",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete topic",
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

@Composable
private fun TopicEditorDialog(
    initialTopic: SyllabusTopicEntity?,
    defaultSubject: Subject,
    onDismiss: () -> Unit,
    onSave: (Long, Subject, String, TopicStatus, TopicPriority, String, Long?) -> Unit
) {
    var title by remember { mutableStateOf(initialTopic?.title ?: "") }
    var subject by remember {
        mutableStateOf(initialTopic?.let { Subject.fromName(it.subject) } ?: defaultSubject)
    }
    var status by remember {
        mutableStateOf(initialTopic?.let { TopicStatus.fromName(it.status) } ?: TopicStatus.NOT_STARTED)
    }
    var priority by remember {
        mutableStateOf(initialTopic?.let { TopicPriority.fromName(it.priority) } ?: TopicPriority.MEDIUM)
    }
    var notes by remember { mutableStateOf(initialTopic?.notes ?: "") }
    var targetDate by remember { mutableStateOf(initialTopic?.targetDateEpochDay) }
    var titleError by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTopic == null) "Add Syllabus Topic" else "Edit Topic") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Topic Name *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Please enter a topic name") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_title_input")
                )

                Text("Subject", style = MaterialTheme.typography.labelLarge)
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

                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TopicStatus.entries.forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = { Text(item.label) }
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

                OutlinedButton(
                    onClick = { showTargetDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = "Target date",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (targetDate != null) "Target: ${DateUtils.formatEpochDay(targetDate!!)}"
                        else "Set Target Completion Date (Optional)"
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Notes / Formulas / Refs") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        titleError = true
                    } else {
                        onSave(initialTopic?.id ?: 0L, subject, title, status, priority, notes, targetDate)
                    }
                },
                modifier = Modifier.testTag("save_topic_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showTargetDatePicker) {
        EcetDatePickerDialog(
            initialEpochDay = targetDate,
            onDismiss = { showTargetDatePicker = false },
            onDateSelected = { targetDate = it },
            onClearDate = { targetDate = null }
        )
    }
}

@Composable
private fun SyllabusImportDialog(
    defaultSubject: Subject,
    onDismiss: () -> Unit,
    onImport: (String, Subject) -> Unit
) {
    var subject by remember { mutableStateOf(defaultSubject) }
    var rawInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Syllabus Topics") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste official TG ECET 2027 topics (one topic per line) or a JSON array/object. Selecting a subject applies to plain text lines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it
                        inputError = false
                    },
                    label = { Text("Topics List or JSON") },
                    placeholder = { Text("Matrices & Determinants\nDifferential Equations\nLaplace Transforms") },
                    isError = inputError,
                    supportingText = if (inputError) {
                        { Text("Paste at least one topic line or valid JSON") }
                    } else null,
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawInput.trim().isEmpty()) {
                        inputError = true
                    } else {
                        onImport(rawInput, subject)
                    }
                }
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
