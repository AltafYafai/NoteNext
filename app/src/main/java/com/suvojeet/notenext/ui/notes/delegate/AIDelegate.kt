
package com.suvojeet.notenext.ui.notes.delegate

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.repository.GroqRepository
import com.suvojeet.notenext.data.repository.GroqResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.util.SimpleDiffUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AIDelegate @Inject constructor(
    private val groqRepository: GroqRepository
) {
    fun summarize(
        content: String,
        scope: CoroutineScope,
        events: MutableSharedFlow<NotesUiEvent>,
        onUpdate: ((NotesEditState) -> NotesEditState) -> Unit
    ) {
        scope.launch {
            onUpdate { it.copy(isSummarizing = true, showSummaryDialog = true) }
            groqRepository.summarizeNote(content).collect { result ->
                result.onSuccess { summary ->
                    onUpdate { it.copy(isSummarizing = false, summaryResult = summary) }
                }.onFailure { failure ->
                    val errorMessage = when (failure) {
                        is GroqResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                        is GroqResult.InvalidKey -> "Invalid API key. Check your settings."
                        is GroqResult.NetworkError -> "Network error: ${failure.message}"
                        is GroqResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
                        else -> "Failed to summarize note."
                    }
                    onUpdate { it.copy(isSummarizing = false, summaryResult = "Error: $errorMessage") }
                }
            }
        }
    }

    fun fixGrammar(content: TextFieldValue, scope: CoroutineScope, events: MutableSharedFlow<NotesUiEvent>, onUpdate: ((NotesEditState) -> NotesEditState) -> Unit) {
        val selection = content.selection
        val fullText = content.text
        val targetText = if (selection.start != selection.end) fullText.substring(selection.start, selection.end) else fullText

        if (targetText.isBlank()) {
            scope.launch { events.emit(NotesUiEvent.ShowToast("No content to fix")) }
            return
        }

        scope.launch {
            onUpdate { it.copy(isFixingGrammar = true, originalContentBackup = content) }
            groqRepository.fixGrammar(targetText).collect { result ->
                result.onSuccess { fixedFragment ->
                    val finalCleanText = if (selection.start != selection.end) fullText.replaceRange(selection.start, selection.end, fixedFragment) else fixedFragment
                    val diffs = SimpleDiffUtils.computeDiff(targetText, fixedFragment)
                    val diffAnnotated = SimpleDiffUtils.generateDiffString(diffs)
                    
                    val inlinePreviewBuilder = AnnotatedString.Builder()
                    if (selection.start != selection.end) {
                        inlinePreviewBuilder.append(fullText.substring(0, selection.start))
                        inlinePreviewBuilder.append(diffAnnotated)
                        inlinePreviewBuilder.append(fullText.substring(selection.end))
                    } else {
                        inlinePreviewBuilder.append(diffAnnotated)
                    }

                    onUpdate { state ->
                        state.copy(
                            isFixingGrammar = false,
                            fixedContentPreview = finalCleanText,
                            editingContent = TextFieldValue(inlinePreviewBuilder.toAnnotatedString(), selection)
                        )
                    }
                }.onFailure { failure ->
                    onUpdate { it.copy(isFixingGrammar = false) }
                    events.emit(NotesUiEvent.ShowToast("Grammar fix failed"))
                }
            }
        }
    }
}
