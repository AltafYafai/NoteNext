@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.core.markdown.MarkdownConverter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.NoteHtmlGenerator
import com.suvojeet.notenext.util.printNote
import com.suvojeet.notenext.util.findActivity
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
    showLabelDialog: Boolean,
    onShowLabelDialogChange: (Boolean) -> Unit,
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
    onSaveAsPdf: () -> Unit,
    onSaveAsTxt: () -> Unit,
    onSaveAsMd: () -> Unit
) {
    val context = LocalContext.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onShowDeleteDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.delete_note)) },
            text = { Text(stringResource(id = R.string.move_to_bin_question)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(NotesEvent.OnDeleteNoteClick)
                        onShowDeleteDialogChange(false)
                    },
                    modifier = Modifier.springPress(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.move_to_bin), color = MaterialTheme.colorScheme.onError)
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
            onEvent = { event ->
                if (event is NotesEvent.OnAddLabelsToCurrentNoteClick) {
                    onShowLabelDialogChange(true)
                } else {
                    onEvent(event)
                }
            },
            onDismiss = { onShowMoreOptionsChange(false) },
            showDeleteDialog = { onShowDeleteDialogChange(it) },
            showSaveAsDialog = { onShowSaveAsDialogChange(it) },
            showHistoryDialog = { onShowHistoryDialogChange(it) },
            onPrint = {
                scope.launch {
                    val fullHtml = NoteHtmlGenerator.generateNoteHtml(
                        context,
                        state.editingTitle,
                        MarkdownConverter.fromAnnotatedString(state.editingContent.annotatedString),
                        state.editingAttachments
                    )
                    printNote(context, fullHtml, state.editingTitle.ifBlank { "Note Document" })
                }
            },
            onToggleLock = {
                val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val biometricAuthManager = BiometricAuthManager(context, activity)
                    biometricAuthManager.showBiometricPrompt(
                        onAuthSuccess = { onEvent(NotesEvent.OnToggleLockClick) },
                        onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    Toast.makeText(context, "Authentication unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showLabelDialog) {
        com.suvojeet.notenext.ui.components.LabelDialog(
            onDismiss = { onShowLabelDialogChange(false) },
            onConfirm = { label ->
                onEvent(NotesEvent.OnLabelChange(label))
                onShowLabelDialogChange(false)
            },
            labels = state.labels
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            onDismiss = { onShowSaveAsDialogChange(false) },
            onSaveAsPdf = {
                onSaveAsPdf()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsTxt = {
                onSaveAsTxt()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsMd = {
                onSaveAsMd()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsNote = if (state.externalUri != null) {
                {
                    onEvent(NotesEvent.SaveExternalAsNote)
                    onShowSaveAsDialogChange(false)
                }
            } else null
        )
    }

    if (showHistoryDialog) {
        NoteHistoryDialog(
            versions = state.editingNoteVersions,
            isLocked = state.editingIsLocked,
            onVersionClick = { version ->
                onEvent(NotesEvent.OnRestoreVersion(version))
                onShowHistoryDialogChange(false)
            },
            onDismiss = { onShowHistoryDialogChange(false) }
        )
    }

    if (showInsertLinkDialog) {
        InsertLinkDialog(
            onDismiss = { onShowInsertLinkDialogChange(false) },
            onInsertLink = { text, url ->
                // Note: NotesEvent.OnInsertLink only takes url. 
                // Display text handling might need refinement if intended to be different.
                onEvent(NotesEvent.OnInsertLink(url))
                onShowInsertLinkDialogChange(false)
            }
        )
    }

    if (clickedUrl != null) {
        LinkActionDialog(
            url = clickedUrl,
            onDismiss = { onClickedUrlChange(null) },
            onOpenLink = {
                onClickedUrlChange(null)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(clickedUrl))
                context.startActivity(intent)
            },
            onCopyLink = {
                onClickedUrlChange(null)
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("URL", clickedUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { onShowExactAlarmDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(id = R.string.exact_alarm_permission)) },
            text = { Text(stringResource(id = R.string.exact_alarm_permission_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowExactAlarmDialogChange(false)
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.springPress()
                ) {
                    Text(stringResource(id = R.string.request))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowExactAlarmDialogChange(false) }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
