package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.add_edit_note.openUrl
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.util.findActivity
import com.suvojeet.notenext.util.printNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AddEditNoteDialogs(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    showDeleteDialog: Boolean,
    onShowDeleteDialogChange: (Boolean) -> Unit,
    showMoreOptions: Boolean,
    onShowMoreOptionsChange: (Boolean) -> Unit,
    showSaveAsDialog: Boolean,
    onShowSaveAsDialogChange: (Boolean) -> Unit,
    showHistoryDialog: Boolean,
    onShowHistoryDialogChange: (Boolean) -> Unit,
    showInsertLinkDialog: Boolean,
    onShowInsertLinkDialogChange: (Boolean) -> Unit,
    clickedUrl: String?,
    onClickedUrlChange: (String?) -> Unit,
    showExactAlarmDialog: Boolean,
    onShowExactAlarmDialogChange: (Boolean) -> Unit,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onSaveAsPdf: () -> Unit = {},
    onSaveAsTxt: () -> Unit = {},
    onSaveAsMd: () -> Unit = {}
) {
    val context = LocalContext.current

    if (showDeleteDialog) {
        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
        AlertDialog(
            onDismissRequest = { onShowDeleteDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Move note to bin?") },
            text = { Text("This note will be moved to the recycle bin and permanently deleted after $autoDeleteDays days.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.springPress(),
                    onClick = {
                        scope.launch {
                            val htmlContent = HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
                            onEvent(NotesEvent.DeleteNote(com.suvojeet.notenext.data.NoteWithAttachments(
                                note = com.suvojeet.notenext.data.Note(
                                    id = state.expandedNoteId ?: 0,
                                    title = state.editingTitle,
                                    content = htmlContent,
                                    createdAt = System.currentTimeMillis(),
                                    lastEdited = System.currentTimeMillis(),
                                    color = state.editingColor
                                ),
                                attachments = emptyList(),
                                checklistItems = emptyList()
                            )))
                            onShowDeleteDialogChange(false)
                        }
                    }
                ) {
                    Text("Move to Bin")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteDialogChange(false) }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showMoreOptions) {
        MoreOptionsSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { onShowMoreOptionsChange(false) },
            showDeleteDialog = { onShowDeleteDialogChange(it) },
            showSaveAsDialog = { onShowSaveAsDialogChange(it) },
            showHistoryDialog = { onShowHistoryDialogChange(it) },
            onPrint = {
                scope.launch {
                    val htmlContent = HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
                    val fullHtml = "<h1>${state.editingTitle}</h1><br>$htmlContent"
                    printNote(context, fullHtml)
                }
            },
            onToggleLock = {
                if (state.editingIsLocked) {
                    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
                    if (activity != null) {
                        val biometricAuthManager = BiometricAuthManager(context, activity)
                        biometricAuthManager.showBiometricPrompt(
                            onAuthSuccess = { onEvent(NotesEvent.OnToggleLockClick) },
                            onAuthError = { Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show() }
                        )
                    } else {
                        Toast.makeText(context, "Authentication unavailable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    onEvent(NotesEvent.OnToggleLockClick)
                }
            }
        )
    }

    if (showSaveAsDialog) {
        com.suvojeet.notenext.ui.add_edit_note.components.SaveAsDialog(
            onDismiss = { onShowSaveAsDialogChange(false) },
            onSaveAsPdf = onSaveAsPdf,
            onSaveAsTxt = onSaveAsTxt,
            onSaveAsMd = onSaveAsMd
        )
    }

    if (clickedUrl != null) {
        AlertDialog(
            onDismissRequest = { onClickedUrlChange(null) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Open Link") },
            text = { Text("Do you want to open this link?\n\n$clickedUrl") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.springPress(),
                    onClick = {
                        openUrl(context, clickedUrl)
                        onClickedUrlChange(null)
                    }
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { onClickedUrlChange(null) }, modifier = Modifier.springPress()) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHistoryDialog) {
        NoteHistoryDialog(
            versions = state.editingNoteVersions,
            onDismiss = { onShowHistoryDialogChange(false) },
            onVersionSelected = { version ->
                onEvent(NotesEvent.OnRestoreVersion(version))
            }
        )
    }

    if (showInsertLinkDialog) {
        InsertLinkDialog(
            onDismiss = { onShowInsertLinkDialogChange(false) },
            onConfirm = { url ->
                onEvent(NotesEvent.OnInsertLink(url))
                onShowInsertLinkDialogChange(false)
            }
        )
    }
    
    if (state.showSummaryDialog) {
         AiSummarySheet(
            summary = state.summaryResult,
            isSummarizing = state.isSummarizing,
            onDismiss = { if (!state.isSummarizing) onEvent(NotesEvent.ClearSummary) },
            onClearSummary = { onEvent(NotesEvent.ClearSummary) }
        )
    }
    
    if (showExactAlarmDialog) {
         AlertDialog(
            onDismissRequest = { onShowExactAlarmDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Exact Alarm Permission Needed") },
            text = { Text("To ensure reminders fire at the exact time, please allow 'Alarms & reminders' permission in Settings.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.springPress(),
                    onClick = {
                        onShowExactAlarmDialogChange(false)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                             val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                             context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowExactAlarmDialogChange(false) }, modifier = Modifier.springPress()) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.showLabelDialog) {
        com.suvojeet.notenext.ui.components.LabelDialog(
            labels = state.labels,
            onDismiss = { onEvent(NotesEvent.DismissLabelDialog) },
            onConfirm = { label ->
                onEvent(NotesEvent.OnLabelChange(label))
                onEvent(NotesEvent.DismissLabelDialog)
            }
        )
    }
}
