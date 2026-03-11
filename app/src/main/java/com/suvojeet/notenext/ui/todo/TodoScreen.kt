@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun TodoScreen(
    onBackClick: () -> Unit,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

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

            if (state.isLoading) {
                ExpressiveLoading()
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
                        items(state.todos, key = { it.id }) { todo ->
                            TodoItemCard(
                                todo = todo,
                                onToggleComplete = { viewModel.onEvent(TodoEvent.ToggleComplete(todo)) },
                                onClick = { viewModel.onEvent(TodoEvent.ShowEditDialog(todo)) },
                                onDelete = { viewModel.onEvent(TodoEvent.DeleteTodo(todo)) }
                            )
                        }
                    }
                }
            }
        }
    }

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
