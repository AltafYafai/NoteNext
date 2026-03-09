package com.suvojeet.notenext.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class Converters {
    @TypeConverter
    fun fromLinkPreviewList(value: List<LinkPreview>): String {
        return Json.encodeToString(ListSerializer(LinkPreview.serializer()), value)
    }

    @TypeConverter
    fun toLinkPreviewList(value: String): List<LinkPreview> {
        return try {
            Json.decodeFromString(ListSerializer(LinkPreview.serializer()), value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
