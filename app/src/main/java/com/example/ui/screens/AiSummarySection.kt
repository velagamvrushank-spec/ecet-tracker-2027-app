package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AiSummaryEntity
import com.example.data.model.AiChatMessage
import com.example.data.model.AiSummaryUiState
import com.example.data.model.AttachedStudyFile
import com.example.data.model.Subject
import com.example.ui.components.EcetCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubjectBadge
import com.example.ui.viewmodel.EcetUiState
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun AiSummarySection(
    uiState: EcetUiState,
    selectedSubject: Subject,
    topicInput: String,
    attachedFile: AttachedStudyFile?,
    summaryUiState: AiSummaryUiState,
    qaMessages: List<AiChatMessage>,
    qaError: String?,
    isQaLoading: Boolean,
    onSelectSubject: (Subject) -> Unit,
    onTopicInputChange: (String) -> Unit,
    onAttachFileUri: (Uri) -> Unit,
    onClearAttachedFile: () -> Unit,
    onGenerateSummary: () -> Unit,
    onAskQuestion: (String) -> Unit,
    onSaveSummary: () -> Unit,
    onLoadSavedSummary: (AiSummaryEntity) -> Unit,
    onDeleteSavedSummary: (Long) -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var questionInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachFileUri(uri)
        }
    }

    val isGenerating = summaryUiState is AiSummaryUiState.Loading || summaryUiState is AiSummaryUiState.Streaming

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_summary_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Input & File Picker Card
        item {
            EcetCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Gemini Electronics Summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Electronics AI Summary",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Summarize any topic or file (PDF, image, text) in simple ECE language via Gemini AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Subject Focus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Subject.entries.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { onSelectSubject(subj) },
                            label = { Text(subj.shortName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = topicInput,
                    onValueChange = onTopicInputChange,
                    label = { Text("Topic or Question Focus") },
                    placeholder = { Text("e.g., BJT CE Amplifier, K-Maps, Op-Amp Integrator, Laplace RLC") },
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (topicInput.isNotEmpty()) {
                            IconButton(onClick = { onTopicInputChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear topic text")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_topic_input")
                )

                // Quick fill from saved syllabus topics
                if (uiState.topics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick pick from syllabus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.topics.take(10).forEach { topic ->
                            FilterChip(
                                selected = topicInput.equals(topic.title, ignoreCase = true),
                                onClick = {
                                    onSelectSubject(Subject.fromName(topic.subject))
                                    onTopicInputChange(topic.title)
                                },
                                label = {
                                    Text(
                                        text = topic.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Attach File Row
                if (attachedFile != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = "Attached file",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachedFile.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatFileSizeKb(attachedFile.sizeBytes)} · ${attachedFile.mimeType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onClearAttachedFile) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove attached file")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf("application/pdf", "image/*", "text/*", "application/json")
                            )
                        },
                        modifier = Modifier.testTag("attach_summary_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = "Attach study file",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (attachedFile == null) "Attach File" else "Change File")
                    }

                    Button(
                        onClick = onGenerateSummary,
                        enabled = !isGenerating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_summary_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Summarizing...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Summarize",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Summarize", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Summary Output / Streaming / Error Card
        if (summaryUiState !is AiSummaryUiState.Idle) {
            item {
                EcetCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    when (summaryUiState) {
                        is AiSummaryUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Simplifying into Electronics concepts...",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Using Gemini 3.8 Flash via Firebase AI Logic",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is AiSummaryUiState.Streaming, is AiSummaryUiState.Success -> {
                            val text = when (summaryUiState) {
                                is AiSummaryUiState.Streaming -> summaryUiState.text
                                is AiSummaryUiState.Success -> summaryUiState.text
                                else -> ""
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubjectBadge(selectedSubject)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "ECET Summary", text)
                                            onEmitMessage("Summary copied to clipboard.")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy summary",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (summaryUiState is AiSummaryUiState.Success) {
                                        OutlinedButton(
                                            onClick = onSaveSummary,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("save_summary_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.BookmarkAdd,
                                                contentDescription = "Save summary offline",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        is AiSummaryUiState.Error -> {
                            Text(
                                text = "Could not generate summary",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summaryUiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onGenerateSummary) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry summary",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Again")
                            }
                        }

                        AiSummaryUiState.Idle -> Unit
                    }
                }
            }
        }

        // Ask Questions (Gemini AI + Other AI Websites)
        item {
            EcetCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Text(
                    text = "Ask Electronics Questions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ask follow-up doubts using Gemini AI in-app, or launch external AI websites with your prompt pre-copied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (qaMessages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        qaMessages.forEach { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isUser) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (msg.isUser) "You" else "Gemini ECE Tutor",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (msg.isUser) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SelectionContainer {
                                        Text(
                                            text = msg.text.ifBlank { "Thinking..." },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!qaError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = qaError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("Ask any doubt (e.g., Why is CE gain inverting?)") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_question_input")
                    )
                    Button(
                        onClick = {
                            val q = questionInput.trim()
                            if (q.isNotEmpty()) {
                                questionInput = ""
                                onAskQuestion(q)
                            }
                        },
                        enabled = !isQaLoading && questionInput.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("ask_question_button")
                    ) {
                        if (isQaLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send question to Gemini",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Or open in external AI websites (copies electronics prompt)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExternalAiLauncherButton(
                        label = "Gemini Web",
                        url = "https://gemini.google.com/app",
                        onClick = {
                            launchExternalAi(
                                context = context,
                                label = "Gemini Web",
                                url = "https://gemini.google.com/app",
                                subject = selectedSubject,
                                topic = topicInput,
                                question = questionInput,
                                attachedFileName = attachedFile?.displayName,
                                onEmitMessage = onEmitMessage
                            )
                        }
                    )
                    ExternalAiLauncherButton(
                        label = "ChatGPT",
                        url = "https://chatgpt.com/",
                        onClick = {
                            launchExternalAi(
                                context = context,
                                label = "ChatGPT",
                                url = "https://chatgpt.com/",
                                subject = selectedSubject,
                                topic = topicInput,
                                question = questionInput,
                                attachedFileName = attachedFile?.displayName,
                                onEmitMessage = onEmitMessage
                            )
                        }
                    )
                    ExternalAiLauncherButton(
                        label = "Perplexity",
                        url = "https://www.perplexity.ai/",
                        onClick = {
                            launchExternalAi(
                                context = context,
                                label = "Perplexity",
                                url = "https://www.perplexity.ai/",
                                subject = selectedSubject,
                                topic = topicInput,
                                question = questionInput,
                                attachedFileName = attachedFile?.displayName,
                                onEmitMessage = onEmitMessage
                            )
                        }
                    )
                }
            }
        }

        // Saved Offline Summaries Library
        item {
            SectionHeader(
                title = "Saved Electronics Summaries (${uiState.aiSummaries.size})",
                subtitle = "Available offline anytime before exams"
            )
        }

        if (uiState.aiSummaries.isEmpty()) {
            item {
                EcetCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No summaries saved yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate an electronics summary above and tap Save to store it offline on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.aiSummaries, key = { it.id }) { item ->
                val subj = Subject.fromName(item.subject)
                EcetCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onLoadSavedSummary(item) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            SubjectBadge(subj)
                            Text(
                                text = item.sourceType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSavedSummary(item.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete saved summary",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = item.sourceTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.summaryMarkdown.replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtils.formatEpochDay(DateUtils.epochMillisToLocalEpochDay(item.createdAtMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onLoadSavedSummary(item) }) {
                            Text("Open & Ask Q&A")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalAiLauncherButton(
    label: String,
    url: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Open $label ($url)",
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun formatFileSizeKb(bytes: Long): String {
    return if (bytes >= 1024 * 1024) {
        String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    } else {
        "${(bytes / 1024L).coerceAtLeast(1L)} KB"
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun launchExternalAi(
    context: Context,
    label: String,
    url: String,
    subject: Subject,
    topic: String,
    question: String,
    attachedFileName: String?,
    onEmitMessage: (String) -> Unit
) {
    val prompt = buildString {
        appendLine("Explain in a simple way related to Electronics & Communication Engineering (TG ECET 2027 · ${subject.displayName}):")
        if (topic.isNotBlank()) appendLine("Topic: ${topic.trim()}")
        if (!attachedFileName.isNullOrBlank()) appendLine("File reference: $attachedFileName")
        if (question.isNotBlank()) appendLine("Question: ${question.trim()}")
    }.trim()
    copyToClipboard(context, "ECET Electronics AI Prompt", prompt)
    onEmitMessage("Prompt copied — opening $label.")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure {
        onEmitMessage("Could not open browser for $label. Prompt is copied to clipboard.")
    }
}
