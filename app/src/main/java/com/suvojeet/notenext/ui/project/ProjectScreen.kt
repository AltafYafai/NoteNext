@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.project

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
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
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    onMenuClick: () -> Unit,
    onProjectClick: (Int) -> Unit,
    navController: androidx.navigation.NavController,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateSubProjectDialog by remember { mutableStateOf<Int?>(null) }
    val expandedProjects = remember { mutableStateMapOf<Int, Boolean>() }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectScreenEvent.CreateNewNote -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=TEXT")
                }
                is ProjectScreenEvent.CreateNewChecklist -> {
                    navController.navigate("add_edit_note?projectId=${event.projectId}&noteType=CHECKLIST")
                }
                else -> { }
            }
        }
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onConfirm = { projectName, projectDescription ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription))
                showCreateProjectDialog = false
            }
        )
    }

    if (showCreateSubProjectDialog != null) {
        CreateProjectDialog(
            onDismiss = { showCreateSubProjectDialog = null },
            onConfirm = { projectName, projectDescription ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription, showCreateSubProjectDialog))
                showCreateSubProjectDialog = null
            },
            title = "Create Sub-Project"
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.projects),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
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
                onClick = { showCreateProjectDialog = true },
                modifier = Modifier.springPress(),
                shape = MaterialTheme.colorScheme.extraLarge,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_new_project))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.projects.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Create,
                    message = stringResource(id = R.string.no_projects_yet),
                    description = stringResource(id = R.string.create_first_project)
                )
            } else {
                ExpressiveSection(
                    title = "Workspaces",
                    description = "Group your related notes and ideas together"
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.projects, key = { it.id }) { project ->
                            val isExpanded = expandedProjects[project.id] ?: false
                            HierarchicalProjectItem(
                                project = project,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedProjects[project.id] = !isExpanded
                                },
                                onClick = { onProjectClick(project.id) },
                                onCreateSubProject = {
                                    showCreateSubProjectDialog = project.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(id = R.string.create_new_project), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(id = R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text(stringResource(id = R.string.project_description)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(projectName, projectDescription.ifBlank { null }) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(id = R.string.create), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
