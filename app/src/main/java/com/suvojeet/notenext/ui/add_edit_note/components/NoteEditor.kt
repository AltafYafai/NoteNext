@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteTitleEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onReminderClick: () -> Unit,
    scrollOffset: Float = 0f
) {
    val parallaxOffset = (-scrollOffset * 0.2f).coerceIn(-20f, 20f)
    
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .graphicsLayer {
                translationY = parallaxOffset
            }
    ) {
        val titleTextColor = MaterialTheme.colorScheme.onSurface

        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { 
                Text(
                    stringResource(id = R.string.title), 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = titleTextColor,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            singleLine = true,
            maxLines = 1
        )

        ReminderDisplay(
            reminderTime = state.editingReminderTime,
            repeatOption = state.editingRepeatOption,
            onClick = onReminderClick
        )
    }
}

@Composable
fun NoteContentEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (String) -> Unit,
    onSlashCommand: () -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val interactionSource = remember { MutableInteractionSource() }

    // Hold latest references for use inside pointerInput(Unit) coroutine
    val currentOnUrlClick by rememberUpdatedState(onUrlClick)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentContent by rememberUpdatedState(state.editingContent)
    
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val cursorColor = MaterialTheme.colorScheme.primary
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            cursorColor.copy(alpha = glowAlpha),
            cursorColor.copy(alpha = glowAlpha * 0.4f),
            Color.Transparent
        ),
        radius = 60f
    )

    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        val contentTextColor = MaterialTheme.colorScheme.onSurface
        val contentTextStyle = when (state.activeHeadingStyle) {
            1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
            else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor, lineHeight = 28.sp)
        }

        BasicTextField(
            value = state.editingContent,
            onValueChange = { newContent -> 
                onEvent(NotesEvent.OnContentChange(newContent))
                val cursor = newContent.selection.start
                if (cursor > 0 && newContent.text.isNotEmpty() && cursor <= newContent.text.length) {
                    val lastChar = newContent.text[cursor - 1]
                    if (lastChar == '/') {
                         val precedingChar = if (cursor > 1) newContent.text[cursor - 2] else ' '
                         if (precedingChar.isWhitespace()) {
                             onSlashCommand()
                         }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    textLayoutResult?.let { layout ->
                        val cursorPosition = state.editingContent.selection.start
                        if (cursorPosition >= 0 && layout.layoutInput.text.isNotEmpty() && cursorPosition <= layout.layoutInput.text.length) {
                            try {
                                val cursorRect = layout.getCursorRect(cursorPosition.coerceIn(0, layout.layoutInput.text.length))
                                drawCircle(
                                    brush = glowBrush,
                                    radius = 40f,
                                    center = Offset(cursorRect.left, cursorRect.top + cursorRect.height / 2)
                                )
                            } catch (e: Exception) {}
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                            textLayoutResult?.let { layoutResult ->
                                val position = layoutResult.getOffsetForPosition(offset)
                                val content = currentContent

                                val urlAnnotation = content.annotatedString.getStringAnnotations("URL", position, position).firstOrNull()
                                    ?: content.annotatedString.getStringAnnotations("EMAIL", position, position).firstOrNull()
                                    ?: content.annotatedString.getStringAnnotations("PHONE", position, position).firstOrNull()

                                if (urlAnnotation != null) {
                                    currentOnUrlClick(urlAnnotation.item)
                                } else {
                                    content.annotatedString.getStringAnnotations("NOTE_LINK", position, position)
                                        .firstOrNull()?.let { annotation ->
                                            currentOnEvent(NotesEvent.NavigateToNoteByTitle(annotation.item))
                                        }
                                }
                            }
                    }
                },
            onTextLayout = { textLayoutResult = it },
            textStyle = contentTextStyle,
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = state.editingContent.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { Text(stringResource(id = R.string.note), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), style = contentTextStyle) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = cursorColor,
                        selectionColors = TextSelectionColors(
                            handleColor = cursorColor,
                            backgroundColor = cursorColor.copy(alpha = 0.3f)
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp) 
                )
            }
        )
    }
}

@Composable
fun ReminderDisplay(
    reminderTime: Long?,
    repeatOption: String?,
    onClick: () -> Unit
) {
    if (reminderTime != null) {
        val dateTime = java.time.Instant.ofEpochMilli(reminderTime).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")
        val formattedTime = formatter.format(dateTime)
        val repeatText = if (repeatOption != null && repeatOption != "DOES_NOT_REPEAT") ", $repeatOption" else ""

        AssistChip(
            onClick = onClick,
            label = { Text(text = "$formattedTime$repeatText") },
            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = "Reminder") },
            shape = MaterialTheme.shapes.medium,
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.springPress()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
