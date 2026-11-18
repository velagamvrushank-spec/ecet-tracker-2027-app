package com.example.data.ai

import android.util.Log
import com.example.data.model.AiChatMessage
import com.example.data.model.AttachedStudyFile
import com.example.data.model.Subject
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "ElectronicsAiService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val MAX_INLINE_BYTES = 18 * 1024 * 1024L
private const val REQUEST_TIMEOUT_MS = 60_000L

class ElectronicsAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    """
                    You are an Electronics & Communication Engineering (ECE) study mentor helping a Diploma student preparing for TG ECET 2027.
                    Explain any given file or topic in simple, beginner-friendly English connected directly to Electronics & Communication Engineering concepts (circuits, signals, semiconductor devices, digital logic, communication systems, microprocessors, or ECE applications of math/physics/chemistry).
                    Format summaries cleanly for a mobile screen using clear headings and short bullets:
                    1. Simple Meaning (plain intuitive explanation)
                    2. Core Electronics Concepts & Key Formulas
                    3. Practical Circuit / ECE Example
                    4. TG ECET 2027 Exam Pointers & Common Traps
                    5. Quick Self-Check (2 short questions with concise answers)
                    Stay concise, accurate, and grounded strictly in engineering facts. If a file is unclear or unreadable, state that honestly instead of guessing.
                    """.trimIndent()
                )
            }
        )
    }

    fun summarizeStream(
        subject: Subject,
        topicPrompt: String,
        attachedFile: AttachedStudyFile?
    ): Flow<String> = flow {
        if (attachedFile != null && attachedFile.sizeBytes > MAX_INLINE_BYTES) {
            throw IllegalArgumentException("Attached file exceeds 18 MB limit. Please choose a smaller PDF, image, or text file.")
        }

        val cleanPrompt = topicPrompt.trim()
        require(cleanPrompt.isNotEmpty() || attachedFile != null) {
            "Enter a topic or attach a file to summarize."
        }

        val inputContent = content {
            val subjectHeader = "Subject Focus: ${subject.displayName} (relating to Electronics & Communication / ECE)"
            if (attachedFile != null) {
                text(
                    buildString {
                        appendLine(subjectHeader)
                        appendLine("Uploaded File: ${attachedFile.displayName} (${attachedFile.mimeType})")
                        if (cleanPrompt.isNotEmpty()) {
                            appendLine("Student Request / Topic Context: $cleanPrompt")
                        }
                        appendLine("Summarize this material in simple language related to electronics.")
                    }
                )
                if (!attachedFile.textContent.isNullOrBlank()) {
                    text("--- FILE TEXT START ---\n${attachedFile.textContent.take(28_000)}\n--- FILE TEXT END ---")
                } else {
                    inlineData(attachedFile.bytes, attachedFile.mimeType)
                }
            } else {
                text(
                    """
                    $subjectHeader
                    Topic to Summarize: $cleanPrompt
                    Explain this topic in a simple way related to electronics for TG ECET 2027 revision.
                    """.trimIndent()
                )
            }
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                model.generateContentStream(inputContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty summary. Please try rephrasing or attaching a clearer file.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini summary timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini summary generation failed", e)
            throw e
        }
    }

    fun askFollowUpStream(
        subject: Subject,
        contextTitle: String,
        contextSummary: String,
        attachedFile: AttachedStudyFile?,
        priorMessages: List<AiChatMessage>,
        question: String
    ): Flow<String> = flow {
        val cleanQuestion = question.trim()
        require(cleanQuestion.isNotEmpty()) { "Enter a question to ask." }

        val historyContents = buildList {
            add(
                content("user") {
                    text(
                        buildString {
                            appendLine("We are studying ${subject.displayName} (Electronics / TG ECET 2027).")
                            if (contextTitle.isNotBlank()) {
                                appendLine("Reference topic/file: $contextTitle")
                            }
                            if (attachedFile != null && !attachedFile.textContent.isNullOrBlank()) {
                                appendLine("Attached text excerpt:\n${attachedFile.textContent.take(12_000)}")
                            } else if (attachedFile != null && attachedFile.sizeBytes <= MAX_INLINE_BYTES) {
                                inlineData(attachedFile.bytes, attachedFile.mimeType)
                            }
                            if (contextSummary.isNotBlank()) {
                                appendLine("Existing summary:\n${contextSummary.take(8_000)}")
                            }
                        }
                    )
                }
            )
            add(
                content("model") {
                    text("Understood. I have the electronics study context ready. Ask any question about formulas, circuit behavior, or ECET MCQs.")
                }
            )
            priorMessages.takeLast(8).forEach { msg ->
                if (msg.text.isNotBlank()) {
                    add(
                        content(if (msg.isUser) "user" else "model") {
                            text(msg.text)
                        }
                    )
                }
            }
        }

        val chat = model.startChat(history = historyContents)
        val accumulated = StringBuilder()
        try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                chat.sendMessageStream(cleanQuestion).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty answer. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini Q&A timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Q&A failed", e)
            throw e
        }
    }
}
