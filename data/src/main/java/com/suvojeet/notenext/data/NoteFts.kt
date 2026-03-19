package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.Fts5
import com.suvojeet.notenext.data.Note

@Entity(tableName = "notes_fts")
@Fts5(contentEntity = Note::class)
data class NoteFts(
    val title: String,
    val content: String,
    val label: String?
)
