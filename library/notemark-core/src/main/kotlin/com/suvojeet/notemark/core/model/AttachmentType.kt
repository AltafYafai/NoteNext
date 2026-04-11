package com.suvojeet.notemark.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the type of an attachment in the application.
 */
@Serializable
enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}
