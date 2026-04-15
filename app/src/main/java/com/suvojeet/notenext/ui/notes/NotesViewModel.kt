package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.core.util.ImageUtils
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.data.AlarmScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import com.suvojeet.notenext.data.RepeatOption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.repository.GroqRepository
import com.suvojeet.notenext.data.repository.GroqResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.domain.use_case.NoteUseCases
import com.suvojeet.notenext.ui.util.UndoRedoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.suvojeet.notenext.widget.NoteWidgetProvider
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.theme.NoteGradients
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val todoRepository: com.suvojeet.notenext.data.TodoRepository,
    private val noteUseCases: NoteUseCases,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val alarmScheduler: AlarmScheduler,
    private val richTextController: RichTextController,
    private val groqRepository: GroqRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val editorDelegate: com.suvojeet.notenext.ui.notes.delegate.NoteEditorDelegate
) : ViewModel() {

    companion object {
        private const val KEY_EDITING_TITLE = "editing_title"
        private const val KEY_EDITING_CONTENT = "editing_content"
        private const val KEY_EXPANDED_NOTE_ID = "expanded_note_id"
    }

    private val _listState = MutableStateFlow(NotesListState())
    val listState = _listState.asStateFlow()

    private val _editState = MutableStateFlow(
        NotesEditState(
            editingTitle = savedStateHandle.get<String>(KEY_EDITING_TITLE) ?: "",
            editingContent = TextFieldValue(richTextController.parseMarkdownToAnnotatedString(savedStateHandle.get<String>(KEY_EDITING_CONTENT) ?: "")),
            expandedNoteId = savedStateHandle.get<Int>(KEY_EXPANDED_NOTE_ID)
        )
    )
    val editState = _editState.asStateFlow()

    // High-frequency editing flows to isolate recomposition
    val editingContent = _editState.map { it.editingContent }.distinctUntilChanged()
    val editingTitle = _editState.map { it.editingTitle }.distinctUntilChanged()

    private val _events = MutableSharedFlow<NotesUiEvent>()
    val events = _events.asSharedFlow()

    private var recentlyDeletedNote: Note? = null
    
    private val undoRedoManager = UndoRedoManager<Pair<String, TextFieldValue>>("" to TextFieldValue())

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)
    private val _filteredProjectId = MutableStateFlow<Int?>(null)

    private var autoSaveJob: Job? = null
    private var selectionActionsJob: Job? = null
    private var linkDetectionJob: Job? = null

    private var lastCreatedNoteId: Int? = null

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        _editState.value = editState.value.copy(saveStatus = SaveStatus.SAVING)
        autoSaveJob = viewModelScope.launch {
            try {
                delay(1000L) // 1 second debounce
                saveNote(shouldCollapse = false)
                _editState.value = editState.value.copy(saveStatus = SaveStatus.SAVED)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                _editState.value = editState.value.copy(saveStatus = SaveStatus.ERROR)
                _events.emit(NotesUiEvent.ShowToast("Auto-save failed: ${e.message}"))
            }
        }
    }

    init {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        val queryFlow = _searchQuery
        val sortFlow = _sortType
        val projectFlow = _filteredProjectId
        val combinedFlow = combine(queryFlow, sortFlow, projectFlow) { query, sortType, projectId -> Triple(query, sortType, projectId) }
        
        combinedFlow.flatMapLatest { (query, sortType, projectId) ->
            repository.getPinnedNoteSummaries(query, projectId)
        }.onEach { pinned ->
            _listState.value = _listState.value.copy(pinnedNotes = pinned)
        }.launchIn(viewModelScope)

        combinedFlow.onEach { (query, sortType, projectId) ->
            _listState.value = _listState.value.copy(
                pagedNotes = repository.getOtherNoteSummariesPaged(query, sortType, projectId).cachedIn(viewModelScope),
                searchQuery = query,
                sortType = sortType,
                filteredProjectId = projectId
            )
        }.launchIn(viewModelScope)

        repository.getLabels().onEach { labels ->
            val labelNames = labels.map { it.name }
            _listState.value = _listState.value.copy(labels = labelNames)
            _editState.value = _editState.value.copy(labels = labelNames)
        }.launchIn(viewModelScope)

        repository.getProjects().onEach { projects ->
            _listState.value = _listState.value.copy(projects = projects, isLoading = false)
        }.launchIn(viewModelScope)

        selectionActionsJob = viewModelScope.launch {
            try {
                 NoteSelectionManager.actions.collect { style ->
                     onEvent(NotesEvent.ApplyStyleToContent(style))
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        selectionActionsJob?.cancel()
        linkDetectionJob?.cancel()
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.GenerateChecklist -> {
                viewModelScope.launch {
                    _editState.value = editState.value.copy(isGeneratingChecklist = true, generatedChecklistPreview = emptyList())
                    groqRepository.generateChecklist(event.topic).collect { result ->
                        result.onSuccess { items ->
                            // Store preview instead of inserting directly
                            _editState.value = editState.value.copy(
                                isGeneratingChecklist = false,
                                generatedChecklistPreview = items
                            )
                        }.onFailure { failure ->
                            _editState.value = editState.value.copy(isGeneratingChecklist = false, generatedChecklistPreview = emptyList())
                            val errorMessage = when (failure) {
                                is GroqResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                                is GroqResult.InvalidKey -> "Invalid API key. Check your settings."
                                is GroqResult.NetworkError -> "Network error: ${failure.message}"
                                is GroqResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
                                else -> "Failed to generate checklist."
                            }
                            _events.emit(NotesUiEvent.ShowToast(errorMessage))
                        }
                    }
                }
            }
            is NotesEvent.InsertGeneratedChecklist -> {
                val items = event.items
                if (items.isNotEmpty()) {
                    val checklistItems = items.mapIndexed { index, text -> 
                        ChecklistItem(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text, 
                            isChecked = false, 
                            position = editState.value.editingChecklist.size + index,
                            noteId = editState.value.expandedNoteId ?: 0
                        ) 
                    }
                    
                    val newInputValues = checklistItems.associate { item ->
                        item.id to TextFieldValue(item.text)
                    }

                    _editState.value = editState.value.copy(
                        editingNoteType = NoteType.CHECKLIST,
                        editingChecklist = editState.value.editingChecklist + checklistItems,
                        checklistInputValues = editState.value.checklistInputValues + newInputValues,
                        generatedChecklistPreview = emptyList()
                    )
                }
            }
            is NotesEvent.ClearGeneratedChecklist -> {
                _editState.value = editState.value.copy(generatedChecklistPreview = emptyList(), isGeneratingChecklist = false)
            }
            is NotesEvent.FixGrammar -> {
                val currentTextFieldValue = editState.value.editingContent
                val selection = currentTextFieldValue.selection
                val fullText = currentTextFieldValue.text

                // Determine target text (selection or full)
                val targetText = if (selection.start != selection.end) {
                    fullText.substring(selection.start, selection.end)
                } else {
                    fullText
                }

                if (targetText.isBlank()) {
                    viewModelScope.launch { _events.emit(NotesUiEvent.ShowToast("No content to fix")) }
                    return
                }

                viewModelScope.launch {
                    _editState.value = editState.value.copy(
                        isFixingGrammar = true, 
                        fixedContentPreview = null,
                        originalContentBackup = currentTextFieldValue // Backup for Undo
                    )
                    
                    groqRepository.fixGrammar(targetText).collect { result ->
                        result.onSuccess { fixedFragment ->
                            // Calculate global clean text (what it will be if accepted)
                            val finalCleanText = if (selection.start != selection.end) {
                                fullText.replaceRange(selection.start, selection.end, fixedFragment)
                            } else {
                                fixedFragment
                            }

                            // Calculate Diff just for the changed part
                            val diffs = com.suvojeet.notenext.util.SimpleDiffUtils.computeDiff(targetText, fixedFragment)
                            val diffAnnotated = com.suvojeet.notenext.util.SimpleDiffUtils.generateDiffString(diffs)

                            // Construct Inline Preview (Original Before + Diff + Original After)
                            val inlinePreviewBuilder = androidx.compose.ui.text.AnnotatedString.Builder()
                            if (selection.start != selection.end) {
                                inlinePreviewBuilder.append(fullText.substring(0, selection.start))
                                inlinePreviewBuilder.append(diffAnnotated)
                                inlinePreviewBuilder.append(fullText.substring(selection.end))
                            } else {
                                inlinePreviewBuilder.append(diffAnnotated)
                            }
                            val inlinePreview = inlinePreviewBuilder.toAnnotatedString()

                            _editState.value = editState.value.copy(
                                isFixingGrammar = false,
                                fixedContentPreview = finalCleanText, // Clean text for Apply
                                editingContent = TextFieldValue(inlinePreview, selection) // Show Diff Inline
                            )
                        }.onFailure { failure ->
                            _editState.value = editState.value.copy(isFixingGrammar = false, fixedContentPreview = null, originalContentBackup = null)
                            val errorMessage = when (failure) {
                                is GroqResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                                is GroqResult.InvalidKey -> "Invalid API key. Check your settings."
                                is GroqResult.NetworkError -> "Network error: ${failure.message}"
                                is GroqResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
                                else -> "Failed to fix grammar."
                            }
                            _events.emit(NotesUiEvent.ShowToast(errorMessage))
                        }
                    }
                }
            }
            is NotesEvent.AutoSaveNote -> {
                viewModelScope.launch {
                    saveNote(shouldCollapse = false)
                    _editState.value = editState.value.copy(saveStatus = SaveStatus.SAVED)
                }
            }
            is NotesEvent.OnMentionSearchQueryChange -> {
                val query = event.query
                val filteredNotes = listState.value.notes.filter { 
                    it.note.title.contains(query, ignoreCase = true) && 
                    it.note.id != editState.value.expandedNoteId 
                }
                _editState.value = editState.value.copy(
                    isMentionPopupVisible = true,
                    mentionSearchQuery = query,
                    mentionableNotes = filteredNotes
                )
            }
            is NotesEvent.InsertMention -> {
                val currentText = editState.value.editingContent.text
                val selection = editState.value.editingContent.selection
                val mentionText = "@${editState.value.mentionSearchQuery}"
                
                // Find the mention text before the cursor and replace it with Markdown link
                val textBeforeCursor = currentText.substring(0, selection.start)
                val textAfterCursor = currentText.substring(selection.end)
                
                val lastMentionIndex = textBeforeCursor.lastIndexOf(mentionText)
                if (lastMentionIndex != -1) {
                    val wikiLink = "[[${event.noteTitle}]]"
                    val newTextBeforeCursor = textBeforeCursor.substring(0, lastMentionIndex) + wikiLink
                    
                    val newAnnotatedString = richTextController.parseMarkdownToAnnotatedString(newTextBeforeCursor + textAfterCursor)
                    val newCursorPosition = newTextBeforeCursor.length
                    
                    _editState.value = editState.value.copy(
                        editingContent = TextFieldValue(newAnnotatedString, androidx.compose.ui.text.TextRange(newCursorPosition)),
                        isMentionPopupVisible = false,
                        mentionSearchQuery = "",
                        mentionableNotes = emptyList()
                    )
                } else {
                    _editState.value = editState.value.copy(
                        isMentionPopupVisible = false,
                        mentionSearchQuery = "",
                        mentionableNotes = emptyList()
                    )
                }
            }
            is NotesEvent.CloseMentionPopup -> {
                _editState.value = editState.value.copy(
                    isMentionPopupVisible = false,
                    mentionSearchQuery = "",
                    mentionableNotes = emptyList()
                )
            }
            is NotesEvent.ImportImage -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val uri = event.uri
                    val compressedUri = ImageUtils.compressImage(context, uri)
                    if (compressedUri != null) {
                        val mimeType = context.contentResolver.getType(compressedUri)
                        onEvent(NotesEvent.AddAttachment(compressedUri.toString(), mimeType ?: "image/jpeg"))
                    } else {
                        // Fallback: Copy to internal storage manually if compression fails
                        try {
                            val fileName = "IMG_${java.util.UUID.randomUUID()}.jpg"
                            val destFile = java.io.File(context.filesDir, "images/$fileName")
                            destFile.parentFile?.mkdirs()
                            
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            val localUri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", destFile
                            )
                            val mimeType = context.contentResolver.getType(localUri)
                            onEvent(NotesEvent.AddAttachment(localUri.toString(), mimeType ?: "image/jpeg"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _events.emit(NotesUiEvent.ShowToast("Failed to import image: ${e.message}"))
                        }
                    }
                }
            }
            is NotesEvent.ApplyGrammarFix -> {
                val fixedContent = editState.value.fixedContentPreview
                if (fixedContent != null) {
                    _editState.value = editState.value.copy(
                        editingContent = TextFieldValue(fixedContent), // Apply clean text
                        fixedContentPreview = null,
                        originalContentBackup = null
                    )
                    viewModelScope.launch { _events.emit(NotesUiEvent.ShowToast("Fixed!")) }
                }
            }
            is NotesEvent.ClearGrammarFix -> {
                // Revert to backup
                editState.value.originalContentBackup?.let { backup ->
                    _editState.value = editState.value.copy(
                        editingContent = backup,
                        fixedContentPreview = null,
                        originalContentBackup = null
                    )
                } ?: run {
                     _editState.value = editState.value.copy(fixedContentPreview = null, isFixingGrammar = false)
                }
            }
            is NotesEvent.LoadExternalFile -> {
                viewModelScope.launch {
                    try {
                        val uri = event.uri
                        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                        val fileName = com.suvojeet.notenext.util.ContextUtils.getFileName(context, uri) ?: "External Note"
                        
                        undoRedoManager.reset(fileName to TextFieldValue(content))
                        
                        _editState.value = editState.value.copy(
                            expandedNoteId = -1, // Treat as new note but with external URI
                            externalUri = uri,
                            editingTitle = fileName,
                            editingContent = TextFieldValue(richTextController.parseMarkdownToAnnotatedString(content)),
                            editingNoteType = NoteType.TEXT,
                            editingIsNewNote = true,
                            canUndo = false,
                            canRedo = false
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _events.emit(NotesUiEvent.ShowToast("Failed to load file: ${e.message}"))
                    }
                }
            }
            is NotesEvent.SaveExternalAsNote -> {
                viewModelScope.launch {
                    val currentTime = System.currentTimeMillis()
                    val note = Note(
                        title = editState.value.editingTitle,
                        content = if (editState.value.editingNoteType == NoteType.TEXT) {
                            HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString)
                        } else "",
                        createdAt = currentTime,
                        lastEdited = currentTime,
                        color = editState.value.editingColor,
                        noteType = editState.value.editingNoteType
                    )
                    val newId = repository.insertNote(note)
                    require(newId <= Int.MAX_VALUE) { "Note ID overflow" }
                    
                    _editState.value = editState.value.copy(
                        expandedNoteId = newId.toInt(),
                        externalUri = null,
                        editingIsNewNote = false
                    )
                    _events.emit(NotesUiEvent.ShowToast("Saved as internal note"))
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleNoteSearch -> {
                val isSearching = !editState.value.isSearchingInNote
                _editState.value = editState.value.copy(
                    isSearchingInNote = isSearching,
                    noteSearchQuery = if (!isSearching) "" else editState.value.noteSearchQuery,
                    searchResultIndices = if (!isSearching) emptyList() else editState.value.searchResultIndices,
                    currentSearchResultIndex = if (!isSearching) -1 else editState.value.currentSearchResultIndex
                )
            }
            is NotesEvent.OnNoteSearchQueryChange -> {
                val query = event.query
                val content = editState.value.editingContent.text
                val indices = if (query.isNotBlank()) {
                    val foundIndices = mutableListOf<Int>()
                    var index = content.indexOf(query, ignoreCase = true)
                    while (index >= 0) {
                        foundIndices.add(index)
                        index = content.indexOf(query, index + 1, ignoreCase = true)
                    }
                    foundIndices
                } else emptyList()

                _editState.value = editState.value.copy(
                    noteSearchQuery = query,
                    searchResultIndices = indices,
                    currentSearchResultIndex = if (indices.isNotEmpty()) 0 else -1
                )
                
                if (indices.isNotEmpty()) {
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[0]))
                    }
                }
            }
            is NotesEvent.NextSearchResult -> {
                val indices = editState.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val nextIndex = (editState.value.currentSearchResultIndex + 1) % indices.size
                    _editState.value = editState.value.copy(currentSearchResultIndex = nextIndex)
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[nextIndex]))
                    }
                }
            }
            is NotesEvent.PreviousSearchResult -> {
                val indices = editState.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val prevIndex = if (editState.value.currentSearchResultIndex <= 0) indices.size - 1 else editState.value.currentSearchResultIndex - 1
                    _editState.value = editState.value.copy(currentSearchResultIndex = prevIndex)
                    viewModelScope.launch {
                        _events.emit(NotesUiEvent.ScrollToSearchResult(indices[prevIndex]))
                    }
                }
            }
            is NotesEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is NotesEvent.OnRestoreVersion -> {
                viewModelScope.launch {
                    val content = HtmlConverter.htmlToAnnotatedString(event.version.content)
                    _editState.value = editState.value.copy(
                        editingTitle = event.version.title,
                        editingContent = TextFieldValue(content),
                        editingNoteType = event.version.noteType
                    )
                    _events.emit(NotesUiEvent.ShowToast("Version restored"))
                }
            }
            is NotesEvent.NavigateToNoteByTitle -> {
                viewModelScope.launch {
                    _events.emit(NotesUiEvent.NavigateToNoteByTitle(event.title))
                }
            }
            is NotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    repository.getNoteById(event.note.note.id)?.let { noteWithAttachments ->
                        noteUseCases.deleteNote(noteWithAttachments.note)
                        recentlyDeletedNote = noteWithAttachments.note
                        _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                        updateWidgets()
                    }
                }
            }
            is NotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        repository.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                        updateWidgets()
                    }
                }
            }
            is NotesEvent.ToggleNoteSelection -> {
                val selectedIds = listState.value.selectedNoteIds.toMutableList()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _listState.value = listState.value.copy(selectedNoteIds = selectedIds)
            }
            is NotesEvent.ClearSelection -> {
                _listState.value = listState.value.copy(selectedNoteIds = emptyList())
            }
            is NotesEvent.SelectAllNotes -> {
                // Since we use paging, "Select All" is tricky.
                // For now, we can only select what's currently loaded in pinned notes.
                // Or we can try to get all IDs from the repository.
                viewModelScope.launch {
                    val allPinned = listState.value.pinnedNotes.map { it.note.id }
                    // We can't easily get all paged IDs without fetching them all.
                    // For now, just select pinned notes or implement a better "Select All".
                    _listState.value = listState.value.copy(selectedNoteIds = allPinned)
                }
            }
            is NotesEvent.TogglePinForSelectedNotes -> {
                viewModelScope.launch {
                    val allSelectedNotes = getSelectedNotes()
                    if (allSelectedNotes.isEmpty()) return@launch

                    val areNotesBeingPinned = allSelectedNotes.any { !it.note.isPinned }
                    
                    for (note in allSelectedNotes) {
                        repository.updateNote(note.note.copy(isPinned = areNotesBeingPinned))
                    }
                    
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    val message = if (areNotesBeingPinned) {
                        if (allSelectedNotes.size > 1) "${allSelectedNotes.size} notes pinned" else "Note pinned"
                    } else {
                        if (allSelectedNotes.size > 1) "${allSelectedNotes.size} notes unpinned" else "Note unpinned"
                    }
                    _events.emit(NotesUiEvent.ShowToast(message))
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleLockForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    if (selectedNotes.isEmpty()) return@launch
                    val areNotesBeingLocked = selectedNotes.firstOrNull()?.note?.isLocked == false
                    try {
                        for (note in selectedNotes) {
                            var noteToUpdate = note.note.copy(isLocked = areNotesBeingLocked)
                            if (!areNotesBeingLocked && note.note.isEncrypted) {
                                // If unlocking, decrypt first using the session's auth duration
                                // This will either succeed or return a note where isEncrypted is still true
                                val decrypted = com.suvojeet.notenext.util.CryptoUtils.decryptNote(note.note)
                                noteToUpdate = decrypted.copy(isLocked = false)
                            }
                            repository.updateNote(noteToUpdate)
                        }
                        _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                        val message = if (areNotesBeingLocked) {
                            if (selectedNotes.size > 1) "${selectedNotes.size} notes locked" else "Note locked"
                        } else {
                            if (selectedNotes.size > 1) "${selectedNotes.size} notes unlocked" else "Note unlocked"
                        }
                        _events.emit(NotesUiEvent.ShowToast(message))
                        updateWidgets()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val errorMessage = if (areNotesBeingLocked) "Failed to lock notes" else "Failed to unlock notes: Authentication may be required"
                        _events.emit(NotesUiEvent.ShowToast(errorMessage))
                    }
                }
            }
            is NotesEvent.DeleteSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to Bin"))
                    updateWidgets()
                }
            }
            is NotesEvent.ArchiveSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isArchived = !note.note.isArchived))
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    updateWidgets()
                }
            }
            is NotesEvent.ToggleImportantForSelectedNotes -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(isImportant = !note.note.isImportant))
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ChangeColorForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(color = event.color))
                    }
                    _listState.value = listState.value.copy(
                        selectedNoteIds = emptyList()
                    )
                    _events.emit(NotesUiEvent.ShowToast("Color updated"))
                }
            }
            is NotesEvent.CopySelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (noteWithAttachments in selectedNotes) {
                        val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)")
                        val newNoteId = repository.insertNote(copiedNote)
                        require(newNoteId <= Int.MAX_VALUE) { "Note ID overflow" }
                        noteWithAttachments.attachments.forEach { attachment ->
                            repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                        }
                        // Copy checklist items
                        val newChecklistItems = noteWithAttachments.checklistItems.map { item ->
                            item.copy(id = java.util.UUID.randomUUID().toString(), noteId = newNoteId.toInt())
                        }
                        repository.insertChecklistItems(newChecklistItems)
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    val message = if (selectedNotes.size > 1) "${selectedNotes.size} notes copied" else "Note copied"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.SendSelectedNotes -> {
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
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.SetReminderForSelectedNotes -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    val reminderDateTime = LocalDateTime.of(event.date, event.time)
                    val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    for (noteWithAttachments in selectedNotes) {
                        val updatedNote = noteWithAttachments.note.copy(
                            reminderTime = reminderMillis,
                            repeatOption = event.repeatOption.name // Store enum name as string
                        )
                        repository.updateNote(updatedNote)
                        alarmScheduler.schedule(updatedNote)
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("Reminder set for ${selectedNotes.size} notes"))
                }
            }
            is NotesEvent.SetLabelForSelectedNotes -> {
                viewModelScope.launch {
                    if (event.label.isNotBlank()) {
                        repository.insertLabel(Label(event.label))
                    }
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(label = event.label))
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                }
            }
            is NotesEvent.ExpandNote -> {
                lastCreatedNoteId = null
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        noteUseCases.getNote(event.noteId)?.let { noteWithAttachments ->
                            val note = if (noteWithAttachments.note.isLocked) {
                                // Time-based auth key (validity = 60s). Biometric auth in NotesScreen
                                // unlocks the AndroidKeyStore key for 60 seconds. We call decryptNote
                                // immediately after auth — cipher.init() succeeds within that window.
                                // authenticatedCipherTitle/Content are always null now (CryptoObject
                                // flow removed — it's incompatible with time-based keys).
                                com.suvojeet.notenext.util.CryptoUtils.decryptNote(
                                    noteWithAttachments.note,
                                    event.authenticatedCipherTitle,
                                    event.authenticatedCipherContent
                                )
                            } else {                                noteWithAttachments.note
                            }

                            val content = if (note.noteType == NoteType.TEXT) {
                                HtmlConverter.htmlToAnnotatedString(note.content)
                            } else {
                                AnnotatedString("")
                            }
                            
                            val checklist = if (note.noteType == NoteType.CHECKLIST) {
                                // Decrypt checklist items if the note is locked, 
                                // they will succeed due to the 60s window.
                                noteWithAttachments.checklistItems.sortedBy { it.position }.map { item ->
                                    if (note.isLocked && item.isEncrypted) {
                                        com.suvojeet.notenext.util.CryptoUtils.decryptChecklistItem(item, isLocked = true)
                                    } else {
                                        item
                                    }
                                }
                            } else {
                                emptyList<ChecklistItem>()
                            }

                            // Fetch versions
                            viewModelScope.launch {
                                repository.getNoteVersions(event.noteId).collect { versions ->
                                    _editState.value = editState.value.copy(editingNoteVersions = versions)
                                }
                            }
                            
                            val contentValue = TextFieldValue(content)
                            undoRedoManager.reset(note.title to contentValue)

                            savedStateHandle[KEY_EXPANDED_NOTE_ID] = event.noteId
                            savedStateHandle[KEY_EDITING_TITLE] = note.title
                            savedStateHandle[KEY_EDITING_CONTENT] = note.content

                            _editState.value = editState.value.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = contentValue,
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                isPinned = note.isPinned,
                                isArchived = note.isArchived,
                                editingLabel = note.label,
                                editingProjectId = note.projectId,
                                canUndo = undoRedoManager.canUndo.value,
                                canRedo = undoRedoManager.canRedo.value,
                                linkPreviews = note.linkPreviews,
                                editingNoteType = note.noteType,
                                editingChecklist = checklist,
                                editingAttachments = noteWithAttachments.attachments.map { it.copy(tempId = java.util.UUID.randomUUID().toString()) },
                                editingIsLocked = note.isLocked,
                                checklistInputValues = checklist.associate { item ->
                                    item.id to TextFieldValue(richTextController.parseMarkdownToAnnotatedString(item.text))
                                },
                                editingReminderTime = note.reminderTime,
                                editingRepeatOption = note.repeatOption,
                                summaryResult = note.aiSummary,
                                showSummaryDialog = false
                            )
                        }
                    } else {
                        undoRedoManager.reset("" to TextFieldValue())
                        savedStateHandle[KEY_EXPANDED_NOTE_ID] = -1
                        savedStateHandle[KEY_EDITING_TITLE] = ""
                        savedStateHandle[KEY_EDITING_CONTENT] = ""
                        _editState.value = editState.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = NoteGradients.NO_COLOR,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            canUndo = undoRedoManager.canUndo.value,
                            canRedo = undoRedoManager.canRedo.value,
                            editingLabel = null,
                            linkPreviews = emptyList(),
                            editingNoteType = event.noteType,
                            editingChecklist = emptyList(),
                            checklistInputValues = emptyMap(),
                            editingAttachments = emptyList(),
                            editingIsLocked = false,
                            editingNoteVersions = emptyList(),
                            summaryResult = null,
                            showSummaryDialog = false
                        )
                    }
                }
            }
            is NotesEvent.OnToggleLockClick -> {
                viewModelScope.launch {
                    val currentLockState = editState.value.editingIsLocked
                    val newLockState = !currentLockState
                    _editState.value = editState.value.copy(editingIsLocked = newLockState)
                    
                    // If note exists, update immediately using the current decrypted state
                    editState.value.expandedNoteId?.let { noteId ->
                         if (noteId != -1) {
                             saveNote(shouldCollapse = false)
                             _events.emit(NotesUiEvent.ShowToast(if (newLockState) "Note locked" else "Note unlocked"))
                             updateWidgets()
                         }
                    }
                }
            }
            is NotesEvent.CollapseNote -> {
                onEvent(NotesEvent.OnSaveNoteClick)
                savedStateHandle.remove<Int>(KEY_EXPANDED_NOTE_ID)
                savedStateHandle.remove<String>(KEY_EDITING_TITLE)
                savedStateHandle.remove<String>(KEY_EDITING_CONTENT)
            }

            is NotesEvent.AddChecklistItem -> {
                val (updatedChecklist, newItemId) = ChecklistManager.addChecklistItem(editState.value.editingChecklist)
                _editState.value = editState.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = editState.value.checklistInputValues + (newItemId to TextFieldValue(""))
                )
                scheduleAutoSave()
            }
            is NotesEvent.SwapChecklistItems -> {
                val updatedList = ChecklistManager.swapItems(editState.value.editingChecklist, event.fromId, event.toId)
                if (updatedList != editState.value.editingChecklist) {
                    _editState.value = editState.value.copy(editingChecklist = updatedList)
                    scheduleAutoSave()
                }
            }
            is NotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = ChecklistManager.deleteItem(editState.value.editingChecklist, event.itemId)
                _editState.value = editState.value.copy(
                    editingChecklist = updatedChecklist,
                    checklistInputValues = editState.value.checklistInputValues - event.itemId
                )
                scheduleAutoSave()
            }
            is NotesEvent.IndentChecklistItem -> {
                val updatedChecklist = ChecklistManager.indentItem(editState.value.editingChecklist, event.itemId)
                _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is NotesEvent.OutdentChecklistItem -> {
                val updatedChecklist = ChecklistManager.outdentItem(editState.value.editingChecklist, event.itemId)
                _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }

            is NotesEvent.OnChecklistItemCheckedChange -> {
                val updatedChecklist = ChecklistManager.changeItemCheckedState(editState.value.editingChecklist, event.itemId, event.isChecked)
                _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is NotesEvent.OnChecklistItemTextChange -> {
                val updatedChecklist = ChecklistManager.changeItemText(editState.value.editingChecklist, event.itemId, event.text)
                _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is NotesEvent.OnTitleChange -> {
                undoRedoManager.addState(event.title to editState.value.editingContent)
                _editState.value = editState.value.copy(
                    editingTitle = event.title,
                    canUndo = undoRedoManager.canUndo.value,
                    canRedo = undoRedoManager.canRedo.value,
                    summaryResult = null // Invalidate cache on title change
                )
                savedStateHandle[KEY_EDITING_TITLE] = event.title
                scheduleAutoSave()
            }
            is NotesEvent.OnContentChange -> {
                if (editState.value.editingNoteType == NoteType.TEXT) {
                    val newContent = event.content
                    val oldContent = editState.value.editingContent

                    val finalContent = richTextController.processContentChange(
                        oldContent,
                        newContent,
                        editState.value.activeStyles,
                        editState.value.activeHeadingStyle
                    )

                    val selection = finalContent.selection
                    val styles = if (selection.collapsed) {
                        if (selection.start > 0) {
                            finalContent.annotatedString.spanStyles.filter {
                                it.start <= selection.start - 1 && it.end >= selection.start
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        finalContent.annotatedString.spanStyles.filter {
                            maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                        }
                    }
                    
                    // Only add to undo history if text actually changed (not just selection)
                    val textChanged = oldContent.text != finalContent.text
                    if (textChanged) {
                        undoRedoManager.addState(editState.value.editingTitle to finalContent)
                    }
                    
                    _editState.value = editState.value.copy(
                        editingContent = finalContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value,
                        isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                        isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                        isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline },
                        summaryResult = null // Invalidate cache on content change
                    )

                    viewModelScope.launch {
                        savedStateHandle[KEY_EDITING_CONTENT] = HtmlConverter.annotatedStringToHtml(finalContent.annotatedString)
                    }

                    if (textChanged) {
                        scheduleAutoSave()
                    }

                    // Debounced Link detection
                    val urlRegex = "(https?://[\\w.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)".toRegex()
                    val detectedUrls = urlRegex.findAll(finalContent.text).map { it.value }.toSet()

                    detectedUrls.forEach { url ->
                        onEvent(NotesEvent.OnLinkDetected(url))
                    }
                }
            }
            is NotesEvent.OnChecklistItemValueChange -> {
                val updatedInputValues = editState.value.checklistInputValues.toMutableMap()
                updatedInputValues[event.itemId] = event.value

                // Check for styles at selection to update toolbar state
                 val selection = event.value.selection
                 val styles = if (selection.collapsed) {
                    if (selection.start > 0) {
                        event.value.annotatedString.spanStyles.filter {
                            it.start <= selection.start - 1 && it.end >= selection.start
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    event.value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                }

                _editState.value = editState.value.copy(
                    checklistInputValues = updatedInputValues,
                     isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                     isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                     isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                )
                scheduleAutoSave()
                
                // Async update for persistence model
                viewModelScope.launch {
                    val updatedText = HtmlConverter.annotatedStringToHtml(event.value.annotatedString).let {
                        com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                    }

                    val updatedChecklist = editState.value.editingChecklist.map {
                        if (it.id == event.itemId) it.copy(text = updatedText) else it
                    }
                    _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
                }
            }
            is NotesEvent.OnChecklistItemFocus -> {
                _editState.value = editState.value.copy(focusedChecklistItemId = event.itemId)
                // Update active styles based on the focused item's cursor position handled in ValueChange or just reset/check here
                val value = editState.value.checklistInputValues[event.itemId]
                if (value != null) {
                     val selection = value.selection
                     val styles = value.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                     _editState.value = editState.value.copy(
                         isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                         isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                         isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                     )
                }
            }
            is NotesEvent.ApplyStyleToContent -> {
                if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    val focusedId = editState.value.focusedChecklistItemId
                    if (focusedId != null) {
                        val currentValue = editState.value.checklistInputValues[focusedId]
                        if (currentValue != null) {
                             val result = richTextController.toggleStyle(
                                currentValue,
                                event.style,
                                emptySet(), // We don't track activeStyles per item easily yet, relies on result
                                editState.value.isBoldActive,
                                editState.value.isItalicActive,
                                editState.value.isUnderlineActive
                            )
                            if (result.updatedContent != null) {
                                onEvent(NotesEvent.OnChecklistItemValueChange(focusedId, result.updatedContent))
                            }
                        }
                    }
                } else {
                    val result = richTextController.toggleStyle(
                    editState.value.editingContent,
                    event.style,
                    editState.value.activeStyles,
                    editState.value.isBoldActive,
                    editState.value.isItalicActive,
                    editState.value.isUnderlineActive
                )

                if (result.updatedActiveStyles != null) {
                    val activeStyles = result.updatedActiveStyles
                    _editState.value = editState.value.copy(
                        activeStyles = activeStyles,
                        isBoldActive = activeStyles.any { it.fontWeight == FontWeight.Bold },
                        isItalicActive = activeStyles.any { it.fontStyle == FontStyle.Italic },
                        isUnderlineActive = activeStyles.any { it.textDecoration == TextDecoration.Underline }
                    )
                } else if (result.updatedContent != null) {
                    undoRedoManager.addState(editState.value.editingTitle to result.updatedContent)
                    _editState.value = editState.value.copy(
                        editingContent = result.updatedContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
                }
            }
            is NotesEvent.ApplyBulletedList -> {
                val updatedContent = richTextController.toggleBulletedList(editState.value.editingContent)
                undoRedoManager.addState(editState.value.editingTitle to updatedContent)
                _editState.value = editState.value.copy(
                    editingContent = updatedContent,
                    canUndo = undoRedoManager.canUndo.value,
                    canRedo = undoRedoManager.canRedo.value
                )
                scheduleAutoSave()
            }
            is NotesEvent.ApplyHeadingStyle -> {
                val updatedContent = richTextController.applyHeading(editState.value.editingContent, event.level)

                if (updatedContent == null) {
                    // Selection is collapsed, update active styles for future typing
                    val newActiveStyles = mutableSetOf<SpanStyle>()
                    if (event.level != 0) {
                        newActiveStyles.add(richTextController.getHeadingStyle(event.level))
                    }
                    _editState.value = editState.value.copy(
                        activeHeadingStyle = event.level,
                        activeStyles = newActiveStyles,
                        isBoldActive = false,
                        isItalicActive = false,
                        isUnderlineActive = false
                    )
                } else {
                    // Applied to selection
                    undoRedoManager.addState(editState.value.editingTitle to updatedContent)

                    _editState.value = editState.value.copy(
                        editingContent = updatedContent,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value,
                        activeHeadingStyle = event.level
                    )
                }
            }
            is NotesEvent.OnColorChange -> {
                _editState.value = editState.value.copy(editingColor = event.color)
                scheduleAutoSave()
            }
            is NotesEvent.OnLabelChange -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.label))
                    _editState.value = editState.value.copy(editingLabel = event.label)
                }
            }
            is NotesEvent.OnTogglePinClick -> {
                val newPinnedState = !editState.value.isPinned
                _editState.value = editState.value.copy(isPinned = newPinnedState)
                viewModelScope.launch {
                    saveNote(shouldCollapse = false)
                    val message = if (newPinnedState) "Note pinned" else "Note unpinned"
                    _events.emit(NotesUiEvent.ShowToast(message))
                }
            }
            is NotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            repository.updateNote(updatedNote)
                            val updatedNotesList = listState.value.notes.map { if (it.note.id == updatedNote.id) it.copy(note = updatedNote.toNoteSummary()) else it }
                            _editState.value = editState.value.copy(
                                isArchived = updatedNote.isArchived,
                            )
                            _listState.value = listState.value.copy(
                                notes = updatedNotesList
                            )
                            updateWidgets()
                        }
                    }
                }
            }
            is NotesEvent.OnUndoClick -> {
                undoRedoManager.undo()?.let { (title, content) ->
                    _editState.value = editState.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
            }
            is NotesEvent.OnRedoClick -> {
                undoRedoManager.redo()?.let { (title, content) ->
                    _editState.value = editState.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        canUndo = undoRedoManager.canUndo.value,
                        canRedo = undoRedoManager.canRedo.value
                    )
                }
            }
            is NotesEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    saveNote(shouldCollapse = true)
                }
            }
            is NotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let {
                        if (it != -1) {
                            repository.getNoteById(it)?.let { note ->
                                repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(NotesUiEvent.ShowToast("Note moved to Bin"))
                                updateWidgets()
                            }
                        }
                    }
                    _editState.value = editState.value.copy(expandedNoteId = null)
                }
            }
            is NotesEvent.OnCopyCurrentNoteClick -> {
                viewModelScope.launch {
                    editState.value.expandedNoteId?.let {
                        repository.getNoteById(it)?.let { noteWithAttachments ->
                            val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                            val newNoteId = repository.insertNote(copiedNote)
                            require(newNoteId <= Int.MAX_VALUE) { "Note ID overflow" }
                            noteWithAttachments.attachments.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                            }
                            _events.emit(NotesUiEvent.ShowToast("Note copied"))
                        }
                    }
                }
            }
            is NotesEvent.OnAddLabelsToCurrentNoteClick -> {
                _editState.value = editState.value.copy(showLabelDialog = true)
            }
            is NotesEvent.DismissLabelDialog -> {
                _editState.value = editState.value.copy(showLabelDialog = false)
            }
            is NotesEvent.FilterByLabel -> {
                _listState.value = listState.value.copy(filteredLabel = event.label)
            }
            is NotesEvent.FilterByProject -> {
                _filteredProjectId.value = event.projectId
            }
            is NotesEvent.ToggleLayout -> {
                val newLayout = if (listState.value.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
                _listState.value = listState.value.copy(layoutType = newLayout)
            }
            is NotesEvent.SortNotes -> {
                _sortType.value = event.sortType
            }
            is NotesEvent.OnRemoveLinkPreview -> {
                val updatedLinkPreviews = editState.value.linkPreviews.filter { it.url != event.url }
                _editState.value = editState.value.copy(linkPreviews = updatedLinkPreviews)
                viewModelScope.launch {
                    _events.emit(NotesUiEvent.LinkPreviewRemoved)
                }
            }
            is NotesEvent.OnInsertLink -> {
                val content = editState.value.editingContent
                val selection = content.selection
                if (!selection.collapsed) {
                    val selectedText = content.text.substring(selection.start, selection.end)
                    val newAnnotatedString = buildAnnotatedString {
                        append(content.annotatedString.subSequence(0, selection.start))
                        pushStringAnnotation(tag = "URL", annotation = event.url)
                        withStyle(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append(selectedText)
                        }
                        pop()
                        append(content.annotatedString.subSequence(selection.end, content.text.length))
                    }
                    val newTextFieldValue = content.copy(annotatedString = newAnnotatedString)
                    _editState.value = editState.value.copy(editingContent = newTextFieldValue)
                }
            }
            is NotesEvent.ClearNewlyAddedChecklistItemId -> {
                _editState.value = editState.value.copy(newlyAddedChecklistItemId = null)
            }
            is NotesEvent.AddAttachment -> {
                val type = when {
                    event.mimeType.startsWith("image") -> AttachmentType.IMAGE
                    event.mimeType.startsWith("video") -> AttachmentType.VIDEO
                    event.mimeType.startsWith("audio") -> AttachmentType.AUDIO
                    else -> AttachmentType.FILE
                }
                val attachment = Attachment(
                    noteId = editState.value.expandedNoteId ?: -1,
                    uri = event.uri,
                    type = type,
                    mimeType = event.mimeType,
                    tempId = java.util.UUID.randomUUID().toString()
                )
                _editState.value = editState.value.copy(editingAttachments = editState.value.editingAttachments + attachment)
                scheduleAutoSave()
            }
            is NotesEvent.OnLinkDetected -> {
                linkDetectionJob?.cancel()
                linkDetectionJob = viewModelScope.launch {
                    delay(1000L) // 1s delay - only fetch when user stops typing
                    
                    val existingLinkPreviews = editState.value.linkPreviews
                    if (existingLinkPreviews.none { it.url == event.url }) {
                        val preview = linkPreviewRepository.getLinkPreview(event.url)
                        onEvent(NotesEvent.OnLinkPreviewFetched(
                            url = preview.url,
                            title = preview.title,
                            description = preview.description,
                            imageUrl = preview.imageUrl
                        ))
                    }
                }
            }
            is NotesEvent.OnLinkPreviewFetched -> {
                val newPreview = LinkPreview(event.url, event.title, event.description, event.imageUrl)
                val updatedPreviews = (editState.value.linkPreviews + newPreview).distinctBy { it.url }
                _editState.value = editState.value.copy(linkPreviews = updatedPreviews)
                scheduleAutoSave()
            }
            is NotesEvent.RemoveAttachment -> {
                viewModelScope.launch {
                    val attachmentToRemove = editState.value.editingAttachments.firstOrNull { it.tempId == event.tempId }
                    attachmentToRemove?.let {
                        if (it.id != 0) { // Only delete from DB if it has a real ID
                            repository.deleteAttachmentById(it.id)
                        }
                        val updatedAttachments = editState.value.editingAttachments.filter { attachment -> attachment.tempId != event.tempId }
                        _editState.value = editState.value.copy(editingAttachments = updatedAttachments)
                        scheduleAutoSave()
                    }
                }
            }
            is NotesEvent.CreateProject -> {
                viewModelScope.launch {
                    val newProject = Project(name = event.name)
                    repository.insertProject(newProject)
                    _events.emit(NotesUiEvent.ProjectCreated(event.name))
                }
            }
            is NotesEvent.MoveSelectedNotesToProject -> {
                viewModelScope.launch {
                    val selectedNotes = getSelectedNotes()
                    for (note in selectedNotes) {
                        repository.updateNote(note.note.copy(projectId = event.projectId))
                    }
                    _listState.value = listState.value.copy(selectedNoteIds = emptyList())
                    _events.emit(NotesUiEvent.ShowToast("${selectedNotes.size} notes moved to project"))
                }
            }
            is NotesEvent.OnToggleNoteType -> {
                val currentType = editState.value.editingNoteType
                if (currentType == NoteType.TEXT) {
                    // Convert TEXT to CHECKLIST
                    val lines = editState.value.editingContent.text.split("\n")
                    val checklistItems = lines.filter { it.isNotBlank() }.mapIndexed { index, text ->
                        ChecklistItem(text = text.trim(), isChecked = false, position = index)
                    }
                    // If empty, add one empty item
                    val finalItems = if (checklistItems.isEmpty()) listOf(ChecklistItem(text = "", isChecked = false, position = 0)) else checklistItems
                    
                    _editState.value = editState.value.copy(
                        editingNoteType = NoteType.CHECKLIST,
                        editingChecklist = finalItems,
                        editingContent = TextFieldValue("") // Clear text content
                    )
                } else {
                    // Convert CHECKLIST to TEXT
                    val textContent = editState.value.editingChecklist.joinToString("\n") { it.text }
                    _editState.value = editState.value.copy(
                        editingNoteType = NoteType.TEXT,
                        editingContent = TextFieldValue(textContent),
                        editingChecklist = emptyList()
                    )
                }
            }
            is NotesEvent.ConvertToTodo -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val title = editState.value.editingTitle
                    val content = editState.value.editingContent.text
                    val noteType = editState.value.editingNoteType
                    val projectId = editState.value.editingProjectId
                    
                    val maxPos = todoRepository.getMaxPosition()
                    val todo = com.suvojeet.notenext.data.TodoItem(
                        title = if (title.isBlank()) "Converted Note" else title,
                        description = if (noteType == NoteType.TEXT) content else "",
                        projectId = projectId,
                        position = maxPos + 1,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val todoId = todoRepository.insertTodo(todo).toInt()
                    
                    if (noteType == NoteType.CHECKLIST) {
                        val subtasks = editState.value.editingChecklist.map { item ->
                            com.suvojeet.notenext.data.TodoSubtask(
                                todoId = todoId,
                                text = item.text,
                                isChecked = item.isChecked,
                                position = item.position
                            )
                        }
                        todoRepository.insertSubtasks(subtasks)
                    }
                    
                    // Bin the note after conversion
                    val currentNoteId = editState.value.expandedNoteId
                    if (currentNoteId != null && currentNoteId != -1) {
                        repository.getNoteById(currentNoteId)?.let { noteWithAttachments ->
                            repository.updateNote(noteWithAttachments.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                        }
                    }
                    
                    onEvent(NotesEvent.CollapseNote)
                    _events.emit(NotesUiEvent.ShowToast("Converted to Todo successfully"))
                }
            }
            is NotesEvent.ToggleCheckedItemsExpanded -> {
                _editState.value = editState.value.copy(
                    isCheckedItemsExpanded = !editState.value.isCheckedItemsExpanded
                )
            }
            is NotesEvent.SummarizeNote -> {
                val content = if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    editState.value.editingChecklist.joinToString("\n") { it.text }
                } else {
                    editState.value.editingContent.text
                }

                if (content.isNotBlank()) {
                    if (editState.value.summaryResult != null) {
                         // Cache hit - just show dialog
                         _editState.value = editState.value.copy(showSummaryDialog = true)
                    } else {
                         // Cache miss - fetch
                        _editState.value = editState.value.copy(isSummarizing = true, showSummaryDialog = true)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                            groqRepository.summarizeNote(content).collect { result ->
                                result.onSuccess { summary ->
                                    _editState.value = editState.value.copy(isSummarizing = false, summaryResult = summary)
                                }.onFailure { failure ->
                                    val errorMessage = when (failure) {
                                        is GroqResult.RateLimited -> "AI is busy. Please try again in ${failure.retryAfterSeconds}s."
                                        is GroqResult.InvalidKey -> "Invalid API key. Check your settings."
                                        is GroqResult.NetworkError -> "Network error: ${failure.message}"
                                        is GroqResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
                                        else -> "Failed to summarize note."
                                    }
                                    _editState.value = editState.value.copy(
                                        isSummarizing = false,
                                        summaryResult = "Error: $errorMessage"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is NotesEvent.ClearSummary -> {
                _editState.value = editState.value.copy(showSummaryDialog = false)
            }
            is NotesEvent.DeleteAllCheckedItems -> {
                val updatedChecklist = ChecklistManager.deleteAllCheckedItems(editState.value.editingChecklist)
                _editState.value = editState.value.copy(editingChecklist = updatedChecklist)
            }
            is NotesEvent.CreateNoteFromSharedText -> {
                undoRedoManager.reset("" to TextFieldValue(event.text))
                _editState.value = editState.value.copy(
                    expandedNoteId = -1,
                    editingTitle = "",
                    editingContent = TextFieldValue(event.text),
                    editingColor = NoteGradients.NO_COLOR,
                    editingIsNewNote = true,
                    editingLastEdited = 0,
                    canUndo = undoRedoManager.canUndo.value,
                    canRedo = undoRedoManager.canRedo.value,
                    editingLabel = null,
                    linkPreviews = emptyList(),
                    editingNoteType = NoteType.TEXT,
                    editingChecklist = emptyList(),
                    editingAttachments = emptyList()
                )
            }
            is NotesEvent.SetInitialTitle -> {
                _editState.value = editState.value.copy(
                    editingTitle = event.title
                )
            }
            is NotesEvent.OnReminderChange -> {
                _editState.value = editState.value.copy(
                    editingReminderTime = event.time,
                    editingRepeatOption = event.repeatOption
                )
                scheduleAutoSave()
            }
            is NotesEvent.ExportNote -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val contentToExport = if (event.format == "MD") {
                            if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                                editState.value.editingChecklist.joinToString("\n") { 
                                    (if (it.isChecked) "- [x] " else "- [ ] ") + it.text 
                                }
                            } else {
                                HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString).let {
                                    com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                                }
                            }
                        } else {
                            // TXT (Plain Text)
                            if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                                editState.value.editingChecklist.joinToString("\n") { 
                                    (if (it.isChecked) "[x] " else "[ ] ") + it.text 
                                }
                            } else {
                                editState.value.editingContent.text
                            }
                        }

                        context.contentResolver.openOutputStream(event.uri)?.use { outputStream ->
                            outputStream.write(contentToExport.toByteArray())
                        }
                        _events.emit(NotesUiEvent.ShowToast("Exported successfully"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _events.emit(NotesUiEvent.ShowToast("Export failed: ${e.message}"))
                    }
                }
            }
            else -> {
                // Handle any other events or do nothing
            }
        }
    }


    private suspend fun getSelectedNotes(): List<NoteWithAttachments> {
        val selectedIds = listState.value.selectedNoteIds
        return selectedIds.mapNotNull { id ->
            repository.getNoteById(id)
        }
    }

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
    }

    suspend fun getNoteLockStatus(noteId: Int): Boolean {
        return repository.getNoteById(noteId)?.note?.isLocked == true
    }

    suspend fun getNoteIdByTitle(title: String): Int? {
        return repository.getNoteIdByTitle(title)
    }

    private suspend fun saveNote(shouldCollapse: Boolean) {
        val expandedId = editState.value.expandedNoteId
        val externalUri = editState.value.externalUri
        
        if (expandedId == null) return
        
        // Handle external files separately
        if (externalUri != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val content = if (editState.value.editingNoteType == NoteType.TEXT) {
                         editState.value.editingContent.text
                    } else {
                        editState.value.editingChecklist.joinToString("\n") { (if (it.isChecked) "[x] " else "[ ] ") + it.text }
                    }
                    context.contentResolver.openOutputStream(externalUri, "rwt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _events.emit(NotesUiEvent.ShowToast("Failed to save external file: ${e.message}"))
                }
            }
            if (shouldCollapse) {
                 _editState.value = editState.value.copy(
                    expandedNoteId = null,
                    externalUri = null,
                    editingTitle = "",
                    editingContent = TextFieldValue()
                )
            }
            return
        }

        // If it's a new note (-1), check if we already have a real ID from a previous auto-save
        val noteId = if (expandedId == -1 && lastCreatedNoteId != null) lastCreatedNoteId!! else expandedId

        val title = editState.value.editingTitle
        val content = if (editState.value.editingNoteType == NoteType.TEXT) {
            HtmlConverter.annotatedStringToHtml(editState.value.editingContent.annotatedString)
        } else {
            ""
        }

        if (title.isBlank() && (editState.value.editingNoteType == NoteType.TEXT && content.isBlank() || editState.value.editingNoteType == NoteType.CHECKLIST && editState.value.editingChecklist.all { it.text.isBlank() })) {
            if (noteId != -1) { // It's an existing note, so delete it
                repository.getNoteById(noteId)?.let { repository.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
            }
        } else {
            val currentTime = System.currentTimeMillis()
            val note = if (noteId == -1) { // New note (truly new, first save)
                Note(
                    title = title,
                    content = content,
                    createdAt = currentTime,
                    lastEdited = currentTime,
                    color = editState.value.editingColor,
                    isPinned = editState.value.isPinned,
                    isArchived = editState.value.isArchived,
                    label = editState.value.editingLabel,
                    linkPreviews = editState.value.linkPreviews,
                    noteType = editState.value.editingNoteType,
                    isLocked = editState.value.editingIsLocked,
                    reminderTime = editState.value.editingReminderTime,
                    repeatOption = editState.value.editingRepeatOption
                )
            } else { // Existing note
                repository.getNoteById(noteId)?.let { existingNote ->
                    existingNote.note.copy(
                        title = title,
                        content = content,
                        lastEdited = currentTime,
                        color = editState.value.editingColor,
                        isPinned = editState.value.isPinned,
                        isArchived = editState.value.isArchived,
                        label = editState.value.editingLabel,
                        linkPreviews = editState.value.linkPreviews,
                        noteType = editState.value.editingNoteType,
                        isLocked = editState.value.editingIsLocked,
                        reminderTime = editState.value.editingReminderTime,
                        repeatOption = editState.value.editingRepeatOption,
                        aiSummary = editState.value.summaryResult,
                        isEncrypted = false,
                        iv = null
                    )
                }
            }
            if (note != null) {
                val currentNoteId = if (noteId == -1) { // New note
                    repository.insertNote(note)
                } else { // Existing note
                    // Before updating, save current state as a version if it's not a new note
                    repository.getNoteById(noteId)?.let { oldNoteWithAttachments ->
                        val oldNote = oldNoteWithAttachments.note
                        // Only save version if content or title changed
                        if (oldNote.title != title || oldNote.content != content) {
                            repository.insertNoteVersion(
                                NoteVersion(
                                    noteId = noteId,
                                    title = oldNote.title,
                                    content = oldNote.content,
                                    timestamp = oldNote.lastEdited,
                                    noteType = oldNote.noteType
                                )
                            )
                            repository.limitNoteVersions(noteId, 10)
                        }
                    }
                    check(!note.isEncrypted) { "Attempting to save encrypted note from ViewModel — decrypt first." }
                    repository.updateNote(note)
                    noteId.toLong() // Convert Int to Long for consistency
                }
                require(currentNoteId <= Int.MAX_VALUE) { "Note ID overflow" }

                if (editState.value.editingReminderTime != null) {
                    alarmScheduler.schedule(note.copy(id = currentNoteId.toInt()))
                } else if (noteId != -1) {
                    alarmScheduler.cancel(note.copy(id = currentNoteId.toInt()))
                }

                // Handle Checklist Items
                if (editState.value.editingNoteType == NoteType.CHECKLIST) {
                    val checklistItems = editState.value.editingChecklist.mapIndexed { index, item ->
                        item.copy(noteId = currentNoteId.toInt(), position = index)
                    }
                    repository.deleteChecklistForNote(currentNoteId.toInt())
                    repository.insertChecklistItems(checklistItems)
                }

                // Handle attachments
                val existingAttachmentsInDb = if (noteId != -1) {
                    repository.getNoteById(noteId)?.attachments ?: emptyList()
                } else {
                    emptyList()
                }

                val attachmentsToAdd = editState.value.editingAttachments.filter { uiAttachment ->
                    existingAttachmentsInDb.none { dbAttachment ->
                        dbAttachment.uri == uiAttachment.uri && dbAttachment.type == uiAttachment.type
                    }
                }

                val attachmentsToRemove = existingAttachmentsInDb.filter { dbAttachment ->
                    editState.value.editingAttachments.none { uiAttachment ->
                        uiAttachment.uri == dbAttachment.uri && uiAttachment.type == dbAttachment.type
                    }
                }

                attachmentsToRemove.forEach { attachment ->
                    repository.deleteAttachment(attachment)
                }

                attachmentsToAdd.forEach { attachment ->
                    repository.insertAttachment(attachment.copy(noteId = currentNoteId.toInt()))
                }

                // If it was a new note, we now have a real ID. 
                // We update editingIsNewNote to false so next saves know it's not new anymore.
                // But we DO NOT update expandedNoteId yet if it was -1, to avoid the 'jolt' in AnimatedContent.
                if (expandedId == -1 && lastCreatedNoteId == null) {
                    _editState.value = editState.value.copy(
                        editingIsNewNote = false,
                        expandedNoteId = -1, // Keep it -1 to avoid triggering NoteTransition in NotesScreen
                        editingLastEdited = currentTime
                    )
                    lastCreatedNoteId = currentNoteId.toInt()
                } else {
                    // Update lastEdited time so UI (MoreOptionsSheet) shows it
                    _editState.value = editState.value.copy(
                        editingLastEdited = currentTime
                    )
                }
            }
        }

        if (shouldCollapse) {
            lastCreatedNoteId = null
            // Reset editing state and collapse
            _editState.value = editState.value.copy(
                expandedNoteId = null,
                editingTitle = "",
                editingContent = TextFieldValue(),
                editingColor = NoteGradients.NO_COLOR,
                editingIsNewNote = true,
                editingLastEdited = 0,
                canUndo = false,
                canRedo = false,
                isPinned = false,
                isArchived = false,
                editingLabel = null,
                editingProjectId = null,
                isBoldActive = false,
                isItalicActive = false,
                isUnderlineActive = false,
                activeStyles = emptySet(),
                linkPreviews = emptyList(),
                editingChecklist = emptyList(),
                editingAttachments = emptyList(),
                editingReminderTime = null,
                editingRepeatOption = null
            )
        }
        updateWidgets()
    }
}

