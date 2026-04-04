@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.todo

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun TodoScreen(
    onBackClick: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagedTodos = viewModel.pagedTodos.collectAsLazyPagingItems()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Todos",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TodoEvent.ShowAiTodoDialog) }, modifier = Modifier.springPress()) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Todo")
                    }
                    if (state.filter == TodoFilter.Completed) {
                        IconButton(onClick = { viewModel.onEvent(TodoEvent.DeleteAllCompleted) }, modifier = Modifier.springPress()) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Completed")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(TodoEvent.ShowAddDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.springPress()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Todo")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChipRow(
                selectedFilter = state.filter,
                onFilterSelected = { viewModel.onEvent(TodoEvent.SetFilter(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (!state.isLoading && state.filter is TodoFilter.All) {
                ProductivityDashboard(
                    activeCount = state.activeCount,
                    completedTodayCount = state.completedTodayCount,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                )
            }

            if (state.isLoading) {
                ExpressiveLoading()
            } else if (pagedTodos.itemCount == 0) {
                EmptyState(
                    icon = Icons.Default.CheckCircle,
                    message = when (state.filter) {
                        is TodoFilter.All -> "All clear! No tasks for now."
                        is TodoFilter.Active -> "You've finished everything! Great job."
                        is TodoFilter.Completed -> "No completed tasks yet. Go get 'em!"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = pagedTodos.itemCount,
                        key = pagedTodos.itemKey { it.todo.id },
                        contentType = pagedTodos.itemContentType { "todo" }
                    ) { index ->
                        pagedTodos[index]?.let { todoWithSubtasks ->
                            TodoItemCard(
                                todoWithSubtasks = todoWithSubtasks,
                                onToggleComplete = { viewModel.onEvent(TodoEvent.ToggleComplete(todoWithSubtasks.todo)) },
                                onClick = { viewModel.onEvent(TodoEvent.ShowEditDialog(todoWithSubtasks.todo)) },
                                onDelete = { viewModel.onEvent(TodoEvent.DeleteTodo(todoWithSubtasks.todo)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showAddEditDialog) {
        val projects by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
        AddEditTodoDialog(
            editingTodo = state.editingTodo,
            initialSubtasks = state.editingSubtasks,
            projects = projects,
            onDismiss = { viewModel.onEvent(TodoEvent.DismissDialog) },
            onSave = { title, description, priority, dueDate, reminderTime, projectId, subtasks ->
                viewModel.onEvent(TodoEvent.SaveTodo(title, description, priority, dueDate, reminderTime, projectId, subtasks))
            }
        )
    }

    if (state.showAiTodoDialog) {
        AiTodoDialog(
            isGenerating = state.isGenerating,
            onDismiss = { viewModel.onEvent(TodoEvent.DismissAiTodoDialog) },
            onGenerate = { viewModel.onEvent(TodoEvent.GenerateAiTodos(it)) }
        )
    }
}

@Composable
fun ProductivityDashboard(
    activeCount: Int,
    completedTodayCount: Int,
    modifier: Modifier = Modifier
) {
    val total = activeCount + completedTodayCount
    val progress = if (total > 0) completedTodayCount.toFloat() / total else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (completedTodayCount > 0) "You've crushed $completedTodayCount tasks today!" else "Time to get started!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    label = "Remaining",
                    value = activeCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Done Today",
                    value = completedTodayCount.toString(),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    selectedFilter: TodoFilter,
    onFilterSelected: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TodoFilterItem(
            filter = TodoFilter.All,
            label = "All",
            isSelected = selectedFilter is TodoFilter.All,
            onClick = { onFilterSelected(TodoFilter.All) }
        )
        TodoFilterItem(
            filter = TodoFilter.Active,
            label = "Active",
            isSelected = selectedFilter is TodoFilter.Active,
            onClick = { onFilterSelected(TodoFilter.Active) }
        )
        TodoFilterItem(
            filter = TodoFilter.Completed,
            label = "Done",
            isSelected = selectedFilter is TodoFilter.Completed,
            onClick = { onFilterSelected(TodoFilter.Completed) }
        )
    }
}

@Composable
private fun TodoFilterItem(
    filter: TodoFilter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.springPress(),
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AiTodoDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Todos with AI") },
        text = {
            Column {
                Text("Describe your task or paste a messy note, and AI will split it into clear todos.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enter text...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(input) },
                enabled = input.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thinking...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Text("Generate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
