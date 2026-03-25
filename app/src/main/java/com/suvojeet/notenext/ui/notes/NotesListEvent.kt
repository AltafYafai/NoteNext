package com.suvojeet.notenext.ui.notes

import com.suvojeet.notenext.data.SortType
import com.suvojeet.notenext.data.NoteWithAttachments

sealed class NotesListEvent {
    data class DeleteNote(val note: NoteWithAttachments) : NotesListEvent()
    object RestoreNote : NotesListEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesListEvent()
    object ClearSelection : NotesListEvent()
    object SelectAllNotes : NotesListEvent()
    object TogglePinForSelectedNotes : NotesListEvent()
    object ToggleLockForSelectedNotes : NotesListEvent()
    object DeleteSelectedNotes : NotesListEvent()
    object ArchiveSelectedNotes : NotesListEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : NotesListEvent()
    object CopySelectedNotes : NotesListEvent()
    object SendSelectedNotes : NotesListEvent()
    data class SetReminderForSelectedNotes(val date: java.time.LocalDate, val time: java.time.LocalTime, val repeatOption: com.suvojeet.notenext.data.RepeatOption) : NotesListEvent()
    object ToggleImportantForSelectedNotes : NotesListEvent()
    data class SetLabelForSelectedNotes(val label: String) : NotesListEvent()
    data class FilterByLabel(val label: String?) : NotesListEvent()
    data class FilterByProject(val projectId: Int?) : NotesListEvent()
    object ToggleLayout : NotesListEvent()
    data class SortNotes(val sortType: SortType) : NotesListEvent()
    data class CreateProject(val name: String) : NotesListEvent()
    data class MoveSelectedNotesToProject(val projectId: Int?) : NotesListEvent()
    data class OnSearchQueryChange(val query: String) : NotesListEvent()
}
