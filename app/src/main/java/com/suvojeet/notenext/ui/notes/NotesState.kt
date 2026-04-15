package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.SortType

import com.suvojeet.notenext.core.model.NoteType

data class NotesListState(
    val notes: List<NoteSummaryWithAttachments> = emptyList(),
    val pinnedNotes: List<NoteSummaryWithAttachments> = emptyList(),
    val pagedNotes: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<NoteSummaryWithAttachments>> = kotlinx.coroutines.flow.emptyFlow(),
    val layoutType: LayoutType = LayoutType.GRID,
    val sortType: SortType = SortType.DATE_MODIFIED,
    val selectedNoteIds: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val filteredLabel: String? = null,
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val searchQuery: String = "",
    val filteredProjectId: Int? = null
)

data class NotesEditState(
    val labels: List<String> = emptyList(),
    val expandedNoteId: Int? = null,
    val editingTitle: String = "",
    val editingContent: TextFieldValue = TextFieldValue(),
    val editingColor: Int = -1,
    val editingIsNewNote: Boolean = true,
    val editingLastEdited: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val editingLabel: String? = null,
    val editingProjectId: Int? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val isUnderlineActive: Boolean = false,
    val activeHeadingStyle: Int = 0,
    val activeStyles: Set<androidx.compose.ui.text.SpanStyle> = emptySet(),
    val linkPreviews: List<LinkPreview> = emptyList(),
    val editingNoteType: NoteType = NoteType.TEXT,
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
    val mentionableNotes: List<NoteSummaryWithAttachments> = emptyList(),

    // External file & Search in note
    val externalUri: android.net.Uri? = null,
    val isSearchingInNote: Boolean = false,
    val noteSearchQuery: String = "",
    val searchResultIndices: List<Int> = emptyList(),
    val currentSearchResultIndex: Int = -1
)

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    ERROR
}
