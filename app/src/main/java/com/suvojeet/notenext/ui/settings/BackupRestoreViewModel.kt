package com.suvojeet.notenext.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.*
import com.suvojeet.notenext.data.backup.GoogleDriveManager
import com.suvojeet.notenext.data.backup.KeepNote
import com.suvojeet.notenext.data.backup.KeepLabel
import com.suvojeet.notenext.data.backup.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class BackupDetails(
    val notesCount: Int,
    val labelsCount: Int,
    val projectsCount: Int,
    val attachmentsCount: Int,
    val totalSize: Long,
    val notesSize: Long,
    val labelsSize: Long,
    val projectsSize: Long,
    val attachmentsSize: Long
)

data class BackupRestoreState(
    val backupDetails: BackupDetails? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupResult: String? = null,
    val restoreResult: String? = null,
    val driveBackupExists: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isDeleting: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val backupFrequency: String = "Daily",
    val foundBackupDetails: com.suvojeet.notenext.data.backup.BackupScanResult? = null,
    val isScanning: Boolean = false,
    val googleAccountEmail: String? = null,
    val uploadProgress: String? = null,
    val driveBackupMetadata: com.suvojeet.notenext.data.backup.GoogleDriveManager.DriveBackupMetadata? = null,
    val isSdCardAutoBackupEnabled: Boolean = false,
    val sdCardFolderUri: String? = null,
    val includeAttachments: Boolean = true,
    val backupVersions: List<com.suvojeet.notenext.data.backup.GoogleDriveManager.DriveBackupMetadata> = emptyList(),
    val isLoadingVersions: Boolean = false,
    val isPasswordRequired: Boolean = false,
    val pendingRestoreUri: String? = null,
    val pendingMerge: Boolean = false,
    val isEncryptionEnabled: Boolean = false,
    val hasPasswordSet: Boolean = false,
    val lastBackupTime: Long = 0L,
    val lastBackupStatus: String? = null
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val backupRepository: com.suvojeet.notenext.data.backup.BackupRepository,
    private val application: Application,
    private val googleDriveManager: GoogleDriveManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreState())
    val state = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    init {
        val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("auto_backup_enabled", false)
        val frequency = sharedPrefs.getString("backup_frequency", "Daily") ?: "Daily"
        val includeAttachments = sharedPrefs.getBoolean("include_backup_attachments", true)
        
        val sdCardEnabled = sharedPrefs.getBoolean("sd_card_backup_enabled", false)
        val sdCardUri = sharedPrefs.getString("sd_card_folder_uri", null)
        
        // Migrate legacy plain-text password to EncryptedSharedPreferences
        var encryptionEnabled = sharedPrefs.getBoolean("backup_encryption_enabled", false)
        val legacyPassword = sharedPrefs.getString("backup_password", null) ?: sharedPrefs.getString("auto_backup_password", null)
        
        if (encryptionEnabled && legacyPassword != null) {
            SecurityUtils.saveBackupPassword(application, legacyPassword)
            sharedPrefs.edit().remove("backup_password").remove("auto_backup_password").apply()
        }
        
        val hasPassword = SecurityUtils.getBackupPassword(application) != null
        
        val lastTime = sharedPrefs.getLong("last_backup_time", 0L)
        val lastStatus = sharedPrefs.getString("last_backup_status", null)

        _state.value = _state.value.copy(
            isAutoBackupEnabled = enabled, 
            backupFrequency = frequency,
            isSdCardAutoBackupEnabled = sdCardEnabled,
            sdCardFolderUri = sdCardUri,
            includeAttachments = includeAttachments,
            isEncryptionEnabled = encryptionEnabled,
            hasPasswordSet = hasPassword,
            lastBackupTime = lastTime,
            lastBackupStatus = lastStatus
        )
    }

    private fun updateLastBackup(status: String) {
        val time = System.currentTimeMillis()
        val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("last_backup_time", time)
            .putString("last_backup_status", status)
            .apply()
        _state.value = _state.value.copy(lastBackupTime = time, lastBackupStatus = status)
    }

    fun setGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        _state.value = _state.value.copy(googleAccountEmail = account?.email)
        if (account != null) {
            checkDriveBackupStatus(account)
            refreshBackupVersions(account)
        } else {
            _state.value = _state.value.copy(driveBackupExists = false, backupVersions = emptyList())
        }
    }

    fun signOut(context: android.content.Context) {
         com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).signOut().addOnCompleteListener {
             _state.value = _state.value.copy(googleAccountEmail = null, driveBackupExists = false, backupVersions = emptyList())
        }
    }

    fun getBackupDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val notes = repository.getNotes().first()
                val labels = repository.getLabels().first()
                val projects = repository.getProjects().first()
                val attachments = notes.flatMap { it.attachments }

                val notesJson = json.encodeToString(ListSerializer(NoteWithAttachments.serializer()), notes)
                val labelsJson = json.encodeToString(ListSerializer(Label.serializer()), labels)
                val projectsJson = json.encodeToString(ListSerializer(Project.serializer()), projects)

                var attachmentsSize = 0L
                attachments.forEach { attachment ->
                    try {
                        val attachmentUri = Uri.parse(attachment.uri)
                        application.contentResolver.openFileDescriptor(attachmentUri, "r")?.use {
                            attachmentsSize += it.statSize
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val notesSize = notesJson.toByteArray().size.toLong()
                val labelsSize = labelsJson.toByteArray().size.toLong()
                val projectsSize = projectsJson.toByteArray().size.toLong()
                val totalSize = notesSize + labelsSize + projectsSize + attachmentsSize

                _state.value = _state.value.copy(
                    backupDetails = BackupDetails(
                        notesCount = notes.size,
                        labelsCount = labels.size,
                        projectsCount = projects.size,
                        attachmentsCount = attachments.size,
                        totalSize = totalSize,
                        notesSize = notesSize,
                        labelsSize = labelsSize,
                        projectsSize = projectsSize,
                        attachmentsSize = attachmentsSize
                    )
                )
            }
        }
    }

    fun setEncryption(password: String) {
        SecurityUtils.saveBackupPassword(application, password)
        val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("backup_encryption_enabled", true)
            putBoolean("auto_backup_encryption_enabled", true) 
            apply()
        }
        _state.value = _state.value.copy(isEncryptionEnabled = true, hasPasswordSet = true)
        refreshWorkerSchedule()
    }

    fun disableEncryption() {
        SecurityUtils.saveBackupPassword(application, null)
        val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("backup_encryption_enabled", false)
            putBoolean("auto_backup_encryption_enabled", false)
            apply()
        }
        _state.value = _state.value.copy(isEncryptionEnabled = false, hasPasswordSet = false)
        refreshWorkerSchedule()
    }

    fun changePassword(newPassword: String) {
        setEncryption(newPassword)
    }

    fun createBackup(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = null)
            withContext(Dispatchers.IO) {
                try {
                    val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val effectivePassword = password ?: if (state.value.isEncryptionEnabled) storedPassword else null

                    if (effectivePassword.isNullOrBlank()) {
                        application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            backupRepository.createBackupZip(outputStream, state.value.includeAttachments)
                        }
                        _state.value = _state.value.copy(isBackingUp = false, backupResult = "Local Backup successful")
                        updateLastBackup("Success (Local)")
                    } else {
                        backupRepository.backupToEncryptedStream(
                            application.contentResolver.openOutputStream(uri), 
                            effectivePassword, 
                            state.value.includeAttachments
                        )
                        _state.value = _state.value.copy(isBackingUp = false, backupResult = "Encrypted Local Backup successful")
                        updateLastBackup("Success (Encrypted Local)")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isBackingUp = false, backupResult = "Local Backup failed: ${e.message}")
                    updateLastBackup("Failed (Local)")
                }
            }
        }
    }

    fun backupToDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
         viewModelScope.launch {
            _state.value = _state.value.copy(
                isBackingUp = true, 
                backupResult = "Uploading to Drive...",
                uploadProgress = "Starting..."
            )
            withContext(Dispatchers.IO) {
                try {
                    val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val password = if (state.value.isEncryptionEnabled) storedPassword else null
                    
                    backupRepository.backupToDrive(account, password, state.value.includeAttachments) { uploaded, total ->
                        val progress = if (total > 0) {
                            val percent = (uploaded * 100) / total
                            val uploadedMb = String.format("%.2f", uploaded / (1024.0 * 1024.0))
                            val totalMb = String.format("%.2f", total / (1024.0 * 1024.0))
                            "$uploadedMb MB / $totalMb MB ($percent%)"
                        } else {
                            "Uploading..."
                        }
                        _state.value = _state.value.copy(uploadProgress = progress)
                    }
                    
                    _state.value = _state.value.copy(
                        isBackingUp = false,
                        backupResult = "Drive Backup successful",
                        driveBackupExists = true,
                        uploadProgress = null
                    )
                    updateLastBackup("Success (Drive)")
                    refreshBackupVersions(account)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(
                        isBackingUp = false, 
                        backupResult = "Drive Backup failed: ${e.message}",
                        uploadProgress = null
                    )
                    updateLastBackup("Failed (Drive)")
                }
            }
        }
    }

    fun restoreBackup(uri: Uri, merge: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
            withContext(Dispatchers.IO) {
                if (backupRepository.checkIsEncrypted(uri)) {
                    _state.value = _state.value.copy(
                        isRestoring = false, 
                        isPasswordRequired = true, 
                        pendingRestoreUri = uri.toString(),
                        pendingMerge = merge
                    )
                    return@withContext
                }
                
                try {
                    application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            readBackupFromZip(zis, merge)
                        }
                    }
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = if (merge) "Merge successful" else "Local Restore successful")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Local Restore failed: ${e.message}")
                }
            }
        }
    }

    fun restoreEncryptedBackup(password: String) {
        val uriString = state.value.pendingRestoreUri ?: return
        val uri = Uri.parse(uriString)
        val merge = state.value.pendingMerge
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = null, isPasswordRequired = false)
            withContext(Dispatchers.IO) {
                try {
                    val tempZipFile = backupRepository.decryptBackupToTempFile(uri, password)
                    try {
                        java.io.FileInputStream(tempZipFile).use { inputStream ->
                            ZipInputStream(inputStream).use { zis ->
                                readBackupFromZip(zis, merge)
                            }
                        }
                        _state.value = _state.value.copy(isRestoring = false, restoreResult = if (merge) "Encrypted Merge successful" else "Encrypted Restore successful", pendingRestoreUri = null, pendingMerge = false)
                    } finally {
                        if (tempZipFile.exists()) tempZipFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(
                        isRestoring = false, 
                        restoreResult = "Restore failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelPasswordEntry() {
        _state.value = _state.value.copy(isPasswordRequired = false, pendingRestoreUri = null)
    }
    
    private suspend fun readBackupFromZip(zis: ZipInputStream, merge: Boolean = false) {
        val oldToNewProjectIds = mutableMapOf<Int, Int>()
        var notesJson: String? = null
        var labelsJson: String? = null
        var projectsJson: String? = null
        var manifestJson: String? = null

        // Pass 1: Read JSON Data from ZIP
        try {
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                when (zipEntry.name) {
                    "notes.json" -> notesJson = InputStreamReader(zis).readText()
                    "labels.json" -> labelsJson = InputStreamReader(zis).readText()
                    "projects.json" -> projectsJson = InputStreamReader(zis).readText()
                    "manifest.json" -> manifestJson = InputStreamReader(zis).readText()
                }
                zipEntry = zis.nextEntry
            }
        } catch (e: Exception) {
            throw Exception("Failed to read backup contents: ${e.message}")
        }

        // Pass 1.5: Verify Checksums
        if (manifestJson != null) {
            val manifest: Map<String, String> = json.decodeFromString(manifestJson)
            val md = java.security.MessageDigest.getInstance("SHA-256")
            
            fun verify(name: String, content: String?) {
                if (content != null && manifest.containsKey(name)) {
                    val hashBytes = md.digest(content.toByteArray())
                    val hashString = hashBytes.joinToString("") { "%02x".format(it) }
                    if (hashString != manifest[name]) {
                        throw Exception("Data corruption detected: $name failed checksum verification.")
                    }
                }
            }
            
            verify("notes.json", notesJson)
            verify("labels.json", labelsJson)
            verify("projects.json", projectsJson)
        }

        // Pass 2: Deserialize into in-memory objects BEFORE deletion
        if (notesJson == null) {
            throw Exception("Invalid backup: Missing notes.json")
        }

        val projectsToRestore = projectsJson?.let {
            json.decodeFromString(ListSerializer(Project.serializer()), it)
        } ?: emptyList()

        val labelsToRestore = labelsJson?.let {
            json.decodeFromString(ListSerializer(Label.serializer()), it)
        } ?: emptyList()

        val notesToRestore = json.decodeFromString(ListSerializer(NoteWithAttachments.serializer()), notesJson)

        repository.runInTransaction {
            if (!merge) {
                // Only delete local data if NOT merging
                repository.getNotes().first().flatMap { it.attachments }.forEach { repository.deleteAttachment(it) }
                repository.getNotes().first().forEach { repository.deleteNote(it.note) }
                repository.getLabels().first().forEach { repository.deleteLabel(it) }
                repository.getProjects().first().forEach { repository.deleteProject(it.id) }
            }

            // Pass 3: Restore Data
            projectsToRestore.forEach { project ->
                val oldId = project.id
                val newId = repository.insertProject(project.copy(id = 0))
                require(newId <= Int.MAX_VALUE) { "Project ID overflow" }
                oldToNewProjectIds[oldId] = newId.toInt()
            }

            labelsToRestore.forEach { 
                try {
                    repository.insertLabel(it) 
                } catch (e: Exception) {
                    // Label might already exist, ignore in merge mode
                }
            }

            notesToRestore.forEach { noteWithAttachments ->
                val oldProjectId = noteWithAttachments.note.projectId
                val newProjectId = if (oldProjectId != null) oldToNewProjectIds[oldProjectId] else null
                val newNote = noteWithAttachments.note.copy(id = 0, projectId = newProjectId)
                val newNoteId = repository.insertNote(newNote)
                require(newNoteId <= Int.MAX_VALUE) { "Note ID overflow" }
                
                if (noteWithAttachments.checklistItems.isNotEmpty()) {
                    val newChecklistItems = noteWithAttachments.checklistItems.map { checklistItem ->
                        checklistItem.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            noteId = newNoteId.toInt()
                        )
                    }
                    repository.insertChecklistItems(newChecklistItems)
                }
            }
        }
    }

    fun importFromGoogleKeep(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = "Importing from Google Keep...")
            withContext(Dispatchers.IO) {
                try {
                    var importedCount = 0
                    application.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zis ->
                            val notesToSave = mutableListOf<KeepNote>()
                            var zipEntry = zis.nextEntry
                            
                            while (zipEntry != null) {
                                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".json")) {
                                    try {
                                        val jsonString = readZipEntryText(zis)
                                        val keepNote: KeepNote = json.decodeFromString(KeepNote.serializer(), jsonString)
                                        if (!keepNote.isTrashed) {
                                            notesToSave.add(keepNote)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                zipEntry = zis.nextEntry
                            }

                            if (notesToSave.isNotEmpty()) {
                                repository.runInTransaction {
                                    notesToSave.forEach { keepNote ->
                                        saveKeepNote(keepNote)
                                        importedCount++
                                    }
                                }
                            }
                        }
                    }
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        restoreResult = "Imported $importedCount notes from Google Keep"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Import failed: ${e.message}")
                }
            }
        }
    }

    private fun readZipEntryText(zis: ZipInputStream): String {
        return zis.readBytes().toString(Charsets.UTF_8)
    }

    private suspend fun saveKeepNote(keepNote: KeepNote) {
        val color = mapKeepColor(keepNote.color)
        val noteType = if (!keepNote.listContent.isNullOrEmpty()) "CHECKLIST" else "TEXT"
        val content = keepNote.textContent ?: ""
        
        val newNote = Note(
            title = keepNote.title ?: "",
            content = content,
            createdAt = keepNote.createdTimestampUsec / 1000,
            lastEdited = keepNote.userEditedTimestampUsec / 1000,
            color = color,
            isPinned = keepNote.isPinned,
            isArchived = keepNote.isArchived,
            noteType = noteType,
            label = keepNote.labels?.firstOrNull()?.name 
        )
        
        val newIdLong = repository.insertNote(newNote)
        require(newIdLong <= Int.MAX_VALUE) { "Note ID overflow" }
        val noteId = newIdLong.toInt()

        val listContent = keepNote.listContent
        if (!listContent.isNullOrEmpty()) {
            val checklistItems = listContent.mapIndexed { index, item ->
                ChecklistItem(
                    noteId = noteId,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = index
                )
            }
            repository.insertChecklistItems(checklistItems)
        }
        
        keepNote.labels?.forEach { keepLabel ->
             try {
                repository.insertLabel(Label(keepLabel.name))
             } catch (e: Exception) {}
        }
    }
    
    private fun mapKeepColor(keepColor: String?): Int {
        return when (keepColor) {
            "RED" -> android.graphics.Color.parseColor("#F28B82")
            "ORANGE" -> android.graphics.Color.parseColor("#FBBC04")
            "YELLOW" -> android.graphics.Color.parseColor("#FFF475")
            "GREEN" -> android.graphics.Color.parseColor("#CCFF90")
            "TEAL" -> android.graphics.Color.parseColor("#A7FFEB")
            "BLUE" -> android.graphics.Color.parseColor("#CBF0F8")
            "DARK_BLUE" -> android.graphics.Color.parseColor("#AECBFA")
            "PURPLE" -> android.graphics.Color.parseColor("#D7AEFB")
            "PINK" -> android.graphics.Color.parseColor("#FDCFE8")
            "BROWN" -> android.graphics.Color.parseColor("#E6C9A8")
            "GRAY" -> android.graphics.Color.parseColor("#E8EAED")
             else -> 0 
        }
    }

    fun restoreFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String? = null, merge: Boolean = false) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isRestoring = true, restoreResult = null)
             val backupName = if (fileId != null) "selected version" else "latest backup"
            withContext(Dispatchers.IO) {
                val tempFile = File(application.cacheDir, "temp_restore.zip")
                var shouldDeleteTempFile = true
                try {
                     googleDriveManager.downloadBackup(application, account, tempFile, fileId)
                     if (backupRepository.checkIsEncrypted(Uri.fromFile(tempFile))) {
                         _state.value = _state.value.copy(
                            isRestoring = false,
                            isPasswordRequired = true,
                            pendingRestoreUri = Uri.fromFile(tempFile).toString(),
                            pendingMerge = merge
                         )
                         shouldDeleteTempFile = false
                         return@withContext
                     }

                     java.io.FileInputStream(tempFile).use { inputStream ->
                         ZipInputStream(inputStream).use { zis ->
                             readBackupFromZip(zis, merge)
                         }
                     }
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = if (merge) "Drive Merge successful" else "Drive Restore ($backupName) successful")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Drive Restore failed: ${e.message}")
                } finally {
                    if (shouldDeleteTempFile && tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            }
        }
    }

    fun checkDriveBackupStatus(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingBackup = true)
            withContext(Dispatchers.IO) {
                try {
                    val metadata = googleDriveManager.getBackupMetadata(application, account)
                    _state.value = _state.value.copy(
                        isCheckingBackup = false,
                        driveBackupExists = metadata != null,
                        driveBackupMetadata = metadata,
                        googleAccountEmail = account.email
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                   _state.value = _state.value.copy(isCheckingBackup = false, driveBackupExists = false, driveBackupMetadata = null)
                }
            }
        }
    }

    fun refreshBackupVersions(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingVersions = true)
            withContext(Dispatchers.IO) {
                try {
                    val versions = googleDriveManager.getBackups(application, account)
                    _state.value = _state.value.copy(isLoadingVersions = false, backupVersions = versions)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isLoadingVersions = false)
                }
            }
        }
    }

    fun deleteBackupVersion(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, fileId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)
            withContext(Dispatchers.IO) {
                try {
                    googleDriveManager.deleteBackupFile(application, account, fileId)
                    refreshBackupVersions(account)
                    checkDriveBackupStatus(account)
                    _state.value = _state.value.copy(isDeleting = false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isDeleting = false, backupResult = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    fun deleteDriveBackup(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
             _state.value = _state.value.copy(isDeleting = true, backupResult = "Deleting Drive Backups...")
            withContext(Dispatchers.IO) {
                try {
                     googleDriveManager.deleteBackup(application, account)
                    _state.value = _state.value.copy(
                        isDeleting = false, 
                        backupResult = "Drive Backups deleted successfully",
                        driveBackupExists = false,
                        backupVersions = emptyList()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isDeleting = false, backupResult = "Failed to delete backup: ${e.message}")
                }
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean, email: String? = null, frequency: String = "Daily") {
        viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
            sharedPrefs.edit().putString("backup_frequency", frequency).apply()
            _state.value = _state.value.copy(isAutoBackupEnabled = enabled, backupFrequency = frequency)
            if (enabled && email != null) {
                scheduleWorker(email, frequency)
            } else {
                cancelWorker()
            }
        }
    }

    fun toggleSdCardAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("sd_card_backup_enabled", enabled).apply()
            _state.value = _state.value.copy(isSdCardAutoBackupEnabled = enabled)
            refreshWorkerSchedule()
        }
    }

    fun toggleIncludeAttachments(enabled: Boolean) {
         viewModelScope.launch {
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("include_backup_attachments", enabled).apply()
            _state.value = _state.value.copy(includeAttachments = enabled)
        }
    }

    fun setSdCardLocation(uri: Uri) {
        viewModelScope.launch {
            val contentResolver = application.contentResolver
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("sd_card_folder_uri", uri.toString()).apply()
            _state.value = _state.value.copy(sdCardFolderUri = uri.toString())
            refreshWorkerSchedule()
        }
    }

    fun backupToSdCard() {
        val uriString = state.value.sdCardFolderUri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBackingUp = true, backupResult = "Backing up to SD Card...")
            withContext(Dispatchers.IO) {
                try {
                    val sharedPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
                    val storedPassword = SecurityUtils.getBackupPassword(application)
                    val result = if (state.value.isEncryptionEnabled && !storedPassword.isNullOrBlank()) {
                         backupRepository.backupToEncryptedFolder(Uri.parse(uriString), storedPassword, state.value.includeAttachments)
                    } else {
                         backupRepository.backupToUri(Uri.parse(uriString), state.value.includeAttachments)
                    }
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = result)
                     updateLastBackup("Success (SD Card)")
                } catch (e: Exception) {
                    e.printStackTrace()
                     _state.value = _state.value.copy(isBackingUp = false, backupResult = "SD Card Backup failed: ${e.message}")
                     updateLastBackup("Failed (SD Card)")
                }
            }
        }
    }

    private fun refreshWorkerSchedule() {
        val currentState = state.value
        val email = currentState.googleAccountEmail
        val frequency = currentState.backupFrequency
        if ((currentState.isAutoBackupEnabled && email != null) || (currentState.isSdCardAutoBackupEnabled && currentState.sdCardFolderUri != null)) {
            scheduleWorker(email, frequency)
        } else {
            cancelWorker()
        }
    }

    private fun scheduleWorker(email: String?, frequency: String) {
        val workManager = androidx.work.WorkManager.getInstance(application)
        val repeatInterval = if (frequency == "Daily") 1L else 7L
        val timeUnit = java.util.concurrent.TimeUnit.DAYS
        val inputDataBuilder = androidx.work.Data.Builder()
        if (email != null) {
            inputDataBuilder.putString("email", email)
        }
        val inputData = inputDataBuilder.build()
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.suvojeet.notenext.data.backup.BackupWorker>(repeatInterval, timeUnit)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "auto_backup",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE, 
            workRequest
        )
    }

    private fun cancelWorker() {
        androidx.work.WorkManager.getInstance(application).cancelUniqueWork("auto_backup")
    }

    fun scanBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, restoreResult = null, foundBackupDetails = null)
            withContext(Dispatchers.IO) {
                try {
                    val scanResult = backupRepository.scanBackupContent(uri)
                    _state.value = _state.value.copy(isScanning = false, foundBackupDetails = scanResult)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(isScanning = false, restoreResult = "Failed to scan backup: ${e.message}")
                }
            }
        }
    }

    fun restoreSelectedProjects(uri: Uri, selectedProjectIds: List<Int>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, restoreResult = "Restoring selected projects...")
            withContext(Dispatchers.IO) {
                try {
                    backupRepository.restoreSelectedProjects(uri, selectedProjectIds)
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Selected projects restored successfully", foundBackupDetails = null)
                } catch (e: Exception) {
                     e.printStackTrace()
                    _state.value = _state.value.copy(isRestoring = false, restoreResult = "Restore failed: ${e.message}")
                }
            }
        }
    }

    fun clearFoundProjects() {
        _state.value = _state.value.copy(foundBackupDetails = null)
    }
}
