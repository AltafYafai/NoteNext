package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEditEvent
import com.suvojeet.notenext.ui.notes.NotesListEvent
import com.suvojeet.notenext.ui.notes.NotesUiEvent

fun NotesEditEvent.toProjectNotesEvent(): ProjectNotesEvent {
    return when (this) {
        is NotesEditEvent.OnChecklistItemCheckedChange -> ProjectNotesEvent.OnChecklistItemCheckedChange(this.itemId, this.isChecked)
        is NotesEditEvent.OnChecklistItemTextChange -> ProjectNotesEvent.OnChecklistItemTextChange(this.itemId, this.text)
        is NotesEditEvent.OnChecklistItemValueChange -> ProjectNotesEvent.OnChecklistItemValueChange(this.itemId, this.value)
        is NotesEditEvent.OnChecklistItemFocus -> ProjectNotesEvent.OnChecklistItemFocus(this.itemId)
        is NotesEditEvent.SwapChecklistItems -> ProjectNotesEvent.SwapChecklistItems(this.fromId, this.toId)
        is NotesEditEvent.AddChecklistItem -> ProjectNotesEvent.AddChecklistItem
        is NotesEditEvent.DeleteChecklistItem -> ProjectNotesEvent.DeleteChecklistItem(this.itemId)
        is NotesEditEvent.IndentChecklistItem -> ProjectNotesEvent.IndentChecklistItem(this.itemId)
        is NotesEditEvent.OutdentChecklistItem -> ProjectNotesEvent.OutdentChecklistItem(this.itemId)
        is NotesEditEvent.OnTitleChange -> ProjectNotesEvent.OnTitleChange(this.title)
        is NotesEditEvent.OnContentChange -> ProjectNotesEvent.OnContentChange(this.content)
        is NotesEditEvent.ApplyStyleToContent -> ProjectNotesEvent.ApplyStyleToContent(this.style)
        is NotesEditEvent.ApplyHeadingStyle -> ProjectNotesEvent.ApplyHeadingStyle(this.level)
        is NotesEditEvent.OnColorChange -> ProjectNotesEvent.OnColorChange(this.color)
        is NotesEditEvent.OnSaveNoteClick -> ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = true)
        is NotesEditEvent.OnDeleteNoteClick -> ProjectNotesEvent.OnDeleteNoteClick
        is NotesEditEvent.OnTogglePinClick -> ProjectNotesEvent.OnTogglePinClick
        is NotesEditEvent.OnToggleLockClick -> ProjectNotesEvent.OnToggleLockClick
        is NotesEditEvent.OnToggleArchiveClick -> ProjectNotesEvent.OnToggleArchiveClick
        is NotesEditEvent.OnUndoClick -> ProjectNotesEvent.OnUndoClick
        is NotesEditEvent.OnRedoClick -> ProjectNotesEvent.OnRedoClick
        is NotesEditEvent.OnCopyCurrentNoteClick -> ProjectNotesEvent.OnCopyCurrentNoteClick
        is NotesEditEvent.OnAddLabelsToCurrentNoteClick -> ProjectNotesEvent.OnAddLabelsToCurrentNoteClick
        is NotesEditEvent.OnLabelChange -> ProjectNotesEvent.OnLabelChange(this.label)
        is NotesEditEvent.SetInitialTitle -> ProjectNotesEvent.SetInitialTitle(this.title)
        is NotesEditEvent.DismissLabelDialog -> ProjectNotesEvent.DismissLabelDialog
        is NotesEditEvent.OnLinkDetected -> ProjectNotesEvent.OnLinkDetected(this.url)
        is NotesEditEvent.OnLinkPreviewFetched -> ProjectNotesEvent.OnLinkPreviewFetched(this.url, this.title, this.description, this.imageUrl)
        is NotesEditEvent.OnRemoveLinkPreview -> ProjectNotesEvent.OnRemoveLinkPreview(this.url)
        is NotesEditEvent.OnInsertLink -> ProjectNotesEvent.OnInsertLink(this.url)
        is NotesEditEvent.ClearNewlyAddedChecklistItemId -> ProjectNotesEvent.ClearNewlyAddedChecklistItemId
        is NotesEditEvent.AddAttachment -> ProjectNotesEvent.AddAttachment(this.uri, this.mimeType)
        is NotesEditEvent.RemoveAttachment -> ProjectNotesEvent.RemoveAttachment(this.tempId)
        is NotesEditEvent.OnRestoreVersion -> ProjectNotesEvent.OnRestoreVersion(this.version)
        is NotesEditEvent.NavigateToNoteByTitle -> ProjectNotesEvent.NavigateToNoteByTitle(this.title)
        is NotesEditEvent.OnReminderChange -> ProjectNotesEvent.OnReminderChange(this.time, this.repeatOption)
        is NotesEditEvent.OnToggleNoteType -> ProjectNotesEvent.OnToggleNoteType
        is NotesEditEvent.DeleteAllCheckedItems -> ProjectNotesEvent.DeleteAllCheckedItems
        is NotesEditEvent.ToggleCheckedItemsExpanded -> ProjectNotesEvent.ToggleCheckedItemsExpanded
        is NotesEditEvent.SummarizeNote -> ProjectNotesEvent.SummarizeNote
        is NotesEditEvent.GenerateChecklist -> ProjectNotesEvent.GenerateChecklist(this.topic)
        is NotesEditEvent.InsertGeneratedChecklist -> ProjectNotesEvent.InsertGeneratedChecklist(this.items)
        is NotesEditEvent.ClearGeneratedChecklist -> ProjectNotesEvent.ClearGeneratedChecklist
        is NotesEditEvent.ClearSummary -> ProjectNotesEvent.ClearSummary
        is NotesEditEvent.FixGrammar -> ProjectNotesEvent.FixGrammar
        is NotesEditEvent.ApplyGrammarFix -> ProjectNotesEvent.ApplyGrammarFix
        is NotesEditEvent.ClearGrammarFix -> ProjectNotesEvent.ClearGrammarFix
        is NotesEditEvent.AutoSaveNote -> ProjectNotesEvent.AutoSaveNote
        is NotesEditEvent.ExportNote -> ProjectNotesEvent.ExportNote(this.uri, this.format)
        is NotesEditEvent.ExpandNote -> ProjectNotesEvent.ExpandNote(this.noteId, this.noteType)
        is NotesEditEvent.CollapseNote -> ProjectNotesEvent.CollapseNote
        is NotesEditEvent.OnMentionSearchQueryChange -> throw IllegalArgumentException("OnMentionSearchQueryChange event cannot be converted")
        is NotesEditEvent.InsertMention -> throw IllegalArgumentException("InsertMention event cannot be converted")
        is NotesEditEvent.CloseMentionPopup -> throw IllegalArgumentException("CloseMentionPopup event cannot be converted")
        is NotesEditEvent.CreateNoteFromSharedText -> throw IllegalArgumentException("CreateNoteFromSharedText event cannot be converted")
        is NotesEditEvent.ImportImage -> throw IllegalArgumentException("ImportImage event cannot be converted")
    }
}

