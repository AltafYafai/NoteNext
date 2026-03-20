package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.suvojeet.notenext.util.CryptoUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val db: NoteDatabase,
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val projectDao: ProjectDao,
    private val checklistItemDao: ChecklistItemDao
) : NoteRepository {

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction {
            block()
        }
    }

    override fun getNotes(searchQuery: String, sortType: SortType): Flow<List<NoteWithAttachments>> {
        val flow = if (searchQuery.isBlank()) {
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.getNotesOrderedByDateModified()
                SortType.DATE_CREATED -> noteDao.getNotesOrderedByDateCreated()
                SortType.TITLE -> noteDao.getNotesOrderedByTitle()
                SortType.CUSTOM -> noteDao.getNotesOrderedByPosition()
            }
        } else {
            val formattedQuery = "$searchQuery*"
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.searchNotesOrderedByDateModified(formattedQuery)
                SortType.DATE_CREATED -> noteDao.searchNotesOrderedByDateCreated(formattedQuery)
                SortType.TITLE -> noteDao.searchNotesOrderedByTitle(formattedQuery)
                SortType.CUSTOM -> noteDao.searchNotesOrderedByPosition(formattedQuery)
            }
        }
        return flow
    }

    override fun getPinnedNotes(): Flow<List<NoteWithAttachments>> = noteDao.getPinnedNotes()

    override fun getOtherNotesPaged(searchQuery: String, sortType: SortType): Flow<PagingData<NoteWithAttachments>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = true),
            pagingSourceFactory = {
                if (searchQuery.isBlank()) {
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.getOtherNotesPagedOrderedByDateModified()
                        SortType.DATE_CREATED -> noteDao.getOtherNotesPagedOrderedByDateCreated()
                        SortType.TITLE -> noteDao.getOtherNotesPagedOrderedByTitle()
                        SortType.CUSTOM -> noteDao.getOtherNotesPagedOrderedByPosition()
                    }
                } else {
                    val formattedQuery = "$searchQuery*"
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.searchOtherNotesPagedOrderedByDateModified(formattedQuery)
                        SortType.DATE_CREATED -> noteDao.searchOtherNotesPagedOrderedByDateCreated(formattedQuery)
                        SortType.TITLE -> noteDao.searchOtherNotesPagedOrderedByTitle(formattedQuery)
                        SortType.CUSTOM -> noteDao.searchOtherNotesPagedOrderedByPosition(formattedQuery)
                    }
                }
            }
        ).flow
    }

    override fun getArchivedNotes(): Flow<List<NoteWithAttachments>> = 
        noteDao.getArchivedNotes()

    override fun getBinnedNotes(): Flow<List<NoteWithAttachments>> = 
        noteDao.getBinnedNotes()

    override fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>> = 
        noteDao.getNotesByProjectId(projectId)

    override suspend fun getNoteById(id: Int): NoteWithAttachments? = 
        noteDao.getNoteById(id)?.let { 
            // We return it AS IS if locked, so the caller can trigger biometric auth
            if (it.note.isLocked) it else it.copy(note = CryptoUtils.decryptNote(it.note)) 
        }

    override suspend fun insertNote(note: Note): Long {
        val noteToInsert = if (note.isLocked) CryptoUtils.encryptNote(note) else note
        return noteDao.insertNote(noteToInsert)
    }

    override suspend fun updateNote(note: Note) {
        val noteToUpdate = if (note.isLocked) CryptoUtils.encryptNote(note) else note
        noteDao.updateNote(noteToUpdate)
    }

    override suspend fun updateNotePosition(id: Int, position: Int) = noteDao.updateNotePosition(id, position)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun emptyBin() = noteDao.emptyBin()

    override suspend fun insertAttachment(attachment: Attachment) = noteDao.insertAttachment(attachment)

    override suspend fun deleteAttachment(attachment: Attachment) = noteDao.deleteAttachment(attachment)

    override suspend fun deleteAttachmentById(attachmentId: Int) = noteDao.deleteAttachmentById(attachmentId)

    override fun getLabels(): Flow<List<Label>> = labelDao.getLabels()

    override fun getLabelsWithParent(parentName: String): Flow<List<Label>> = labelDao.getLabelsWithParent(parentName)

    override fun getRootLabels(): Flow<List<Label>> = labelDao.getRootLabels()

    override suspend fun insertLabel(label: Label) = labelDao.insertLabel(label)

    override suspend fun updateLabel(label: Label) = labelDao.updateLabel(label)

    override suspend fun deleteLabel(label: Label) = labelDao.deleteLabel(label)

    override suspend fun updateLabelName(oldName: String, newName: String) = 
        noteDao.updateLabelName(oldName, newName)

    override suspend fun removeLabelFromNotes(labelName: String) = 
        noteDao.removeLabelFromNotes(labelName)

    override fun getProjects(): Flow<List<Project>> = projectDao.getProjects()

    override suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    override suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    override suspend fun deleteProject(projectId: Int) = projectDao.deleteProject(projectId)

    override suspend fun getProjectById(projectId: Int): Project? = projectDao.getProjectById(projectId)

    override fun getNotesWithReminders(currentTime: Long): Flow<List<Note>> = 
        noteDao.getNotesWithReminders(currentTime)

    override fun getAllReminders(): Flow<List<Note>> = noteDao.getAllReminders()

    override suspend fun insertChecklistItems(items: List<ChecklistItem>) = checklistItemDao.insertChecklistItems(items)

    override suspend fun updateChecklistItem(item: ChecklistItem) = checklistItemDao.updateChecklistItem(item)

    override suspend fun updateChecklistItems(items: List<ChecklistItem>) = checklistItemDao.updateChecklistItems(items)

    override suspend fun deleteChecklistItem(item: ChecklistItem) = checklistItemDao.deleteChecklistItem(item)

    override suspend fun deleteChecklistForNote(noteId: Int) = checklistItemDao.deleteChecklistForNote(noteId)

    override suspend fun insertNoteVersion(version: NoteVersion) {
        val note = noteDao.getNoteById(version.noteId)
        val isLocked = note?.note?.isLocked == true
        val versionToInsert = if (isLocked) CryptoUtils.encryptNoteVersion(version, isLocked) else version
        noteDao.insertNoteVersion(versionToInsert)
    }

    override fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>> {
        return noteDao.getNoteVersions(noteId)
    }

    override suspend fun limitNoteVersions(noteId: Int, limit: Int) = noteDao.limitNoteVersions(noteId, limit)

    override suspend fun getNoteIdByTitle(title: String): Int? = noteDao.getNoteIdByTitle(title)
}
