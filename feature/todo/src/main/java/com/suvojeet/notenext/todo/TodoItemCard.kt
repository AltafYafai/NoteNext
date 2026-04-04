@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.core.R
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.ui.components.springPress
import java.text.SimpleDateFormat
import java.util.*

object TodoPriorityColors {
    val High = Color(0xFFE53935)
    val Medium = Color(0xFFFF9800)
    val Low = Color(0xFF4CAF50)
}

@Composable
fun TodoItemCard(
    todo: TodoItem,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (todo.priority) {
        2 -> TodoPriorityColors.High
        1 -> TodoPriorityColors.Medium
        else -> TodoPriorityColors.Low
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                },
                animationSpec = spring(),
                label = "SwipeBackgroundColor"
            )
            val icon = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                else -> null
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1.1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "SwipeIconScale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.scale(scale),
                        tint = if (direction == SwipeToDismissBoxValue.EndToStart) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier.clip(MaterialTheme.shapes.extraLarge)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .springPress()
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(80.dp)
                        .background(priorityColor)
                )
                
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.padding(start = 12.dp).springPress(),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (todo.isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (todo.description.isNotBlank()) {
                        Text(
                            text = todo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (todo.isCompleted) 0.5f else 1f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    todo.dueDate?.let { dueDate ->
                        val isOverdue = dueDate < System.currentTimeMillis() && !todo.isCompleted
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (isOverdue) "⚠️ Overdue: ${dateFormat.format(Date(dueDate))}" else "📅 ${dateFormat.format(Date(dueDate))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverdue) 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Surface(
                    shape = CircleShape,
                    color = priorityColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = when (todo.priority) {
                            2 -> "HIGH"
                            1 -> "MED"
                            else -> "LOW"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
