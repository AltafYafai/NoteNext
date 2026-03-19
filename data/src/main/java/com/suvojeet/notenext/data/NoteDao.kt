package com.suvojeet.notenext.data

import androidx.room.*
import androidx.paging.PagingSource
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteWithAttachments
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastEdited DESC")
    fun getArchivedNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isBinned = 1 ORDER BY lastEdited DESC")
    fun getBinnedNotes(): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteWithAttachments?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttachment(attachment: Attachment)

    @Update
    suspend fun updateNote(note: Note)

    @Transaction
    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET label = :newName WHERE label = :oldName")
    suspend fun updateLabelName(oldName: String, newName: String)

    @Query("UPDATE notes SET label = NULL WHERE label = :labelName")
    suspend fun removeLabelFromNotes(labelName: String)

    @Query("DELETE FROM notes WHERE isBinned = 1")
    suspend fun emptyBin()

    @Query("DELETE FROM notes WHERE isBinned = 1 AND binnedOn IS NOT NULL AND binnedOn < :threshold")
    suspend fun deleteBinnedNotesOlderThan(threshold: Long)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: Int)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Int)

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    fun getNotesWithReminders(currentTime: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL ORDER BY reminderTime DESC")
    fun getAllReminders(): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM notes WHERE projectId = :projectId AND isBinned = 0 ORDER BY isPinned DESC, lastEdited DESC")
    fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0
        ORDER BY notes.isPinned DESC, notes.lastEdited DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteWithAttachments>>

    // Optimized Queries for NotesViewModel

    // 1. DATE_MODIFIED
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND isPinned = 1 ORDER BY lastEdited DESC")
    fun getPinnedNotes(): Flow<List<NoteWithAttachments>>

    // Paging Queries for "Others" (Non-pinned)

    // 1. DATE_MODIFIED
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND isPinned = 0 AND projectId IS NULL ORDER BY lastEdited DESC")
    fun getOtherNotesPagedOrderedByDateModified(): PagingSource<Int, NoteWithAttachments>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND isPinned = 0 AND projectId IS NULL
        ORDER BY notes.lastEdited DESC
    """)
    fun searchOtherNotesPagedOrderedByDateModified(query: String): PagingSource<Int, NoteWithAttachments>

    // 2. DATE_CREATED
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND isPinned = 0 AND projectId IS NULL ORDER BY createdAt DESC")
    fun getOtherNotesPagedOrderedByDateCreated(): PagingSource<Int, NoteWithAttachments>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND isPinned = 0 AND projectId IS NULL
        ORDER BY notes.createdAt DESC
    """)
    fun searchOtherNotesPagedOrderedByDateCreated(query: String): PagingSource<Int, NoteWithAttachments>

    // 3. TITLE
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND isPinned = 0 AND projectId IS NULL ORDER BY title ASC")
    fun getOtherNotesPagedOrderedByTitle(): PagingSource<Int, NoteWithAttachments>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND isPinned = 0 AND projectId IS NULL
        ORDER BY notes.title ASC
    """)
    fun searchOtherNotesPagedOrderedByTitle(query: String): PagingSource<Int, NoteWithAttachments>

    // 4. CUSTOM (Position)
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isBinned = 0 AND isPinned = 0 AND projectId IS NULL ORDER BY position ASC")
    fun getOtherNotesPagedOrderedByPosition(): PagingSource<Int, NoteWithAttachments>

    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        AND notes.isArchived = 0 AND notes.isBinned = 0 AND isPinned = 0 AND projectId IS NULL
        ORDER BY notes.position ASC
    """)
    fun searchOtherNotesPagedOrderedByPosition(query: String): PagingSource<Int, NoteWithAttachments>

    @Query("UPDATE notes SET position = :position WHERE id = :id")
    suspend fun updateNotePosition(id: Int, position: Int)

    // Note Versioning
    @Insert
    suspend fun insertNoteVersion(version: NoteVersion)

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>>

    @Query("DELETE FROM note_versions WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun limitNoteVersions(noteId: Int, limit: Int)

    @Query("SELECT id FROM notes WHERE title = :title AND isBinned = 0 LIMIT 1")
    suspend fun getNoteIdByTitle(title: String): Int?
}
