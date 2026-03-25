package com.suvojeet.notenext.ui.notes

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.AlarmScheduler
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.repository.GroqRepository
import com.suvojeet.notenext.data.repository.GroqResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.domain.use_case.NoteUseCases
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.ui.util.UndoRedoManager
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesEditViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val noteUseCases: NoteUseCases,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val richTextController: RichTextController,
    private val groqRepository: GroqRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_EDITING_TITLE = "editing_title"
        private const val KEY_EDITING_CONTENT = "editing_content"
        private const val KEY_EXPANDED_NOTE_ID = "expanded_note_id"
    }

    private val _editState = MutableStateFlow(
        NotesEditState(
            editingTitle = savedStateHandle.get<String>(KEY_EDITING_TITLE) ?: "",
            editingContent = TextFieldValue(richTextController.parseMarkdownToAnnotatedString(savedStateHandle.get<String>(KEY_EDITING_CONTENT) ?: "")),
            expandedNoteId = savedStateHandle.get<Int>(KEY_EXPANDED_NOTE_ID)
        )
    )
    val editState = _editState.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private val undoRedoManager = UndoRedoManager<Pair<String, TextFieldValue>>("" to TextFieldValue())
    private var autoSaveJob: Job? = null

    fun onEvent(event: NotesEditEvent) {
        when (event) {
            is NotesEditEvent.ExpandNote -> {
                expandNote(event)
            }
            is NotesEditEvent.CollapseNote -> {
                saveNote()
                clearSavedState()
                _editState.value = _editState.value.copy(expandedNoteId = null)
            }
            is NotesEditEvent.OnTitleChange -> {
                _editState.value = _editState.value.copy(editingTitle = event.title)
                savedStateHandle[KEY_EDITING_TITLE] = event.title
                scheduleAutoSave()
            }
            is NotesEditEvent.OnContentChange -> {
                handleContentChange(event.content)
            }
            is NotesEditEvent.OnSaveNoteClick -> {
                saveNote()
            }
            is NotesEditEvent.AddChecklistItem -> {
                val (updatedChecklist, newItemId) = ChecklistManager.addChecklistItem(_editState.value.editingChecklist)
                _editState.value = _editState.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = _editState.value.checklistInputValues + (newItemId to TextFieldValue(""))
                )
                scheduleAutoSave()
            }
            // ... more events will be added or delegated
            else -> { /* Handle others */ }
        }
    }

    private fun expandNote(event: NotesEditEvent.ExpandNote) {
        viewModelScope.launch {
            if (event.noteId != -1) {
                noteUseCases.getNote(event.noteId)?.let { noteWithAttachments ->
                    val note = noteWithAttachments.note
                    val content = if (note.noteType == "TEXT") {
                        HtmlConverter.htmlToAnnotatedString(note.content)
                    } else {
                        AnnotatedString("")
                    }
                    val checklist = if (note.noteType == "CHECKLIST") {
                        noteWithAttachments.checklistItems.sortedBy { it.position }
                    } else {
                        emptyList()
                    }

                    _editState.value = _editState.value.copy(
                        expandedNoteId = event.noteId,
                        editingTitle = note.title,
                        editingContent = TextFieldValue(content),
                        editingColor = note.color,
                        editingIsNewNote = false,
                        editingNoteType = note.noteType,
                        editingChecklist = checklist,
                        checklistInputValues = checklist.associate { item ->
                            item.id to TextFieldValue(richTextController.parseMarkdownToAnnotatedString(item.text))
                        }
                    )
                    savedStateHandle[KEY_EXPANDED_NOTE_ID] = event.noteId
                    savedStateHandle[KEY_EDITING_TITLE] = note.title
                    savedStateHandle[KEY_EDITING_CONTENT] = note.content
                }
            } else {
                _editState.value = NotesEditState(expandedNoteId = -1, editingIsNewNote = true)
                clearSavedState()
            }
        }
    }

    private fun handleContentChange(newContent: TextFieldValue) {
        val oldContent = _editState.value.editingContent
        val processedContent = richTextController.processContentChange(
            oldContent, newContent, _editState.value.activeStyles, _editState.value.activeHeadingStyle
        )
        _editState.value = _editState.value.copy(editingContent = processedContent)
        viewModelScope.launch {
            savedStateHandle[KEY_EDITING_CONTENT] = HtmlConverter.annotatedStringToHtml(processedContent.annotatedString)
        }
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000L)
            saveNote()
        }
    }

    private fun saveNote() {
        viewModelScope.launch {
            val state = _editState.value
            if (state.expandedNoteId == null) return@launch

            val contentHtml = if (state.editingNoteType == "TEXT") {
                HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
            } else ""

            if (state.editingIsNewNote) {
                val currentTime = System.currentTimeMillis()
                val newNote = com.suvojeet.notenext.data.Note(
                    title = state.editingTitle,
                    content = contentHtml,
                    color = state.editingColor,
                    noteType = state.editingNoteType,
                    createdAt = currentTime,
                    lastEdited = currentTime
                )
                val id = repository.insertNote(newNote)
                _editState.value = _editState.value.copy(expandedNoteId = id.toInt(), editingIsNewNote = false)
            } else {
                noteUseCases.getNote(state.expandedNoteId)?.let { existingNote ->
                    val updatedNote = existingNote.note.copy(
                        title = state.editingTitle,
                        content = contentHtml,
                        color = state.editingColor,
                        lastEdited = System.currentTimeMillis()
                    )
                    repository.updateNote(updatedNote)
                }
            }
        }
    }

    private fun clearSavedState() {
        savedStateHandle.remove<Int>(KEY_EXPANDED_NOTE_ID)
        savedStateHandle.remove<String>(KEY_EDITING_TITLE)
        savedStateHandle.remove<String>(KEY_EDITING_CONTENT)
    }

    suspend fun getNoteLockStatus(noteId: Int): Boolean {
        return repository.getNoteById(noteId)?.note?.isLocked == true
    }

    suspend fun getNoteIdByTitle(title: String): Int? {
        return repository.getNoteIdByTitle(title)
    }
}
