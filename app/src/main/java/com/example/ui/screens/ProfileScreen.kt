package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ThemeMode
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EcetCard
import com.example.ui.components.EcetDatePickerDialog
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    uiState: EcetUiState,
    onUpdateProfile: (String, String, String, Int) -> Unit,
    onUpdateExamDate: (Long?) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateNotifications: (Boolean, Boolean, Int, Int) -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onResetAllData: () -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = uiState.preferences

    var name by remember(prefs.studentName) { mutableStateOf(prefs.studentName) }
    var branch by remember(prefs.branch) { mutableStateOf(prefs.branch) }
    var targetExam by remember(prefs.targetExam) { mutableStateOf(prefs.targetExam) }
    var dailyTargetHours by remember(prefs.dailyStudyTargetMinutes) {
        mutableStateOf((prefs.dailyStudyTargetMinutes / 60f).let {
            if (it % 1f == 0f) it.toInt().toString() else String.format("%.1f", it)
        })
    }

    var showExamDatePicker by remember { mutableStateOf(false) }
    var showPasteRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var profileError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefs) {
        name = prefs.studentName
        branch = prefs.branch
        targetExam = prefs.targetExam
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateNotifications(true, prefs.pomodoroAlertsEnabled, prefs.reminderHour, prefs.reminderMinute)
        } else {
            onUpdateNotifications(false, prefs.pomodoroAlertsEnabled, prefs.reminderHour, prefs.reminderMinute)
            onEmitMessage("Notification permission denied. Alerts remain off.")
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = onExportBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                onEmitMessage(
                    if (ok) "Backup exported to JSON file." else "Could not write backup file."
                )
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }.getOrNull()
                }
                if (content.isNullOrBlank()) {
                    onEmitMessage("Selected file is empty or unreadable.")
                } else {
                    pendingRestoreJson = content
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Profile & Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "100% offline local storage · JSON backup & restore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Student Profile
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = "Student profile", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Student Profile", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; profileError = null },
                        label = { Text("Student Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it; profileError = null },
                        label = { Text("Branch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetExam,
                            onValueChange = { targetExam = it; profileError = null },
                            label = { Text("Target Exam") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = dailyTargetHours,
                            onValueChange = {
                                dailyTargetHours = it.filter { c -> c.isDigit() || c == '.' }
                                profileError = null
                            },
                            label = { Text("Daily Goal (hrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showExamDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Examination date", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (prefs.examDateEpochDay != null) {
                                "Exam Date: ${DateUtils.formatEpochDay(prefs.examDateEpochDay)}"
                            } else {
                                "Set Official Exam Date"
                            }
                        )
                    }
                    if (profileError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profileError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val hrs = dailyTargetHours.toFloatOrNull()
                            if (name.trim().isEmpty()) {
                                profileError = "Please enter your name"
                            } else if (hrs == null || hrs < 0.25f || hrs > 16f) {
                                profileError = "Daily target should be between 0.25 and 16 hours"
                            } else {
                                onUpdateProfile(name, branch, targetExam, (hrs * 60f).toInt())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save profile", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            }

            // Theme Settings
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { onUpdateThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Notification Preferences
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Preferences", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pomodoro Completion Alerts", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Notify when focus or break sessions end",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.pomodoroAlertsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        onUpdateNotifications(prefs.remindersEnabled, true, prefs.reminderHour, prefs.reminderMinute)
                                        return@Switch
                                    }
                                }
                                onUpdateNotifications(prefs.remindersEnabled, enabled, prefs.reminderHour, prefs.reminderMinute)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Study Reminder Preference", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Remember study goal preference (${String.format("%02d:%02d", prefs.reminderHour, prefs.reminderMinute)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.remindersEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                onUpdateNotifications(enabled, prefs.pomodoroAlertsEnabled, prefs.reminderHour, prefs.reminderMinute)
                            }
                        )
                    }
                }
            }

            // Data Backup & Restore
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Backup & Restore (JSON)", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Export all syllabus topics, study logs, tasks, MCQs, mock tests, and settings to a portable JSON file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "ecet_tracker_backup_${DateUtils.todayEpochDay()}.json"
                                exportFileLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Export JSON", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export File")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = onExportBackupJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ECET Tracker Backup", json))
                                    onEmitMessage("Backup JSON copied to clipboard.")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy JSON", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = "Import JSON file", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import File")
                        }
                        OutlinedButton(
                            onClick = { showPasteRestoreDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = "Paste JSON", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste JSON")
                        }
                    }
                }
            }

            // Reset Option
            item {
                EcetCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Clear all topics, study logs, tasks, MCQ sessions, mock tests, and reset preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_all_data_button")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Reset all data", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Data")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showExamDatePicker) {
        EcetDatePickerDialog(
            initialEpochDay = prefs.examDateEpochDay,
            onDismiss = { showExamDatePicker = false },
            onDateSelected = { onUpdateExamDate(it) },
            onClearDate = { onUpdateExamDate(null) }
        )
    }

    if (showPasteRestoreDialog) {
        PasteBackupDialog(
            onDismiss = { showPasteRestoreDialog = false },
            onRestore = { rawJson ->
                showPasteRestoreDialog = false
                pendingRestoreJson = rawJson
            }
        )
    }

    pendingRestoreJson?.let { rawJson ->
        ConfirmActionDialog(
            title = "Restore Backup?",
            message = "Restoring this JSON backup will replace your current topics, sessions, tasks, MCQs, mock tests, and settings.",
            confirmLabel = "Restore",
            isDestructive = false,
            onConfirm = { onRestoreBackupJson(rawJson) },
            onDismiss = { pendingRestoreJson = null }
        )
    }

    if (showResetConfirm) {
        ConfirmActionDialog(
            title = "Reset All Data?",
            message = "This permanently deletes all saved study records on this device. Export a JSON backup first if you want to keep a copy.",
            confirmLabel = "Reset Everything",
            isDestructive = true,
            onConfirm = onResetAllData,
            onDismiss = { showResetConfirm = false }
        )
    }
}

@Composable
private fun PasteBackupDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var emptyError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Backup JSON") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste a previously exported ECET Tracker 2027 JSON backup below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it; emptyError = false },
                    label = { Text("Backup JSON") },
                    isError = emptyError,
                    supportingText = if (emptyError) {
                        { Text("Please paste backup JSON") }
                    } else null,
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.trim().isEmpty()) {
                        emptyError = true
                    } else {
                        onRestore(jsonText)
                    }
                }
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
