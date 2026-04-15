package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesListState

fun ProjectNotesState.toNotesEditState(): NotesEditState {
    return NotesEditState(
        expandedNoteId = this.expandedNoteId,
        editingTitle = this.editingTitle,
        editingContent = this.editingContent,
        editingColor = this.editingColor,
        editingIsNewNote = this.editingIsNewNote,
        editingLastEdited = this.editingLastEdited,
        canUndo = this.editingHistoryIndex > 0,
        canRedo = this.editingHistory.isNotEmpty() && this.editingHistoryIndex < this.editingHistory.lastIndex,
        isPinned = this.isPinned,
        isArchived = this.isArchived,
        editingLabel = this.editingLabel,
        isBoldActive = this.isBoldActive,
        isItalicActive = this.isItalicActive,
        isUnderlineActive = this.isUnderlineActive,
        activeHeadingStyle = this.activeHeadingStyle,
        activeStyles = this.activeStyles,
        linkPreviews = this.linkPreviews,
        editingNoteType = this.editingNoteType,
        editingChecklist = this.editingChecklist,
        checklistInputValues = this.checklistInputValues,
        focusedChecklistItemId = this.focusedChecklistItemId,
        isCheckedItemsExpanded = this.isCheckedItemsExpanded,
        newlyAddedChecklistItemId = this.newlyAddedChecklistItemId,
        editingAttachments = this.editingAttachments,
        editingIsLocked = this.editingIsLocked,
        editingNoteVersions = this.editingNoteVersions,
        editingReminderTime = this.editingReminderTime,
        editingRepeatOption = this.editingRepeatOption,
        saveStatus = this.saveStatus,
        isSummarizing = this.isSummarizing,
        summaryResult = this.summaryResult,
        showSummaryDialog = this.showSummaryDialog,
        isGeneratingChecklist = this.isGeneratingChecklist,
        generatedChecklistPreview = this.generatedChecklistPreview,
        isFixingGrammar = this.isFixingGrammar,
        fixedContentPreview = this.fixedContentPreview,
        originalContentBackup = this.originalContentBackup,
        isMentionPopupVisible = false,
        mentionSearchQuery = "",
        mentionableNotes = emptyList()
    )
}

fun ProjectNotesState.toNotesListState(): NotesListState {
    return NotesListState(
        notes = this.notes,
        pinnedNotes = this.notes.filter { it.note.isPinned },
        layoutType = this.layoutType,
        sortType = this.sortType,
        selectedNoteIds = this.selectedNoteIds,
        labels = this.labels,
        filteredLabel = this.filteredLabel,
        isLoading = false,
        projects = this.projects,
        searchQuery = "",
        filteredProjectId = null
    )
}
