package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.ChecklistEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    isCheckedItemsExpanded: Boolean,
    onToggleCheckedItems: () -> Unit
) {
    val uncheckedItems = state.editingChecklist.filter { !it.isChecked }.sortedBy { it.position }
    val checkedItems = state.editingChecklist.filter { it.isChecked }.sortedBy { it.position }

    itemsIndexed(uncheckedItems, key = { _, item -> item.id }) { index, item ->
        val dragOffset = remember { mutableStateOf(0f) }
        val isDragging = dragOffset.value != 0f
        
        val currentUncheckedItems by rememberUpdatedState(uncheckedItems)
        val currentIndex by rememberUpdatedState(index)
        
        val dragModifier = Modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset.value += dragAmount.y
                        
                        val threshold = 100f 
                        val items = currentUncheckedItems
                        val i = currentIndex

                        if (dragOffset.value > threshold) {
                            if (i < items.lastIndex) {
                                onEvent(NotesEvent.SwapChecklistItems(item.id, items[i + 1].id))
                                dragOffset.value -= threshold 
                            }
                        } else if (dragOffset.value < -threshold) {
                            if (i > 0) {
                                onEvent(NotesEvent.SwapChecklistItems(item.id, items[i - 1].id))
                                dragOffset.value += threshold
                            }
                        }
                    },
                    onDragEnd = { dragOffset.value = 0f },
                    onDragCancel = { dragOffset.value = 0f }
                )
            }

        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                initialOffsetY = { -40 }
            ) + fadeIn(animationSpec = spring())
        ) {
            ChecklistItemRow(
                item = item,
                inputValue = state.checklistInputValues[item.id],
                onEvent = onEvent,
                isChecked = false,
                isNewlyAdded = state.newlyAddedChecklistItemId == item.id,
                modifier = Modifier
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            scaleX = 1.03f
                            scaleY = 1.03f
                            shadowElevation = 8f
                        }
                    },
                dragModifier = dragModifier
            )
        }
    }

    item {
        TextButton(
            onClick = { onEvent(NotesEvent.AddChecklistItem) },
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .springPress(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.add_item), fontWeight = FontWeight.Bold)
        }
    }

    if (checkedItems.isNotEmpty()) {
        item {
            val rotationAngle by animateFloatAsState(
                targetValue = if (isCheckedItemsExpanded) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "arrow_rotation"
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleCheckedItems,
                    modifier = Modifier.size(24.dp).springPress()
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCheckedItemsExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Checked items (${checkedItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                    
                TextButton(onClick = { onEvent(NotesEvent.DeleteAllCheckedItems) }, modifier = Modifier.springPress()) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(checkedItems, key = { it.id }) { item ->
            AnimatedVisibility(
                visible = isCheckedItemsExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(animationSpec = spring()),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut(animationSpec = spring())
            ) {
                ChecklistItemRow(
                    item = item,
                    inputValue = state.checklistInputValues[item.id],
                    onEvent = onEvent,
                    isChecked = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    inputValue: TextFieldValue?,
    onEvent: (NotesEvent) -> Unit,
    isChecked: Boolean,
    isNewlyAdded: Boolean = false,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onEvent(NotesEvent.DeleteChecklistItem(item.id))
                true
            } else {
                false
            }
        }
    )
    
    val checkScale by animateFloatAsState(
        targetValue = if (isChecked) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "check_scale"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isChecked) 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
        else 
            MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(),
        label = "text_color"
    )
    
    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            focusRequester.requestFocus()
            onEvent(NotesEvent.ClearNewlyAddedChecklistItemId)
        }
    }

    SwipeToDismissBox(
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        state = dismissState,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(vertical = 8.dp)
                .padding(start = (item.level * 24).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(24.dp)
                    .then(dragModifier),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) 
            
            Box(modifier = Modifier.scale(checkScale).springPress()) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        onEvent(NotesEvent.OnChecklistItemCheckedChange(item.id, checked))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            BasicTextField(
                value = inputValue ?: TextFieldValue(item.text),
                onValueChange = { textFieldValue: TextFieldValue ->
                     onEvent(NotesEvent.OnChecklistItemValueChange(item.id, textFieldValue))
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(start = 12.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onEvent(NotesEvent.OnChecklistItemFocus(item.id))
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = textColor,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false 
            )
            
            IconButton(
                onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) },
                modifier = Modifier.alpha(0.6f).springPress()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
