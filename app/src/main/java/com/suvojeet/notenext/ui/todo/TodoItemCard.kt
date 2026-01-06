package com.suvojeet.notenext.ui.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.TodoItem
import java.text.SimpleDateFormat
import java.util.*

// Priority colors with good contrast
object TodoPriorityColors {
    val High = Color(0xFFE53935) // Red
    val Medium = Color(0xFFFF9800) // Orange
    val Low = Color(0xFF4CAF50) // Green
    val HighLight = Color(0xFFFFEBEE) // Light Red background
    val MediumLight = Color(0xFFFFF3E0) // Light Orange background
    val LowLight = Color(0xFFE8F5E9) // Light Green background
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    false // Don't dismiss, just toggle
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
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = "SwipeIconScale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
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
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority color strip on left
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .defaultMinSize(minHeight = 72.dp)
                        .background(priorityColor)
                )
                
                // Checkbox
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.padding(start = 8.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (todo.isCompleted) 0.5f else 1f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    // Due date chip
                    todo.dueDate?.let { dueDate ->
                        val isOverdue = dueDate < System.currentTimeMillis() && !todo.isCompleted
                        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                        
                        Text(
                            text = if (isOverdue) "⚠️ ${dateFormat.format(Date(dueDate))}" else "📅 ${dateFormat.format(Date(dueDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Priority label on right
                Text(
                    text = when (todo.priority) {
                        2 -> "HIGH"
                        1 -> "MED"
                        else -> "LOW"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}

