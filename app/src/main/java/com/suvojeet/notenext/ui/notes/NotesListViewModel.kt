package com.suvojeet.notenext.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.AlarmScheduler
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.domain.use_case.NoteUseCases
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val noteUseCases: NoteUseCases,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _listState = MutableStateFlow(NotesListState())
    val listState = _listState.asStateFlow()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)
    private val _filteredProjectId = MutableStateFlow<Int?>(null)

    private var recentlyDeletedNote: Note? = null

    init {
        val combinedFlow = combine(_searchQuery, _sortType, _filteredProjectId) { query, sortType, projectId ->
            Triple(query, sortType, projectId)
        }

        combinedFlow.flatMapLatest { (query, _, projectId) ->
            repository.getPinnedNotes(query, projectId)
        }.onEach { pinned ->
            _listState.value = _listState.value.copy(pinnedNotes = pinned)
        }.launchIn(viewModelScope)

        combinedFlow.onEach { (query, sortType, projectId) ->
            _listState.value = _listState.value.copy(
                pagedNotes = repository.getOtherNotesPaged(query, sortType, projectId).cachedIn(viewModelScope),
                searchQuery = query,
                sortType = sortType,
                filteredProjectId = projectId
            )
        }.launchIn(viewModelScope)

        repository.getLabels().onEach { labels ->
            _listState.value = _listState.value.copy(labels = labels.map { it.name })
        }.launchIn(viewModelScope)

        repository.getProjects().onEach { projects ->
            _listState.value = _listState.value.copy(projects = projects, isLoading = false)
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NotesListEvent) {
        when (event) {
            is NotesListEvent.DeleteNote -> {
                viewModelScope.launch {
                    noteUseCases.deleteNote(event.note.note)
                    recentlyDeletedNote = event.note.note
                    _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                }
            }
            is NotesListEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        repository.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                    }
                }
            }
            is NotesListEvent.ToggleNoteSelection -> {
                val selectedIds = _listState.value.selectedNoteIds.toMutableList()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _listState.value = _listState.value.copy(selectedNoteIds = selectedIds)
            }
            is NotesListEvent.ClearSelection -> {
                _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
            }
            is NotesListEvent.SelectAllNotes -> {
                val notesToSelect = if (_listState.value.filteredLabel != null) {
                    _listState.value.notes.filter { it.note.label == _listState.value.filteredLabel }
                } else {
                    _listState.value.notes
                }
                val allIds = notesToSelect.map { it.note.id }
                _listState.value = _listState.value.copy(selectedNoteIds = allIds)
            }
            is NotesListEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val allSelectedNotes = getSelectedNotes()
                    if (allSelectedNotes.isEmpty()) return@launch
                    val areNotesBeingPinned = allSelectedNotes.any { !it.note.isPinned }
                    for (note in allSelectedNotes) {
                        repository.updateNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.ToggleLockForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    if (selectedNotes.isEmpty()) return@launch
                    val areNotesBeingLocked = selectedNotes.firstOrNull()?.note?.isLocked == false
                    for (note in selectedNotes) {
                         repository.updateNote(note.note.copy(isLocked = areNotesBeingLocked))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.DeleteSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.ChangeColorForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(color = event.color))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (noteWithAttachments in selectedNotes) {
                        val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)")
                        val newNoteId = repository.insertNote(copiedNote)
                        noteWithAttachments.attachments.forEach { attachment ->
                            repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                        }
                        val newChecklistItems = noteWithAttachments.checklistItems.map { item ->
                            item.copy(id = java.util.UUID.randomUUID().toString(), noteId = newNoteId.toInt())
                        }
                        repository.insertChecklistItems(newChecklistItems)
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.SendSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    if (selectedNotes.isNotEmpty()) {
                        val title = if (selectedNotes.size == 1) selectedNotes.first().note.title else "Multiple Notes"
                        val contentBuilder = StringBuilder()
                        selectedNotes.forEachIndexed { index, it ->
                            contentBuilder.append("Title: ${it.note.title}\n\n${HtmlConverter.htmlToPlainText(it.note.content)}")
                            if (index < selectedNotes.size - 1) {
                                contentBuilder.append("\n\n---\n\n")
                            }
                        }
                        _events.emit(NotesUiEvent.SendNotes(title, contentBuilder.toString()))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.SetReminderForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    val reminderDateTime = LocalDateTime.of(event.date, event.time)
                    val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    for (noteWithAttachments in selectedNotes) {
                        val updatedNote = noteWithAttachments.note.copy(
                            reminderTime = reminderMillis,
                            repeatOption = event.repeatOption.name
                        )
                        repository.updateNote(updatedNote)
                        alarmScheduler.schedule(updatedNote)
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    if (event.label.isNotBlank()) {
                        repository.insertLabel(Label(event.label))
                    }
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(label = event.label))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.FilterByLabel -> {
                _listState.value = _listState.value.copy(filteredLabel = event.label)
            }
            is NotesListEvent.FilterByProject -> {
                _filteredProjectId.value = event.projectId
            }
            is NotesListEvent.ToggleLayout -> {
                val newLayout = if (_listState.value.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
                _listState.value = _listState.value.copy(layoutType = newLayout)
            }
            is NotesListEvent.SortNotes -> {
                _sortType.value = event.sortType
            }
            is NotesListEvent.CreateProject -> {
                viewModelScope.launch {
                    repository.insertProject(com.suvojeet.notenext.data.Project(name = event.name))
                }
            }
            is NotesListEvent.MoveSelectedNotesToProject -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(projectId = event.projectId))
                    }
                    _listState.value = _listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesListEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
        }
    }

    private suspend fun getSelectedNotes(): List<NoteWithAttachments> {
        return _listState.value.selectedNoteIds.mapNotNull { id ->
            noteUseCases.getNote(id)
        }
    }
}
