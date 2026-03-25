package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEditState

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
        showLabelDialog = this.showLabelDialog,
        isBoldActive = this.isBoldActive,
        isItalicActive = this.isItalicActive,
        isUnderlineActive = this.isUnderlineActive,
        activeStyles = this.activeStyles,
        linkPreviews = this.linkPreviews,
        editingNoteType = this.editingNoteType,
        editingChecklist = this.editingChecklist,
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
