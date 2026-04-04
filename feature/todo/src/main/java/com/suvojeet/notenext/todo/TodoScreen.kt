@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.todo

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.suvojeet.notenext.core.R
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun TodoScreen(
    onBackClick: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagedTodos = viewModel.pagedTodos.collectAsLazyPagingItems()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodoUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.todos),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(TodoEvent.ShowAiTodoDialog) },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Todo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state.completedCount > 0) {
                        IconButton(onClick = { viewModel.onEvent(TodoEvent.DeleteAllCompleted) }, modifier = Modifier.springPress()) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(id = R.string.delete_completed),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(TodoEvent.ShowAddDialog) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(id = R.string.add_todo), fontWeight = FontWeight.Bold) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.springPress(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp)
                )
            }

            if (state.isLoading) {
...
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
                ExpressiveLoading()
            } else if (pagedTodos.itemCount == 0) {
                EmptyState(
                    icon = Icons.Default.CheckCircle,
                    message = when (state.filter) {
                        is TodoFilter.All -> stringResource(id = R.string.no_todos_yet)
                        is TodoFilter.Active -> stringResource(id = R.string.active_todos)
                        is TodoFilter.Completed -> stringResource(id = R.string.completed_todos)
                    },
                    description = when (state.filter) {
                        is TodoFilter.All -> stringResource(id = R.string.create_first_todo)
                        else -> null
                    }
                )
            } else {
                ExpressiveSection(
                    title = when(state.filter) {
                        is TodoFilter.All -> "Tasks"
                        is TodoFilter.Active -> "Active Tasks"
                        is TodoFilter.Completed -> "Completed"
                    },
                    description = if (state.activeCount > 0) "${state.activeCount} tasks remaining to crush it" else "All caught up!"
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
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
            onGenerate = { input ->
                viewModel.onEvent(TodoEvent.GenerateAiTodos(input))
            }
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
        onDismissRequest = if (isGenerating) ({}) else onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Todo Generator")
            }
        },
        text = {
            Column {
                Text(
                    "Paste a paragraph or messy notes, and AI will convert them into point-by-point tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enter text...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    enabled = !isGenerating,
                    shape = MaterialTheme.shapes.large
                )
                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Thinking...", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(input) },
                enabled = input.isNotBlank() && !isGenerating,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
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
        FilterChip(
            selected = selectedFilter is TodoFilter.All,
            onClick = { onFilterSelected(TodoFilter.All) },
            label = { Text(stringResource(id = R.string.all_todos)) },
            modifier = Modifier.springPress(),
            shape = MaterialTheme.shapes.medium,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter is TodoFilter.Active,
            onClick = { onFilterSelected(TodoFilter.Active) },
            label = { Text(stringResource(id = R.string.active_todos)) },
            modifier = Modifier.springPress(),
            shape = MaterialTheme.shapes.medium,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter is TodoFilter.Completed,
            onClick = { onFilterSelected(TodoFilter.Completed) },
            label = { Text(stringResource(id = R.string.completed_todos)) },
            modifier = Modifier.springPress(),
            shape = MaterialTheme.shapes.medium,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
