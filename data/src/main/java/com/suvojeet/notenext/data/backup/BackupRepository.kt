package com.suvojeet.notenext.data.backup

import android.content.Context
import android.net.Uri
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.NoteWithAttachments
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async


@Singleton
class BackupRepository @Inject constructor(
    private val repository: NoteRepository,
    @ApplicationContext private val context: Context,
    private val googleDriveManager: GoogleDriveManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun createBackupZip(targetFile: File, includeAttachments: Boolean = true) {
        FileOutputStream(targetFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                writeBackupToZip(zos, includeAttachments)
            }
        }
    }
    
    suspend fun createBackupZip(outputStream: java.io.OutputStream, includeAttachments: Boolean = true) {
         ZipOutputStream(outputStream).use { zos ->
            writeBackupToZip(zos, includeAttachments)
        }
    }

    suspend fun backupToUri(folderUri: Uri, includeAttachments: Boolean = true): String {
        return try {
            val validUri = if (folderUri.toString().endsWith("%3A")) {
                 folderUri
            } else folderUri

            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, validUri)
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                 throw Exception("Cannot write to selected folder. Please select a valid directory.")
            }

            val fileName = "NoteNext_Backup_${System.currentTimeMillis()}.zip"
            val file = dir.createFile("application/zip", fileName) 
                ?: throw Exception("Failed to create file in selected directory.")

            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                createBackupZip(outputStream, includeAttachments)
            }
            "Backup successful: $fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to save backup: ${e.message}")
        }
    }

    suspend fun backupToEncryptedFolder(folderUri: Uri, password: String, includeAttachments: Boolean = true): String {
        return try {
             val validUri = if (folderUri.toString().endsWith("%3A")) {
                 folderUri
            } else folderUri

            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, validUri)
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                 throw Exception("Cannot write to selected folder. Please select a valid directory.")
            }

            val fileName = "NoteNext_Backup_Encrypted_${System.currentTimeMillis()}.enc"
            val file = dir.createFile("application/octet-stream", fileName) 
                ?: throw Exception("Failed to create file in selected directory.")

            backupToEncryptedStream(context.contentResolver.openOutputStream(file.uri), password, includeAttachments)

            "Encrypted Backup successful: $fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to save encrypted backup: ${e.message}")
        }
    }

    suspend fun backupToEncryptedStream(outputStream: java.io.OutputStream?, password: String, includeAttachments: Boolean = true) {
        if (outputStream == null) throw Exception("Output stream is null")
        
        // Use Piped streams to avoid writing plain-text temp files to disk for better security
        val pipedInputStream = java.io.PipedInputStream()
        val pipedOutputStream = java.io.PipedOutputStream(pipedInputStream)
        
        coroutineScope {
            // Launch zip writing in a separate coroutine
            val zipJob = launch(Dispatchers.IO) {
                try {
                    createBackupZip(pipedOutputStream, includeAttachments)
                } finally {
                    pipedOutputStream.close()
                }
            }
            
            // Encrypt and write to outputStream in the current coroutine
            try {
                outputStream.use { out ->
                    EncryptionUtils.encryptStream(pipedInputStream, out, password)
                }
            } finally {
                zipJob.join()
            }
        }
    }

    fun checkIsEncrypted(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                EncryptionUtils.isEncrypted(inputStream)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    // Helper to decrypt and provide a temp file (caller must delete)
    suspend fun decryptBackupToTempFile(uri: Uri, password: String): File {
        val tempZipFile = File(context.cacheDir, "temp_decrypted_restore_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                EncryptionUtils.decryptFile(inputStream, tempZipFile, password)
            }
            return tempZipFile
        } catch (e: Exception) {
            if (tempZipFile.exists()) tempZipFile.delete()
            throw Exception("Decryption failed. Incorrect password?")
        }
    }

    suspend fun createEncryptedBackupZip(targetFile: File, password: String, includeAttachments: Boolean = true) {
        FileOutputStream(targetFile).use { fos ->
            backupToEncryptedStream(fos, password, includeAttachments)
        }
    }

    suspend fun backupToDrive(
        account: GoogleSignInAccount, 
        password: String? = null,
        includeAttachments: Boolean = true, 
        onProgress: ((Long, Long) -> Unit)? = null
    ): String {
        val dbFile = File(context.cacheDir, "temp_backup.zip") // Google Drive SDK uses this file to upload
        try {
            if (password.isNullOrBlank()) {
                createBackupZip(dbFile, includeAttachments)
            } else {
                createEncryptedBackupZip(dbFile, password, includeAttachments)
            }
            return googleDriveManager.uploadBackup(context, account, dbFile, onProgress)
        } finally {
            if (dbFile.exists()) {
                dbFile.delete()
            }
        }
    }

    private suspend fun writeBackupToZip(zos: ZipOutputStream, includeAttachments: Boolean) {
        // Backup notes
        val notes = repository.getNotes().first()
        val notesJson = json.encodeToString(ListSerializer(NoteWithAttachments.serializer()), notes)
        zos.putNextEntry(ZipEntry("notes.json"))
        zos.write(notesJson.toByteArray())
        zos.closeEntry()

        // Backup labels
        val labels = repository.getLabels().first()
        val labelsJson = json.encodeToString(ListSerializer(Label.serializer()), labels)
        zos.putNextEntry(ZipEntry("labels.json"))
        zos.write(labelsJson.toByteArray())
        zos.closeEntry()

        // Backup projects
        val projects = repository.getProjects().first()
        val projectsJson = json.encodeToString(ListSerializer(Project.serializer()), projects)
        zos.putNextEntry(ZipEntry("projects.json"))
        zos.write(projectsJson.toByteArray())
        zos.closeEntry()

        // Backup attachments
        if (includeAttachments) {
            val attachments = notes.flatMap { it.attachments }
            attachments.forEach { attachment ->
                try {
                    val attachmentUri = Uri.parse(attachment.uri)
                    context.contentResolver.openInputStream(attachmentUri)?.use { inputStream ->
                        // Safely get filename using DocumentFile
                        val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, attachmentUri)
                        val fileName = documentFile?.name ?: File(attachmentUri.path ?: "attachment_${System.currentTimeMillis()}").name
                        zos.putNextEntry(ZipEntry("attachments/$fileName"))
                        inputStream.copyTo(zos)
                        zos.closeEntry()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    suspend fun readProjectsFromZip(uri: Uri): List<Project> {
        var projects: List<Project> = emptyList()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    if (zipEntry.name == "projects.json") {
                        val projectsJson = InputStreamReader(zis).readText()
                        projects = json.decodeFromString(ListSerializer(Project.serializer()), projectsJson)
                        break
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }
        return projects
    }

    suspend fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var projectsJson: String? = null
        var labelsJson: String? = null 

        // Pass 1: Read JSON Data
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                   when {
                        zipEntry.name == "notes.json" -> notesJson = InputStreamReader(zis).readText()
                        zipEntry.name == "labels.json" -> labelsJson = InputStreamReader(zis).readText()
                        zipEntry.name == "projects.json" -> projectsJson = InputStreamReader(zis).readText()
                    }
                    zipEntry = zis.nextEntry
                }
            }
        }

        // 1. Restore Labels (All)
        labelsJson?.let {
            val labels: List<Label> = json.decodeFromString(ListSerializer(Label.serializer()), it)
            labels.forEach { repository.insertLabel(it) }
        }

        // 2. Restore Selected Projects
        projectsJson?.let {
            val allProjects: List<Project> = json.decodeFromString(ListSerializer(Project.serializer()), it)
            val selectedProjects = allProjects.filter { project -> selectedProjectIds.contains(project.id) }
            
            selectedProjects.forEach { project ->
                val oldId = project.id
                val newId = repository.insertProject(project.copy(id = 0)).toInt()
                oldToNewProjectIds[oldId] = newId
            }
        }

        // 3. Restore Notes & Prepare Attachment Extraction
        val attachmentsToExtract = mutableListOf<Pair<String, File>>() // ZipEntryName -> TargetFile

        notesJson?.let {
            val notesWithAttachments: List<NoteWithAttachments> = json.decodeFromString(ListSerializer(NoteWithAttachments.serializer()), it)
            
            notesWithAttachments.forEach { noteWithAttachments ->
                val oldProjectId = noteWithAttachments.note.projectId
                // Only restore if the note belongs to a selected project
                if (oldToNewProjectIds.containsKey(oldProjectId)) {
                    val newProjectId = oldToNewProjectIds[oldProjectId]!!
                    val newNote = noteWithAttachments.note.copy(id = 0, projectId = newProjectId)
                    val newNoteId = repository.insertNote(newNote).toInt()

                    // Handle Attachments
                    noteWithAttachments.attachments.forEach { attachment ->
                        try {
                            val originalUri = Uri.parse(attachment.uri)
                            // We assume the filename in the zip matches the original filename
                            val fileName = File(originalUri.path ?: "unknown_${System.currentTimeMillis()}").name
                            val zipEntryName = "attachments/$fileName"
                            
                            // Create target file in internal storage
                            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                            val attachmentsDir = File(context.filesDir, "attachments")
                            if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                            
                            val targetFile = File(attachmentsDir, uniqueFileName)
                            
                            val newUri = Uri.fromFile(targetFile).toString()
                            
                            val newAttachment = attachment.copy(id = 0, noteId = newNoteId, uri = newUri)
                            repository.insertAttachment(newAttachment)
                            
                            attachmentsToExtract.add(zipEntryName to targetFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // Pass 2: Extract Attachment Files
        if (attachmentsToExtract.isNotEmpty()) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        val entryName = zipEntry.name
                        // Find all targets that need this entry
                        val targets = attachmentsToExtract.filter { it.first == entryName }
                        targets.forEach { (_, targetFile) ->
                            try {
                                FileOutputStream(targetFile).use { fos ->
                                    // Copy without closing the ZIS
                                    val buffer = ByteArray(8192)
                                    var length: Int
                                    while (zis.read(buffer).also { length = it } > 0) {
                                        fos.write(buffer, 0, length)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        zipEntry = zis.nextEntry
                    }
                }
            }
        }
    }
}
