package com.suvojeet.notemark.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the type of a note in the application.
 */
@Serializable
enum class NoteType {
    TEXT,
    CHECKLIST,
    MARKDOWN
}
