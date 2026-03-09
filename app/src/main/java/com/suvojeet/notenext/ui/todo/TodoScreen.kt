package com.suvojeet.notenext.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onBackClick: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.todos),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.activeCount > 0) {
                            Text(
                                text = "${state.activeCount} active • ${state.completedCount} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    if (state.completedCount > 0) {
                        IconButton(onClick = { viewModel.onEvent(TodoEvent.DeleteAllCompleted) }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(id = R.string.delete_completed),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(TodoEvent.ShowAddDialog) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(id = R.string.add_todo)) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            FilterChipRow(
                selectedFilter = state.filter,
                onFilterSelected = { viewModel.onEvent(TodoEvent.SetFilter(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.todos.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.todos, key = { it.id }) { todo ->
                        TodoItemCard(
                            todo = todo,
                            onToggleComplete = { viewModel.onEvent(TodoEvent.ToggleComplete(todo)) },
                            onClick = { viewModel.onEvent(TodoEvent.ShowEditDialog(todo)) },
                            onDelete = { viewModel.onEvent(TodoEvent.DeleteTodo(todo)) }
                        )
                    }
                    // Bottom spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (state.showAddEditDialog) {
        AddEditTodoDialog(
            editingTodo = state.editingTodo,
            onDismiss = { viewModel.onEvent(TodoEvent.DismissDialog) },
            onSave = { title, description, priority, dueDate ->
                viewModel.onEvent(TodoEvent.SaveTodo(title, description, priority, dueDate))
            }
        )
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
        FilterChip(
            selected = selectedFilter is TodoFilter.All,
            onClick = { onFilterSelected(TodoFilter.All) },
            label = { Text(stringResource(id = R.string.all_todos)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter is TodoFilter.Active,
            onClick = { onFilterSelected(TodoFilter.Active) },
            label = { Text(stringResource(id = R.string.active_todos)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter is TodoFilter.Completed,
            onClick = { onFilterSelected(TodoFilter.Completed) },
            label = { Text(stringResource(id = R.string.completed_todos)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
