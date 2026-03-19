package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.Fts5

@Entity(tableName = "notes_fts")
@Fts5(contentEntity = Note::class)
data class NoteFts(
    val title: String,
    val content: String,
    val label: String?
)
