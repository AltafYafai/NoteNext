package com.suvojeet.notenext.ui.notes

import android.net.Uri
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.NoteVersion

sealed class NotesEditEvent {
    data class ExpandNote(
        val noteId: Int, 
        val noteType: String = "TEXT",
        val authenticatedCipherTitle: javax.crypto.Cipher? = null,
        val authenticatedCipherContent: javax.crypto.Cipher? = null
    ) : NotesEditEvent()
    object CollapseNote : NotesEditEvent()

    // Checklist Events
    data class OnChecklistItemCheckedChange(val itemId: String, val isChecked: Boolean) : NotesEditEvent()
    data class OnChecklistItemTextChange(val itemId: String, val text: String) : NotesEditEvent()
    data class OnChecklistItemValueChange(val itemId: String, val value: TextFieldValue) : NotesEditEvent()
    data class OnChecklistItemFocus(val itemId: String) : NotesEditEvent()
    data class SwapChecklistItems(val fromId: String, val toId: String) : NotesEditEvent()
    object AddChecklistItem : NotesEditEvent()
    data class DeleteChecklistItem(val itemId: String) : NotesEditEvent()
    data class IndentChecklistItem(val itemId: String) : NotesEditEvent()
    data class OutdentChecklistItem(val itemId: String) : NotesEditEvent()

    data class OnTitleChange(val title: String) : NotesEditEvent()
    data class OnContentChange(val content: TextFieldValue) : NotesEditEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : NotesEditEvent()
    data class ApplyHeadingStyle(val level: Int) : NotesEditEvent()
    data class OnColorChange(val color: Int) : NotesEditEvent()
    object OnSaveNoteClick : NotesEditEvent()
    object OnDeleteNoteClick : NotesEditEvent()
    object OnTogglePinClick : NotesEditEvent()
    object OnToggleLockClick : NotesEditEvent()
    object OnToggleArchiveClick : NotesEditEvent()
    object OnUndoClick : NotesEditEvent()
    object OnRedoClick : NotesEditEvent()
    object OnCopyCurrentNoteClick : NotesEditEvent()
    object OnAddLabelsToCurrentNoteClick : NotesEditEvent()
    data class OnLabelChange(val label: String) : NotesEditEvent()
    object DismissLabelDialog : NotesEditEvent()
    data class OnReminderChange(val time: Long?, val repeatOption: String?) : NotesEditEvent()

    data class OnLinkDetected(val url: String) : NotesEditEvent()
    data class OnLinkPreviewFetched(val url: String, val title: String?, val description: String?, val imageUrl: String?) : NotesEditEvent()
    data class OnRemoveLinkPreview(val url: String) : NotesEditEvent()
    data class OnInsertLink(val url: String) : NotesEditEvent()
    object ClearNewlyAddedChecklistItemId : NotesEditEvent()
    data class AddAttachment(val uri: String, val mimeType: String) : NotesEditEvent()
    data class RemoveAttachment(val tempId: String) : NotesEditEvent()
    data class CreateNoteFromSharedText(val text: String) : NotesEditEvent()
    data class SetInitialTitle(val title: String) : NotesEditEvent()

    data class OnRestoreVersion(val version: NoteVersion) : NotesEditEvent()
    data class NavigateToNoteByTitle(val title: String) : NotesEditEvent()
    object OnToggleNoteType : NotesEditEvent()
    object DeleteAllCheckedItems : NotesEditEvent()
    object ToggleCheckedItemsExpanded : NotesEditEvent()
    object SummarizeNote : NotesEditEvent()
    data class GenerateChecklist(val topic: String) : NotesEditEvent()
    data class InsertGeneratedChecklist(val items: List<String>) : NotesEditEvent()
    object ClearGeneratedChecklist : NotesEditEvent()
    object ClearSummary : NotesEditEvent()
    object FixGrammar : NotesEditEvent()
    object ApplyGrammarFix : NotesEditEvent()
    object ClearGrammarFix : NotesEditEvent()
    data class ImportImage(val uri: Uri) : NotesEditEvent()
    data class ExportNote(val uri: Uri, val format: String) : NotesEditEvent()
    object AutoSaveNote : NotesEditEvent()

    // Mention events
    data class OnMentionSearchQueryChange(val query: String) : NotesEditEvent()
    data class InsertMention(val noteId: Int, val noteTitle: String) : NotesEditEvent()
    object CloseMentionPopup : NotesEditEvent()
}
