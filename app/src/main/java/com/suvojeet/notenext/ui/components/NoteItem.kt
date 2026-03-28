@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.ui.theme.NoteGradients

@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: NoteSummaryWithAttachments,
    isSelected: Boolean,
    searchQuery: String = "",
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
    binnedDaysRemaining: Int? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val adaptiveColor = NoteGradients.getAdaptiveColor(note.note.color, isDarkTheme)
    val isDefaultColor = adaptiveColor == 0

    val contentColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurface
    } else {
        NoteGradients.getContentColor(adaptiveColor)
    }
    
    val tintColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        contentColor.copy(alpha = 0.7f)
    }

    val decryptedNote = remember(note.note.title, note.note.content, note.note.isEncrypted) {
        if (note.note.isEncrypted) {
            if (note.note.isLocked) {
                // Never attempt to decrypt locked notes without auth.
                // Tap-to-unlock is handled by the click handler in NotesScreen.
                note.note
            } else {
                // Non-locked encrypted notes use the non-auth key — safe to decrypt here.
                com.suvojeet.notenext.util.CryptoUtils.decryptNote(note.note)
            }
        } else {
            note.note
        }
    }
    val motionScheme = MaterialTheme.motionScheme
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else (if (isDefaultColor) 1.dp else 0.dp),
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Elevation"
    )

    val cardShape = if (note.note.isPinned) {
        MaterialTheme.shapes.extraLarge
    } else {
        MaterialTheme.shapes.medium
    }

    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (isDefaultColor) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    } else {
        null
    }

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "note-${note.note.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds
                )
                .combinedClickable(
                    onClick = onNoteClick,
                    onLongClick = onNoteLongClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isDefaultColor) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    Color(adaptiveColor)
                },
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                if (decryptedNote.title.isNotBlank()) {
                    Text(
                        text = buildAnnotatedString {
                            val title = decryptedNote.title
                            if (searchQuery.isNotBlank() && title.contains(searchQuery, ignoreCase = true)) {
                                var startIndex = 0
                                while (startIndex < title.length) {
                                    val index = title.indexOf(searchQuery, startIndex, ignoreCase = true)
                                    if (index == -1) {
                                        append(title.substring(startIndex))
                                        break
                                    }
                                    append(title.substring(startIndex, index))
                                    withStyle(SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)) {
                                        append(title.substring(index, index + searchQuery.length))
                                    }
                                    startIndex = index + searchQuery.length
                                }
                            } else {
                                append(title)
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (note.note.noteType == NoteType.CHECKLIST) {
                    val checklistItems = remember(decryptedNote.content) {
                        try {
                            val type = object : com.google.gson.reflect.TypeToken<List<ChecklistItem>>() {}.type
                            com.google.gson.Gson().fromJson<List<ChecklistItem>>(decryptedNote.content, type)
                        } catch (e: Exception) {
                            emptyList<ChecklistItem>()
                        }
                    }
                    checklistItems.take(5).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = tintColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (item.isChecked) contentColor.copy(alpha = 0.5f) else contentColor
                            )
                        }
                    }
                    if (checklistItems.size > 5) {
                        Text(
                            text = "+ ${checklistItems.size - 5} more",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = tintColor
                        )
                    }
                } else if (decryptedNote.content.isNotBlank()) {
                    val plainText = remember(decryptedNote.content) {
                        HtmlCompat.fromHtml(decryptedNote.content, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    }
                    Text(
                        text = buildAnnotatedString {
                            if (searchQuery.isNotBlank() && plainText.contains(searchQuery, ignoreCase = true)) {
                                var startIndex = 0
                                while (startIndex < plainText.length) {
                                    val index = plainText.indexOf(searchQuery, startIndex, ignoreCase = true)
                                    if (index == -1) {
                                        append(plainText.substring(startIndex))
                                        break
                                    }
                                    append(plainText.substring(startIndex, index))
                                    withStyle(SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)) {
                                        append(plainText.substring(index, index + searchQuery.length))
                                    }
                                    startIndex = index + searchQuery.length
                                }
                            } else {
                                append(plainText)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                if (note.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NoteItemAttachments(
                        attachments = note.attachments,
                        tintColor = tintColor
                    )
                }

                if (note.note.isLocked || note.note.reminderTime != null || note.note.isEncrypted || note.note.label != null || binnedDaysRemaining != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (note.note.isPinned) {
                             Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(14.dp),
                                tint = tintColor
                            )
                        }
                        if (note.note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(14.dp),
                                tint = tintColor
                            )
                        }
                        if (note.note.isEncrypted && !note.note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = "Encrypted",
                                modifier = Modifier.size(14.dp),
                                tint = tintColor
                            )
                        }
                        if (note.note.reminderTime != null) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder",
                                modifier = Modifier.size(14.dp),
                                tint = tintColor
                            )
                        }
                        
                        if (note.note.label != null) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Labels",
                                modifier = Modifier.size(14.dp),
                                tint = tintColor
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (binnedDaysRemaining != null) {
                            Text(
                                text = "$binnedDaysRemaining days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteItemAttachments(
    attachments: List<Attachment>,
    tintColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val imageCount = attachments.count { it.type == AttachmentType.IMAGE }
        val audioCount = attachments.count { it.type == AttachmentType.AUDIO }
        val fileCount = attachments.count { it.type == AttachmentType.FILE }

        if (imageCount > 0) {
            AttachmentBadge(imageVector = Icons.Default.Image, count = imageCount, tintColor = tintColor)
        }
        if (audioCount > 0) {
            AttachmentBadge(imageVector = Icons.Default.Mic, count = audioCount, tintColor = tintColor)
        }
        if (fileCount > 0) {
            AttachmentBadge(imageVector = Icons.Default.AttachFile, count = fileCount, tintColor = tintColor)
        }
    }
}

@Composable
private fun AttachmentBadge(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tintColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = tintColor
        )
        if (count > 1) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = tintColor
            )
        }
    }
}
