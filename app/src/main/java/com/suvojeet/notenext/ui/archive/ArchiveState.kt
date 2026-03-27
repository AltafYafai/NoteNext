package com.suvojeet.notenext.ui.archive

import com.suvojeet.notenext.data.NoteSummaryWithAttachments

data class ArchiveState(
    val notes: List<NoteSummaryWithAttachments> = emptyList()
)
