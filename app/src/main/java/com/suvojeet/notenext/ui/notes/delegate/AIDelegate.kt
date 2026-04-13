
package com.suvojeet.notenext.ui.notes.delegate

import androidx.compose.ui.text.TextFieldValue
import com.suvojeet.notenext.data.repository.GroqRepository
import com.suvojeet.notenext.data.repository.GroqResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.util.SimpleDiffUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class AIDelegate @Inject constructor(
    private val groqRepository: GroqRepository
) {
    private val _editState = MutableStateFlow(NotesEditState())
    // Note: In a real scenario, this should probably share state with NoteEditorDelegate 
    // or be observed by the ViewModel. For simplicity, we'll let the ViewModel coordinate.
    
    fun summarize(text: String, scope: CoroutineScope, events: MutableSharedFlow<NotesUiEvent>, onUpdate: (NotesEditState) -> Unit) {
        scope.launch {
            onUpdate(NotesEditState(isSummarizing = true, summaryResult = null))
            groqRepository.summarize(text).collect { result ->
                result.onSuccess { summary ->
                    onUpdate(NotesEditState(isSummarizing = false, summaryResult = summary, showSummaryDialog = true))
                }.onFailure { failure ->
                    onUpdate(NotesEditState(isSummarizing = false))
                    events.emit(NotesUiEvent.ShowToast("Summarization failed"))
                }
            }
        }
    }

    fun fixGrammar(content: TextFieldValue, scope: CoroutineScope, events: MutableSharedFlow<NotesUiEvent>, onUpdate: (NotesEditState) -> Unit) {
        val selection = content.selection
        val fullText = content.text
        val targetText = if (selection.start != selection.end) fullText.substring(selection.start, selection.end) else fullText

        if (targetText.isBlank()) {
            scope.launch { events.emit(NotesUiEvent.ShowToast("No content to fix")) }
            return
        }

        scope.launch {
            onUpdate(NotesEditState(isFixingGrammar = true, originalContentBackup = content))
            groqRepository.fixGrammar(targetText).collect { result ->
                result.onSuccess { fixedFragment ->
                    val finalCleanText = if (selection.start != selection.end) fullText.replaceRange(selection.start, selection.end, fixedFragment) else fixedFragment
                    val diffs = SimpleDiffUtils.computeDiff(targetText, fixedFragment)
                    val diffAnnotated = SimpleDiffUtils.generateDiffString(diffs)
                    
                    val inlinePreviewBuilder = androidx.compose.ui.text.AnnotatedString.Builder()
                    if (selection.start != selection.end) {
                        inlinePreviewBuilder.append(fullText.substring(0, selection.start))
                        inlinePreviewBuilder.append(diffAnnotated)
                        inlinePreviewBuilder.append(fullText.substring(selection.end))
                    } else {
                        inlinePreviewBuilder.append(diffAnnotated)
                    }

                    onUpdate(NotesEditState(
                        isFixingGrammar = false,
                        fixedContentPreview = finalCleanText,
                        editingContent = TextFieldValue(inlinePreviewBuilder.toAnnotatedString(), selection)
                    ))
                }.onFailure { failure ->
                    onUpdate(NotesEditState(isFixingGrammar = false))
                    events.emit(NotesUiEvent.ShowToast("Grammar fix failed"))
                }
            }
        }
    }
}
