package com.suvojeet.notenext.ui.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.data.Label
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLabelsScreen(
    onBackPressed: () -> Unit
) {
    val viewModel: EditLabelsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.edit_labels),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
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
            FloatingActionButton(
                onClick = { viewModel.onEvent(EditLabelsEvent.ShowAddLabelDialog) },
                shape = FloatingActionButtonDefaults.largeShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_label))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.labels.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Label,
                    message = "No labels yet. Create one to organize your notes."
                )
            } else {
                ExpressiveSection(
                    title = "Organization",
                    description = "Manage labels to categorize your thoughts"
                ) {
                    SettingsGroupCard {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(state.labels) { label ->
                                LabelItem(
                                    label = label,
                                    onEditClick = { viewModel.onEvent(EditLabelsEvent.ShowEditLabelDialog(label)) }
                                )
                                if (state.labels.last() != label) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.showAddLabelDialog) {
            AddLabelDialog(
                onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                onConfirm = { name ->
                    viewModel.onEvent(EditLabelsEvent.AddLabel(name))
                }
            )
        }

        if (state.showEditLabelDialog) {
            state.selectedLabel?.let { label ->
                EditLabelDialog(
                    label = label,
                    onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                    onConfirm = { newName ->
                        viewModel.onEvent(EditLabelsEvent.UpdateLabel(label, newName))
                    },
                    onDelete = {
                        viewModel.onEvent(EditLabelsEvent.DeleteLabel(label))
                    }
                )
            }
        }
    }
}

@Composable
fun LabelItem(
    label: Label,
    onEditClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        headlineContent = { 
            Text(
                text = label.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ) 
        },
        leadingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(id = R.string.edit_labels),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

@Composable
fun AddLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_label)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.new_label)) }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                }
            ) {
                Text(stringResource(id = R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun EditLabelDialog(
    label: Label,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(label.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.edit_labels)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.new_label)) }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    )
}