fun NotesListEvent.toProjectNotesEvent(): ProjectNotesEvent {
    return when (this) {
        is NotesListEvent.DeleteNote -> ProjectNotesEvent.DeleteNote(this.note)
        is NotesListEvent.RestoreNote -> ProjectNotesEvent.RestoreNote
        is NotesListEvent.ToggleNoteSelection -> ProjectNotesEvent.ToggleNoteSelection(this.noteId)
        is NotesListEvent.ClearSelection -> ProjectNotesEvent.ClearSelection
        is NotesListEvent.TogglePinForSelectedNotes -> ProjectNotesEvent.TogglePinForSelectedNotes
        is NotesListEvent.DeleteSelectedNotes -> ProjectNotesEvent.DeleteSelectedNotes
        is NotesListEvent.SelectAllNotes -> ProjectNotesEvent.SelectAllNotes
        is NotesListEvent.ArchiveSelectedNotes -> ProjectNotesEvent.ArchiveSelectedNotes
        is NotesListEvent.ChangeColorForSelectedNotes -> ProjectNotesEvent.ChangeColorForSelectedNotes(this.color)
        is NotesListEvent.CopySelectedNotes -> ProjectNotesEvent.CopySelectedNotes
        is NotesListEvent.SendSelectedNotes -> ProjectNotesEvent.SendSelectedNotes
        is NotesListEvent.SetReminderForSelectedNotes -> ProjectNotesEvent.SetReminderForSelectedNotes(this.date, this.time, this.repeatOption)
        is NotesListEvent.ToggleImportantForSelectedNotes -> ProjectNotesEvent.ToggleImportantForSelectedNotes
        is NotesListEvent.SetLabelForSelectedNotes -> ProjectNotesEvent.SetLabelForSelectedNotes(this.label)
        is NotesListEvent.ToggleLayout -> ProjectNotesEvent.ToggleLayout
        is NotesListEvent.SortNotes -> ProjectNotesEvent.SortNotes(this.sortType)
        is NotesListEvent.ToggleLockForSelectedNotes -> ProjectNotesEvent.ToggleLockForSelectedNotes
        else -> throw IllegalArgumentException("This NotesListEvent cannot be converted to ProjectNotesEvent")
    }
}

fun ProjectNotesUiEvent.toNotesUiEvent(): NotesUiEvent {
    return when (this) {
        is ProjectNotesUiEvent.SendNotes -> NotesUiEvent.SendNotes(this.title, this.content)
        is ProjectNotesUiEvent.ShowToast -> NotesUiEvent.ShowToast(this.message)
        is ProjectNotesUiEvent.LinkPreviewRemoved -> NotesUiEvent.LinkPreviewRemoved
        is ProjectNotesUiEvent.NavigateToNoteByTitle -> NotesUiEvent.NavigateToNoteByTitle(this.title)
    }
}
