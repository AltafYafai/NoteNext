package com.suvojeet.notenext.ui.bin

import com.suvojeet.notenext.data.NoteSummaryWithAttachments

data class BinState(
    val notes: List<NoteSummaryWithAttachments> = emptyList(),
    val selectedNoteIds: List<Int> = emptyList(),
    val expandedNoteId: Int? = null,
    val autoDeleteDays: Int = 7
)
