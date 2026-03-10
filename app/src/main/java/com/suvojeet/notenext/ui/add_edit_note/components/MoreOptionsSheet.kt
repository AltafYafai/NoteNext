package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.suvojeet.notenext.ui.notes.NotesState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    state: NotesState,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.editingIsNewNote && state.editingLastEdited != 0L) {
                Text(
                    text = stringResource(id = R.string.last_edited, dateFormat.format(Date(state.editingLastEdited))),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            val lockLabel = if (state.editingIsLocked) "Unlock" else "Lock"
            val lockIcon = if (state.editingIsLocked) Icons.Default.LockOpen else Icons.Default.Lock
            val convertLabel = if (state.editingNoteType == "TEXT") "Convert to List" else "Convert to Text"
            val convertIcon = Icons.Default.Check

            data class OptionItem(val label: String, val icon: ImageVector, val action: () -> Unit)
            val options = mutableListOf<OptionItem>()
            
            options.add(OptionItem(lockLabel, lockIcon) { onToggleLock() })
            options.add(OptionItem(convertLabel, convertIcon) { onEvent(NotesEvent.OnToggleNoteType) })
            options.add(OptionItem(stringResource(id = R.string.delete), Icons.Default.Delete) { showDeleteDialog(true) })
            options.add(OptionItem(stringResource(id = R.string.make_a_copy), Icons.Default.ContentCopy) { onEvent(NotesEvent.OnCopyCurrentNoteClick) })
            options.add(OptionItem(stringResource(id = R.string.share), Icons.Default.Share) {
                 val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + state.editingContent.text)
                    putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            })
            options.add(OptionItem(stringResource(id = R.string.labels), Icons.AutoMirrored.Filled.Label) { onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick) })
            options.add(OptionItem("Print", Icons.Default.Print) { onPrint() })
            options.add(OptionItem(stringResource(id = R.string.save_as), Icons.Default.FileDownload) { showSaveAsDialog(true) })
            
            if (!state.editingIsNewNote) {
                options.add(OptionItem(stringResource(id = R.string.history), Icons.Default.History) { showHistoryDialog(true) })
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
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
            .springPress()
            .clickable(onClick = onClick)
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
