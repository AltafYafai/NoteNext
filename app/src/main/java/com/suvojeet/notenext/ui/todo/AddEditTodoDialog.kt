@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.ui.components.springPress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditTodoDialog(
    editingTodo: TodoItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?) -> Unit
) {
    var title by remember { mutableStateOf(editingTodo?.title ?: "") }
    var description by remember { mutableStateOf(editingTodo?.description ?: "") }
    var priority by remember { mutableIntStateOf(editingTodo?.priority ?: 0) }
    var dueDate by remember { mutableStateOf(editingTodo?.dueDate) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = if (editingTodo == null) stringResource(id = R.string.add_todo) else "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    minLines = 2
                )
                
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriorityChip(
                            label = "Low",
                            selected = priority == 0,
                            onClick = { priority = 0 },
                            color = TodoPriorityColors.Low,
                            modifier = Modifier.weight(1f)
                        )
                        PriorityChip(
                            label = "Med",
                            selected = priority == 1,
                            onClick = { priority = 1 },
                            color = TodoPriorityColors.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        PriorityChip(
                            label = "High",
                            selected = priority == 2,
                            onClick = { priority = 2 },
                            color = TodoPriorityColors.High,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().springPress(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (dueDate != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (dueDate != null) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDate!!))
                            } else "Set due date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dueDate != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { dueDate = null },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, description, priority, dueDate) },
                enabled = title.isNotBlank(),
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(id = R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = color,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.springPress()
    )
}
