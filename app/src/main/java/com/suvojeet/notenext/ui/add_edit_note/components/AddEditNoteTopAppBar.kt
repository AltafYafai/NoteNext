@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState

@Composable
fun AddEditNoteTopAppBar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    editingNoteType: String,
    onToggleFocusMode: () -> Unit,
    isFocusMode: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            if (state.editingIsNewNote) {
                Text(
                    text = if (editingNoteType == "CHECKLIST") stringResource(id = R.string.add_checklist) else stringResource(id = R.string.add_note),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = contentColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            SavedStatusIndicator(status = state.saveStatus, contentColor = contentColor)

            if (editingNoteType == "TEXT" && !state.editingIsNewNote) {
                IconButton(onClick = { onEvent(NotesEvent.SummarizeNote) }, modifier = Modifier.springPress()) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize Note", tint = contentColor)
                }
            }
            
            IconButton(onClick = onToggleFocusMode, modifier = Modifier.springPress()) {
                Icon(
                    imageVector = if (isFocusMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Focus Mode",
                    tint = contentColor
                )
            }

            if (!state.editingIsNewNote) {
                IconButton(onClick = { onEvent(NotesEvent.OnTogglePinClick) }, modifier = Modifier.springPress()) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = if (state.isPinned) stringResource(id = R.string.unpin_note) else stringResource(id = R.string.pin_note),
                        tint = if (state.isPinned) MaterialTheme.colorScheme.primary else contentColor
                    )
                }
                IconButton(onClick = { onEvent(NotesEvent.OnToggleArchiveClick) }, modifier = Modifier.springPress()) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = if (state.isArchived) stringResource(id = R.string.unarchive_note) else stringResource(id = R.string.archive_note),
                        tint = if (state.isArchived) MaterialTheme.colorScheme.primary else contentColor
                    )
                }
                IconButton(onClick = { showDeleteDialog(true) }, modifier = Modifier.springPress()) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_note), tint = contentColor)
                }
            }
        }
    )
}
