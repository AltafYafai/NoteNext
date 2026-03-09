package com.suvojeet.notenext.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromLinkPreviewList(value: List<LinkPreview>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toLinkPreviewList(value: String): List<LinkPreview> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
