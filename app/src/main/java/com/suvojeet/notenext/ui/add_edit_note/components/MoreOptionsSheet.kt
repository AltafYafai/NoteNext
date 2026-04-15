@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.core.model.NoteType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoreOptionsSheet(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    showSaveAsDialog: (Boolean) -> Unit,
    showHistoryDialog: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onToggleLock: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    val lockLabel = stringResource(id = if (state.editingIsLocked) R.string.unlock else R.string.lock)
    val lockIcon = if (state.editingIsLocked) Icons.Default.LockOpen else Icons.Default.Lock
    val convertLabel = stringResource(id = if (state.editingNoteType == NoteType.TEXT) R.string.convert_to_list else R.string.convert_to_text)
    val convertIcon = Icons.Default.Check

    val searchLabel = stringResource(id = R.string.search)
    val deleteLabel = stringResource(id = R.string.delete)
    val copyLabel = stringResource(id = R.string.make_a_copy)
    val shareLabel = stringResource(id = R.string.share)
    val labelsLabel = stringResource(id = R.string.labels)
    val printLabel = stringResource(id = R.string.print)
    val saveAsLabel = stringResource(id = R.string.save_as)
    val historyLabel = stringResource(id = R.string.history)
    val convertToTodoLabel = stringResource(id = R.string.convert_to_todo)

    data class OptionItem(val label: String, val icon: ImageVector, val action: () -> Unit)
    
    val options = remember(state.editingIsLocked, state.editingNoteType, state.editingIsNewNote, lockLabel, convertLabel, searchLabel, deleteLabel, copyLabel, shareLabel, labelsLabel, printLabel, saveAsLabel, historyLabel, convertToTodoLabel) {
        mutableListOf<OptionItem>().apply {
            add(OptionItem(lockLabel, lockIcon) { onToggleLock() })
            add(OptionItem(convertLabel, convertIcon) { onEvent(NotesEvent.OnToggleNoteType) })
            add(OptionItem(searchLabel, Icons.Default.Search) { onEvent(NotesEvent.ToggleNoteSearch) })
            add(OptionItem(deleteLabel, Icons.Default.Delete) { showDeleteDialog(true) })
            add(OptionItem(copyLabel, Icons.Default.ContentCopy) { onEvent(NotesEvent.OnCopyCurrentNoteClick) })
            add(OptionItem(shareLabel, Icons.Default.Share) {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + state.editingContent.text)
                    putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            })
            add(OptionItem(labelsLabel, Icons.AutoMirrored.Filled.Label) { onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick) })
            add(OptionItem(printLabel, Icons.Default.Print) { onPrint() })
            add(OptionItem(saveAsLabel, Icons.Default.FileDownload) { showSaveAsDialog(true) })
            
            if (!state.editingIsNewNote) {
                add(OptionItem(historyLabel, Icons.Default.History) { showHistoryDialog(true) })
            }
            add(OptionItem(convertToTodoLabel, Icons.Default.PlaylistAddCheck) { onEvent(NotesEvent.ConvertToTodo) })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.editingIsNewNote && state.editingLastEdited != null && state.editingLastEdited != 0L) {
                item {
                    Text(
                        text = stringResource(id = R.string.last_edited, dateFormat.format(Date(state.editingLastEdited ?: 0L))),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            items(
                items = options,
                key = { it.label }
            ) { option ->
                MoreOptionsItem(
                    icon = option.icon,
                    label = option.label,
                    onClick = {
                        onDismiss()
                        option.action()
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreOptionsItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .springPress()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
