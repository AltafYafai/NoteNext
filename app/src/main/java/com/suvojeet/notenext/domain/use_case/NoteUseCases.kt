package com.suvojeet.notenext.domain.use_case

import javax.inject.Inject

data class NoteUseCases @Inject constructor(
    val getNotes: GetNotesUseCase,
    val deleteNote: DeleteNoteUseCase,
    val addNote: AddNoteUseCase,
    val getNote: GetNoteUseCase
) {
    fun getPinnedNotes() = getNotes.getPinnedNotes()
    fun getOtherNotesPaged(query: String = "", sortType: com.suvojeet.notenext.data.SortType = com.suvojeet.notenext.data.SortType.DATE_MODIFIED) = 
        getNotes.getOtherNotesPaged(query, sortType)
}
