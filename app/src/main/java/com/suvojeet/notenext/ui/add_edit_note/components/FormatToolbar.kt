@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.theme.ThemeMode
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import androidx.compose.ui.unit.DpOffset

@Composable
fun FormatToolbar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onInsertLinkClick: () -> Unit,
    onGrammarFixClick: () -> Unit,
    isFixingGrammar: Boolean,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    var showHeadingPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.editingNoteType == "CHECKLIST") {
                item {
                    FormatToggleButton(
                        onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.OutdentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                        description = "Outdent",
                        isActive = false,
                        useDarkTheme = useDarkTheme
                    )
                }
                item {
                    FormatToggleButton(
                        onClick = { state.focusedChecklistItemId?.let { onEvent(NotesEvent.IndentChecklistItem(it)) } },
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        description = "Indent",
                        isActive = false,
                        useDarkTheme = useDarkTheme
                    )
                }
                item {
                     VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                }
            }

            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },
                    icon = Icons.Default.FormatBold,
                    description = stringResource(id = R.string.bold_description),
                    isActive = state.isBoldActive,
                    useDarkTheme = useDarkTheme
                )
            }
            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },
                    icon = Icons.Default.FormatItalic,
                    description = stringResource(id = R.string.italic_description),
                    isActive = state.isItalicActive,
                    useDarkTheme = useDarkTheme
                )
            }
            item {
                FormatToggleButton(
                    onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },
                    icon = Icons.Default.FormatUnderlined,
                    description = stringResource(id = R.string.underline_description),
                    isActive = state.isUnderlineActive,
                    useDarkTheme = useDarkTheme
                )
            }

            item {
                 VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
            }

            item {
                Box {
                    FormatToggleButton(
                        onClick = { showHeadingPicker = true },
                        icon = Icons.Default.FormatSize,
                        description = stringResource(id = R.string.heading_style_description),
                        isActive = state.activeHeadingStyle != 0,
                        useDarkTheme = useDarkTheme
                    )
                    DropdownMenu(
                        expanded = showHeadingPicker,
                        onDismissRequest = { showHeadingPicker = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        HeadingStylePickerContent(
                            onDismissRequest = { showHeadingPicker = false },
                            onEvent = onEvent
                        )
                    }
                }
            }

            item {
                 VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
            }
            
            item {
                FormatToggleButton(
                    onClick = onGrammarFixClick,
                    icon = Icons.Default.AutoAwesome,
                    description = "Fix Grammar",
                    isActive = isFixingGrammar,
                    useDarkTheme = useDarkTheme
                )
            }
        }
    }
}

@Composable
private fun FormatToggleButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isActive: Boolean,
    useDarkTheme: Boolean
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) {
         MaterialTheme.colorScheme.onPrimaryContainer
    } else {
         MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).springPress(),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}
