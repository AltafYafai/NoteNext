package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.suvojeet.notenext.util.CryptoUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val projectDao: ProjectDao,
    private val checklistItemDao: ChecklistItemDao
) : NoteRepository {

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
        return flow.map { list -> list.map { it.copy(note = CryptoUtils.decryptNote(it.note)) } }
    }

    override fun getArchivedNotes(): Flow<List<NoteWithAttachments>> = 
        noteDao.getArchivedNotes().map { list -> list.map { it.copy(note = CryptoUtils.decryptNote(it.note)) } }

    override fun getBinnedNotes(): Flow<List<NoteWithAttachments>> = 
        noteDao.getBinnedNotes().map { list -> list.map { it.copy(note = CryptoUtils.decryptNote(it.note)) } }

    override fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>> = 
        noteDao.getNotesByProjectId(projectId).map { list -> list.map { it.copy(note = CryptoUtils.decryptNote(it.note)) } }

    override suspend fun getNoteById(id: Int): NoteWithAttachments? = 
        noteDao.getNoteById(id)?.let { it.copy(note = CryptoUtils.decryptNote(it.note)) }

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
        val versionToInsert = if (note?.note?.isLocked == true) CryptoUtils.encryptNoteVersion(version) else version
        noteDao.insertNoteVersion(versionToInsert)
    }

    override fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>> = 
        noteDao.getNoteVersions(noteId).map { list -> list.map { CryptoUtils.decryptNoteVersion(it) } }

    override suspend fun limitNoteVersions(noteId: Int, limit: Int) = noteDao.limitNoteVersions(noteId, limit)

    override suspend fun getNoteIdByTitle(title: String): Int? = noteDao.getNoteIdByTitle(title)
}
