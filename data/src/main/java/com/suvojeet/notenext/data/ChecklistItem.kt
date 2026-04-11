package com.suvojeet.notenext.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suvojeet.notemark.core.util.ChecklistItemLike
import java.util.UUID

@Serializable
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class ChecklistItem(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    val noteId: Int = 0,
    override val text: String,
    override val isChecked: Boolean,
    override val position: Int = 0,
    override val level: Int = 0,
    val iv: String? = null,
    val isEncrypted: Boolean = false
) : ChecklistItemLike<ChecklistItem> {
    override fun copyWith(text: String, isChecked: Boolean, position: Int, level: Int): ChecklistItem {
        return copy(text = text, isChecked = isChecked, position = position, level = level)
    }
}