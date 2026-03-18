package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.SortType

data class NotesState(
    val notes: List<NoteWithAttachments> = emptyList(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val searchQuery: String = "",

    val expandedNoteId: Int? = null,
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = -1,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int? = null,
    val activeStyles: Set<String> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: String = "TEXT",
    val editingChecklist: List<ChecklistItem> = emptyList(),
    val checklistInputValues: Map<String, TextFieldValue> = emptyMap(),
    val focusedChecklistItemId: String? = null,
    val isCheckedItemsExpanded: Boolean = true,
    val newlyAddedChecklistItemId: String? = null,
    val editingAttachments: List<Attachment> = emptyList(),
    val editingIsLocked: Boolean = false,
    val editingNoteVersions: List<NoteVersion> = emptyList(),
    val editingReminderTime: Long? = null,
    val editingRepeatOption: String? = null,
    val isSummarizing: Boolean = false,
    val summaryResult: String? = null,
    val showSummaryDialog: Boolean = false,
    val showLabelDialog: Boolean = false,
    val isGeneratingChecklist: Boolean = false,
    val generatedChecklistPreview: List<String> = emptyList(),
    val isFixingGrammar: Boolean = false,
    val fixedContentPreview: String? = null,
    val originalContentBackup: TextFieldValue? = null,
    val saveStatus: SaveStatus = SaveStatus.SAVED,

    // Mention state
    val isMentionPopupVisible: Boolean = false,
    val mentionSearchQuery: String = "",
    val mentionableNotes: List<NoteWithAttachments> = emptyList()
)

data class NotesListState(
    val notes: List<NoteWithAttachments> = emptyList(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val searchQuery: String = ""
)

data class NotesEditState(
    val expandedNoteId: Int? = null,
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = -1,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int? = null,
    val activeStyles: Set<String> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: String = "TEXT",
    val editingChecklist: List<ChecklistItem> = emptyList(),
    val checklistInputValues: Map<String, TextFieldValue> = emptyMap(),
    val focusedChecklistItemId: String? = null,
    val isCheckedItemsExpanded: Boolean = true,
    val newlyAddedChecklistItemId: String? = null,
    val editingAttachments: List<Attachment> = emptyList(),
    val editingIsLocked: Boolean = false,
    val editingNoteVersions: List<NoteVersion> = emptyList(),
    val editingReminderTime: Long? = null,
    val editingRepeatOption: String? = null,
    val isSummarizing: Boolean = false,
    val summaryResult: String? = null,
    val showSummaryDialog: Boolean = false,
    val showLabelDialog: Boolean = false,
    val isGeneratingChecklist: Boolean = false,
    val generatedChecklistPreview: List<String> = emptyList(),
    val isFixingGrammar: Boolean = false,
    val fixedContentPreview: String? = null,
    val originalContentBackup: TextFieldValue? = null,
    val saveStatus: SaveStatus = SaveStatus.SAVED,

    // Mention state
    val isMentionPopupVisible: Boolean = false,
    val mentionSearchQuery: String = "",
    val mentionableNotes: List<NoteWithAttachments> = emptyList()
)

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    ERROR
}
