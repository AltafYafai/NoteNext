package com.suvojeet.notenext.navigation

import androidx.compose.animation.core.tween
import com.suvojeet.notenext.ui.project.toNotesUiEvent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.suvojeet.notenext.ui.archive.ArchiveScreen
import com.suvojeet.notenext.ui.bin.BinScreen
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.labels.EditLabelsScreen
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesScreen
import com.suvojeet.notenext.ui.notes.NotesViewModel

import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.reminder.ReminderScreen
import com.suvojeet.notenext.ui.reminder.AddEditReminderScreen
import com.suvojeet.notenext.ui.settings.AboutScreen
import com.suvojeet.notenext.ui.project.ProjectScreen
import com.suvojeet.notenext.ui.project.ProjectViewModel
import com.suvojeet.notenext.ui.project.ProjectNotesScreen
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.project.toNotesState
import com.suvojeet.notenext.ui.project.toProjectNotesEvent
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.settings.BackupScreen
import com.suvojeet.notenext.ui.qr.QrScannerScreen
import com.suvojeet.notenext.ui.todo.TodoScreen

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import com.suvojeet.notenext.R
import kotlinx.coroutines.flow.SharingStarted
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.navigation.toRoute

@Composable
fun NavGraph(themeMode: ThemeMode, windowSizeClass: WindowSizeClass, startNoteId: Int = -1, startAddNote: Boolean = false, sharedText: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notesViewModel: NotesViewModel = hiltViewModel()
    val notesState by notesViewModel.state.collectAsState()

    val activity = context.findActivity() as? FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(startNoteId) {
        if (startNoteId != -1) {
            val isLocked = notesViewModel.getNoteLockStatus(startNoteId)
            if (isLocked) {
                biometricAuthManager?.showBiometricPrompt(
                    onAuthSuccess = {
                        notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                        navController.navigate(Destination.Notes()) {
                            popUpTo<Destination.Notes> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAuthError = {
                        Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
            } else {
                notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                navController.navigate(Destination.Notes()) {
                    popUpTo<Destination.Notes> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(startAddNote) {
        if (startAddNote) {
            notesViewModel.onEvent(NotesEvent.ExpandNote(-1))
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            notesViewModel.onEvent(NotesEvent.CreateNoteFromSharedText(sharedText))
        }
    }

    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.fillMaxWidth(0.15f)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.notes)) },
                        label = { Text(stringResource(id = R.string.notes)) },
                        selected = currentDestination?.route?.contains("Notes") == true && notesState.filteredLabel == null,
                        onClick = {
                            if (currentDestination?.route?.contains("Notes") != true || notesState.filteredLabel != null) {
                                notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                                navController.navigate(Destination.Notes()) {
                                    popUpTo<Destination.Notes> { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(id = R.string.projects)) },
                        label = { Text(stringResource(id = R.string.projects)) },
                        selected = currentDestination?.route?.contains("Projects") == true,
                        onClick = {
                            navController.navigate(Destination.Projects)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = stringResource(id = R.string.archive)) },
                        label = { Text(stringResource(id = R.string.archive)) },
                        selected = currentDestination?.route?.contains("Archive") == true,
                        onClick = {
                            navController.navigate(Destination.Archive) {
                                popUpTo<Destination.Notes>()
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin)) },
                        label = { Text(stringResource(id = R.string.bin)) },
                        selected = currentDestination?.route?.contains("Bin") == true,
                        onClick = {
                            navController.navigate(Destination.Bin) {
                                popUpTo<Destination.Notes>()
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) },
                        label = { Text(stringResource(id = R.string.settings)) },
                        selected = currentDestination?.route?.contains("Settings") == true,
                        onClick = {
                            navController.navigate(Destination.Settings)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = stringResource(id = R.string.reminders)) },
                        label = { Text(stringResource(id = R.string.reminders)) },
                        selected = currentDestination?.route?.contains("Reminder") == true,
                        onClick = {
                            navController.navigate(Destination.Reminder)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (notesState.labels.isEmpty()) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },
                            label = { Text(stringResource(id = R.string.create_new_label)) },
                            selected = false,
                            onClick = {
                                navController.navigate(Destination.EditLabels)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.labels_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = {
                                navController.navigate(Destination.EditLabels)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(id = R.string.edit_labels),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        notesState.labels.forEach { label ->
                            NavigationDrawerItem(
                                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },
                                label = { Text(label) },
                                selected = notesState.filteredLabel == label,
                                onClick = {
                                    notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                                    if (currentDestination?.route?.contains("Notes") != true || notesState.filteredLabel != label) {
                                        navController.navigate(Destination.Notes()) {
                                            popUpTo<Destination.Notes> { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        ) {
            NavHost(navController = navController, startDestination = Destination.Notes(), modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                composable<Destination.Notes>(
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    NotesScreen(
                        viewModel = notesViewModel,
                        onSettingsClick = { navController.navigate(Destination.Settings) },
                        onArchiveClick = { navController.navigate(Destination.Archive) },
                        onEditLabelsClick = { navController.navigate(Destination.EditLabels) },
                        onBinClick = { navController.navigate(Destination.Bin) },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onScanQrClick = { navController.navigate(Destination.QrScanner) },
                        onTodoClick = { navController.navigate(Destination.Todo) },
                        events = notesViewModel.events
                    )
                }

                composable<Destination.Settings>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    SettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigate = { route -> 
                             when(route) {
                                 "backup" -> navController.navigate(Destination.Backup)
                                 "about" -> navController.navigate(Destination.About)
                                 else -> {}
                             }
                        }
                    )
                }
                composable<Destination.Backup>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    BackupScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Destination.Archive>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ArchiveScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable<Destination.EditLabels>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    EditLabelsScreen(
                        onBackPressed = { navController.popBackStack() }
                    )
                }

                composable<Destination.Bin>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    val binViewModel: BinViewModel = hiltViewModel()
                    BinScreen(
                        viewModel = binViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable<Destination.Reminder>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ReminderScreen(
                        onBackClick = { navController.popBackStack() },
                        onNoteClick = { note ->
                            navController.navigate(Destination.Notes(noteId = note.id)) {
                                popUpTo<Destination.Notes> { inclusive = true }
                            }
                        }
                    )
                }
                composable<Destination.AddEditReminder>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    AddEditReminderScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.Projects>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ProjectScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProjectClick = { projectId ->
                            navController.navigate(Destination.ProjectNotes(projectId))
                        },
                        navController = navController,
                        settingsRepository = settingsRepository
                    )
                }
                composable<Destination.About>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    AboutScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.QrScanner>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    QrScannerScreen(
                        onBackClick = { navController.popBackStack() },
                        onNoteScanned = { title, content ->
                            notesViewModel.onEvent(NotesEvent.CreateNoteFromQr(title, content))
                            navController.popBackStack()
                        }
                    )
                }
                composable<Destination.Todo>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    TodoScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.ProjectNotes>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ProjectNotesScreen(
                        onBackClick = { navController.popBackStack() },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository
                    )
                }
                composable<Destination.AddEditNote> {
                    val viewModel: ProjectNotesViewModel = hiltViewModel()
                    AddEditNoteScreen(
                        state = viewModel.state.collectAsState().value.toNotesState(),
                        onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                        onDismiss = { navController.popBackStack() },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository,
                        events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed())
                    )
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = notesState.expandedNoteId == null,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.7f)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.notes)) },
                        label = { Text(stringResource(id = R.string.notes)) },
                        selected = currentDestination?.route?.contains("Notes") == true && notesState.filteredLabel == null,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentDestination?.route?.contains("Notes") != true || notesState.filteredLabel != null) {
                                notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                                navController.navigate(Destination.Notes()) {
                                    popUpTo<Destination.Notes> { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(id = R.string.projects)) },
                        label = { Text(stringResource(id = R.string.projects)) },
                        selected = currentDestination?.route?.contains("Projects") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Destination.Projects)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = stringResource(id = R.string.archive)) },
                        label = { Text(stringResource(id = R.string.archive)) },
                        selected = currentDestination?.route?.contains("Archive") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Destination.Archive) {
                                popUpTo<Destination.Notes>()
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin)) },
                        label = { Text(stringResource(id = R.string.bin)) },
                        selected = currentDestination?.route?.contains("Bin") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Destination.Bin) {
                                popUpTo<Destination.Notes>()
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) },
                        label = { Text(stringResource(id = R.string.settings)) },
                        selected = currentDestination?.route?.contains("Settings") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Destination.Settings)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = stringResource(id = R.string.reminders)) },
                        label = { Text(stringResource(id = R.string.reminders)) },
                        selected = currentDestination?.route?.contains("Reminder") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Destination.Reminder)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (notesState.labels.isEmpty()) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },
                            label = { Text(stringResource(id = R.string.create_new_label)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Destination.EditLabels)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.labels_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Destination.EditLabels)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(id = R.string.edit_labels),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        notesState.labels.forEach { label ->
                            NavigationDrawerItem(
                                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },
                                label = { Text(label) },
                                selected = notesState.filteredLabel == label,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                                    if (currentDestination?.route?.contains("Notes") != true || notesState.filteredLabel != label) {
                                        navController.navigate(Destination.Notes()) {
                                            popUpTo<Destination.Notes> { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        ) {
            NavHost(navController = navController, startDestination = Destination.Notes(), modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                composable<Destination.Notes>(
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val route: Destination.Notes = backStackEntry.toRoute()
                    val noteId = route.noteId
                    LaunchedEffect(noteId) {
                        if (noteId != -1) {
                            notesViewModel.onEvent(NotesEvent.ExpandNote(noteId))
                        }
                    }
                    NotesScreen(
                        viewModel = notesViewModel,
                        onSettingsClick = { navController.navigate(Destination.Settings) },
                        onArchiveClick = { navController.navigate(Destination.Archive) },
                        onEditLabelsClick = { navController.navigate(Destination.EditLabels) },
                        onBinClick = { navController.navigate(Destination.Bin) },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onScanQrClick = { navController.navigate(Destination.QrScanner) },
                        onTodoClick = { navController.navigate(Destination.Todo) },
                        events = notesViewModel.events
                    )
                }

                composable<Destination.Settings>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    SettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigate = { route -> 
                            when(route) {
                                 "backup" -> navController.navigate(Destination.Backup)
                                 "about" -> navController.navigate(Destination.About)
                                 else -> {}
                             }
                        }
                    )
                }
                composable<Destination.Backup>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    BackupScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Destination.Archive>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ArchiveScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable<Destination.EditLabels>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    EditLabelsScreen(
                        onBackPressed = { navController.popBackStack() }
                    )
                }

                composable<Destination.Bin>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    val binViewModel: BinViewModel = hiltViewModel()
                    BinScreen(
                        viewModel = binViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable<Destination.Reminder>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ReminderScreen(
                        onBackClick = { navController.popBackStack() },
                        onNoteClick = { note ->
                            navController.navigate(Destination.Notes(noteId = note.id)) {
                                popUpTo<Destination.Notes> { inclusive = true }
                            }
                        }
                    )
                }
                composable<Destination.AddEditReminder>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    AddEditReminderScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.Projects>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ProjectScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProjectClick = { projectId ->
                            navController.navigate(Destination.ProjectNotes(projectId))
                        },
                        navController = navController,
                        settingsRepository = settingsRepository
                    )
                }
                composable<Destination.About>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    AboutScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.QrScanner>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    QrScannerScreen(
                        onBackClick = { navController.popBackStack() },
                        onNoteScanned = { title, content ->
                            notesViewModel.onEvent(NotesEvent.CreateNoteFromQr(title, content))
                            navController.popBackStack()
                        }
                    )
                }
                composable<Destination.Todo>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    TodoScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Destination.ProjectNotes>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                ) {
                    ProjectNotesScreen(
                        onBackClick = { navController.popBackStack() },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository
                    )
                }
                composable<Destination.AddEditNote> {
                    val viewModel: ProjectNotesViewModel = hiltViewModel()
                    AddEditNoteScreen(
                        state = viewModel.state.collectAsState().value.toNotesState(),
                        onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                        onDismiss = { navController.popBackStack() },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository,
                        events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed())
                    )
                }
            }
        }
    }
}
