package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.MockTestEntity
import com.example.data.model.Subject
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EcetDatePickerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun MockTestSection(
    uiState: EcetUiState,
    shouldOpenAddMock: Boolean,
    onConsumeOpenAddMock: () -> Unit,
    onSaveMockTest: (Long, String, Long, Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, String) -> Unit,
    onDeleteMockTest: (Long) -> Unit
) {
    var editingMock by remember { mutableStateOf<MockTestEntity?>(null) }
    var showMockDialog by remember { mutableStateOf(false) }
    var mockPendingDelete by remember { mutableStateOf<MockTestEntity?>(null) }

    LaunchedEffect(shouldOpenAddMock) {
        if (shouldOpenAddMock) {
            editingMock = null
            showMockDialog = true
            onConsumeOpenAddMock()
        }
    }

    val sortedMocksDesc = remember(uiState.mockTests) {
        uiState.mockTests.sortedWith(
            compareByDescending<MockTestEntity> { it.testDateEpochDay }.thenByDescending { it.createdAtMs }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mock Performance Chart
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mock Test Performance", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Score percentage across tests",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.mockTests.isNotEmpty()) {
                            val best = uiState.mockTests.maxOf {
                                if (it.maxTotalMarks > 0f) (it.totalMarks / it.maxTotalMarks) * 100f else 0f
                            }
                            Text(
                                text = "Best: ${String.format(Locale.US, "%.1f%%", best)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = EmeraldSuccess
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TrendLineChart(
                        points = uiState.analytics.mockScoreSeries,
                        lineColor = MaterialTheme.colorScheme.primary,
                        emptyMessage = "Log your first mock test to see score trends."
                    )
                }
            }

            item {
                Text(
                    text = "Test History (${sortedMocksDesc.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (sortedMocksDesc.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        title = "No mock tests recorded yet",
                        description = "Track full-length or subject mock exams with Math, Physics, Chemistry, and ECE marks.",
                        actionLabel = "Add Mock Test",
                        onActionClick = {
                            editingMock = null
                            showMockDialog = true
                        },
                        actionTestTag = "empty_add_mock_button"
                    )
                }
            } else {
                items(sortedMocksDesc, key = { it.id }) { test ->
                    val scorePct = if (test.maxTotalMarks > 0f) {
                        (test.totalMarks / test.maxTotalMarks) * 100f
                    } else 0f
                    val accuracy = if (test.attempted > 0) {
                        (test.correct.toFloat() / test.attempted.toFloat()) * 100f
                    } else 0f

                    EcetCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = test.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = DateUtils.formatEpochDay(test.testDateEpochDay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatMark(test.totalMarks)} / ${formatMark(test.maxTotalMarks)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", scorePct),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MockSubjectChip(Subject.MATHEMATICS, test.mathMarks, test.mathMaxMarks, Modifier.weight(1f))
                            MockSubjectChip(Subject.PHYSICS, test.physicsMarks, test.physicsMaxMarks, Modifier.weight(1f))
                            MockSubjectChip(Subject.CHEMISTRY, test.chemistryMarks, test.chemistryMaxMarks, Modifier.weight(1f))
                            MockSubjectChip(Subject.ECE, test.eceMarks, test.eceMaxMarks, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attempted ${test.attempted} · Correct ${test.correct} · Wrong ${test.incorrect} · Acc ${String.format(Locale.US, "%.0f%%", accuracy)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        editingMock = test
                                        showMockDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit mock test",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { mockPendingDelete = test },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete mock test",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (test.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = test.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingMock = null
                showMockDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_mock_test_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add mock test")
        }
    }

    if (showMockDialog) {
        MockTestEditorDialog(
            initialMock = editingMock,
            todayEpochDay = uiState.todayEpochDay,
            onDismiss = { showMockDialog = false },
            onSave = { id, title, dateDay, math, mathMax, phy, phyMax, chem, chemMax, ece, eceMax, att, cor, inc, notes ->
                onSaveMockTest(
                    id, title, dateDay, math, mathMax, phy, phyMax, chem, chemMax, ece, eceMax, att, cor, inc, notes
                )
                showMockDialog = false
            }
        )
    }

    mockPendingDelete?.let { mock ->
        ConfirmActionDialog(
            title = "Delete Mock Test",
            message = "Delete \"${mock.title}\" from your mock history?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteMockTest(mock.id) },
            onDismiss = { mockPendingDelete = null }
        )
    }
}

@Composable
private fun MockSubjectChip(
    subject: Subject,
    marks: Float,
    maxMarks: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = subject.accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, subject.accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subject.shortName,
                style = MaterialTheme.typography.labelMedium,
                color = subject.accentColor
            )
            Text(
                text = "${formatMark(marks)}/${formatMark(maxMarks)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatMark(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)
}

@Composable
private fun MockTestEditorDialog(
    initialMock: MockTestEntity?,
    todayEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String, Long, Float, Float, Float, Float, Float, Float, Float, Float, Int, Int, Int, String) -> Unit
) {
    var title by remember { mutableStateOf(initialMock?.title ?: "") }
    var testDate by remember { mutableLongStateOf(initialMock?.testDateEpochDay ?: todayEpochDay) }
    var mathMarks by remember { mutableStateOf(initialMock?.mathMarks?.let { formatMark(it) } ?: "") }
    var mathMax by remember { mutableStateOf(initialMock?.mathMaxMarks?.let { formatMark(it) } ?: "50") }
    var phyMarks by remember { mutableStateOf(initialMock?.physicsMarks?.let { formatMark(it) } ?: "") }
    var phyMax by remember { mutableStateOf(initialMock?.physicsMaxMarks?.let { formatMark(it) } ?: "25") }
    var chemMarks by remember { mutableStateOf(initialMock?.chemistryMarks?.let { formatMark(it) } ?: "") }
    var chemMax by remember { mutableStateOf(initialMock?.chemistryMaxMarks?.let { formatMark(it) } ?: "25") }
    var eceMarks by remember { mutableStateOf(initialMock?.eceMarks?.let { formatMark(it) } ?: "") }
    var eceMax by remember { mutableStateOf(initialMock?.eceMaxMarks?.let { formatMark(it) } ?: "100") }
    var attempted by remember { mutableStateOf(initialMock?.attempted?.toString() ?: "") }
    var correct by remember { mutableStateOf(initialMock?.correct?.toString() ?: "") }
    var incorrect by remember { mutableStateOf(initialMock?.incorrect?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialMock?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val totalPreview = (mathMarks.toFloatOrNull() ?: 0f) +
        (phyMarks.toFloatOrNull() ?: 0f) +
        (chemMarks.toFloatOrNull() ?: 0f) +
        (eceMarks.toFloatOrNull() ?: 0f)
    val maxPreview = (mathMax.toFloatOrNull() ?: 0f) +
        (phyMax.toFloatOrNull() ?: 0f) +
        (chemMax.toFloatOrNull() ?: 0f) +
        (eceMax.toFloatOrNull() ?: 0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMock == null) "Record Mock Test" else "Edit Mock Test") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Mock Test Name *") },
                    placeholder = { Text("e.g., Full Mock Test #1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mock_title_input")
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = "Test date", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Date: ${DateUtils.formatEpochDay(testDate)}")
                }

                Text("Subject-wise Marks / Max Marks", style = MaterialTheme.typography.labelLarge)
                SubjectMarkRow("Math", mathMarks, { mathMarks = it; errorMsg = null }, mathMax, { mathMax = it; errorMsg = null })
                SubjectMarkRow("Physics", phyMarks, { phyMarks = it; errorMsg = null }, phyMax, { phyMax = it; errorMsg = null })
                SubjectMarkRow("Chem", chemMarks, { chemMarks = it; errorMsg = null }, chemMax, { chemMax = it; errorMsg = null })
                SubjectMarkRow("ECE", eceMarks, { eceMarks = it; errorMsg = null }, eceMax, { eceMax = it; errorMsg = null })

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Score", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${formatMark(totalPreview)} / ${formatMark(maxPreview)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = attempted,
                        onValueChange = { attempted = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Attempted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = correct,
                        onValueChange = { correct = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Correct") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = incorrect,
                        onValueChange = { incorrect = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("Wrong") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Analysis Notes (Optional)") },
                    minLines = 2,
                    maxLines = 3,
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
                    val m = mathMarks.toFloatOrNull() ?: 0f
                    val mMax = mathMax.toFloatOrNull() ?: 0f
                    val p = phyMarks.toFloatOrNull() ?: 0f
                    val pMax = phyMax.toFloatOrNull() ?: 0f
                    val c = chemMarks.toFloatOrNull() ?: 0f
                    val cMax = chemMax.toFloatOrNull() ?: 0f
                    val e = eceMarks.toFloatOrNull() ?: 0f
                    val eMax = eceMax.toFloatOrNull() ?: 0f
                    val att = attempted.toIntOrNull() ?: 0
                    val cor = correct.toIntOrNull() ?: 0
                    val inc = incorrect.toIntOrNull() ?: 0

                    when {
                        title.trim().isEmpty() -> errorMsg = "Enter a mock test title"
                        mMax <= 0f || pMax <= 0f || cMax <= 0f || eMax <= 0f ->
                            errorMsg = "Max marks must be greater than 0"
                        m < 0f || p < 0f || c < 0f || e < 0f ->
                            errorMsg = "Marks cannot be negative"
                        m > mMax || p > pMax || c > cMax || e > eMax ->
                            errorMsg = "Subject marks cannot exceed max marks"
                        att < 0 || cor < 0 || inc < 0 ->
                            errorMsg = "Question counts cannot be negative"
                        cor + inc > att ->
                            errorMsg = "Correct + Wrong cannot exceed Attempted"
                        else -> onSave(
                            initialMock?.id ?: 0L,
                            title,
                            testDate,
                            m, mMax,
                            p, pMax,
                            c, cMax,
                            e, eMax,
                            att, cor, inc,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("save_mock_button")
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
            initialEpochDay = testDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { testDate = it }
        )
    }
}

@Composable
private fun SubjectMarkRow(
    label: String,
    marks: String,
    onMarksChange: (String) -> Unit,
    maxMarks: String,
    onMaxMarksChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(56.dp)
        )
        OutlinedTextField(
            value = marks,
            onValueChange = { onMarksChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text("Marks") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = maxMarks,
            onValueChange = { onMaxMarksChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text("Max") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
