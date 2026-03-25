@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.navigation

import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.suvojeet.notenext.ui.archive.ArchiveScreen
import com.suvojeet.notenext.ui.bin.BinScreen
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.labels.EditLabelsScreen
import com.suvojeet.notenext.ui.notes.NotesEditEvent
import com.suvojeet.notenext.ui.notes.NotesListEvent
import com.suvojeet.notenext.ui.notes.NotesListViewModel
import com.suvojeet.notenext.ui.notes.NotesEditViewModel
import com.suvojeet.notenext.ui.notes.NotesScreen
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.ui.notes.NotesListState
import com.suvojeet.notenext.ui.notes.NotesEditState
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.reminder.ReminderScreen
import com.suvojeet.notenext.ui.reminder.AddEditReminderScreen
import com.suvojeet.notenext.ui.settings.AboutScreen
import com.suvojeet.notenext.ui.settings.CreditsScreen
import com.suvojeet.notenext.ui.settings.ChangelogScreen
import com.suvojeet.notenext.ui.settings.GroqSettingsScreen
import com.suvojeet.notenext.ui.donate.DonationScreen
import com.suvojeet.notenext.ui.project.ProjectScreen
import com.suvojeet.notenext.ui.project.ProjectViewModel
import com.suvojeet.notenext.ui.project.ProjectNotesScreen
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.project.toNotesEditState
import com.suvojeet.notenext.ui.project.toProjectNotesEvent
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.settings.BackupScreen
import com.suvojeet.notenext.ui.todo.TodoScreen

import com.suvojeet.notenext.ui.drawing.DrawingScreen

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.suvojeet.notenext.R
import kotlinx.coroutines.flow.shareIn
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.navigation.toRoute
import com.suvojeet.notenext.ui.components.springPress
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun NavGraph(
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    startNoteId: Int = -1,
    startAddNote: Boolean = false,
    sharedText: String? = null,
    initialTitle: String? = null,
    searchQuery: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listViewModel: NotesListViewModel = hiltViewModel()
    val editViewModel: NotesEditViewModel = hiltViewModel()
    val listState by listViewModel.listState.collectAsState()
    val editState by editViewModel.editState.collectAsState()

    val combinedEvents = remember(listViewModel, editViewModel, scope) {
        merge(listViewModel.events, editViewModel.events)
            .shareIn(scope, SharingStarted.WhileSubscribed(5000))
    }

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
            val isLocked = editViewModel.getNoteLockStatus(startNoteId)
            if (isLocked) {
                biometricAuthManager?.showBiometricPrompt(
                    onAuthSuccess = {
                        editViewModel.onEvent(NotesEditEvent.ExpandNote(startNoteId))
                        navController.navigate(Destination.Notes()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onAuthError = {
                        Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
            } else {
                editViewModel.onEvent(NotesEditEvent.ExpandNote(startNoteId))
                navController.navigate(Destination.Notes()) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(startAddNote) {
        if (startAddNote) {
            editViewModel.onEvent(NotesEditEvent.ExpandNote(-1))
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            editViewModel.onEvent(NotesEditEvent.CreateNoteFromSharedText(sharedText))
        }
    }

    LaunchedEffect(initialTitle) {
        if (initialTitle != null) {
            editViewModel.onEvent(NotesEditEvent.SetInitialTitle(initialTitle))
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != null) {
            listViewModel.onEvent(NotesListEvent.OnSearchQueryChange(searchQuery))
        }
    }

    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.15f),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerShape = MaterialTheme.shapes.extraLarge
                ) {
                    DrawerContent(
                        navController = navController,
                        listState = listState,
                        editState = editState,
                        listViewModel = listViewModel,
                        editViewModel = editViewModel,
                        onCloseDrawer = { /* no-op for permanent drawer */ }
                    )

                }
            }
        ) {
            AppNavHost(
                navController = navController,
                listViewModel = listViewModel,
                editViewModel = editViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = false,
                combinedEvents = combinedEvents
            )

        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = editState.expandedNoteId == null,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
                    DrawerContent(
                        navController = navController,
                        listState = listState,
                        editState = editState,
                        listViewModel = listViewModel,
                        editViewModel = editViewModel,
                        onCloseDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            AppNavHost(
                navController = navController,
                listViewModel = listViewModel,
                editViewModel = editViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = true,
                combinedEvents = combinedEvents
            )

        }
    }
}

// ─── Shared drawer content ───────────────────────────────────────────

@Composable
private fun DrawerContent(
    navController: NavHostController,
    listState: NotesListState,
    editState: NotesEditState,
    listViewModel: NotesListViewModel,
    editViewModel: NotesEditViewModel,
    onCloseDrawer: () -> Unit
) {

    Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(24.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    data class DrawerItem(val label: Int, val icon: @Composable () -> Unit, val isSelected: Boolean, val onClick: () -> Unit)

    val items = listOf(
        DrawerItem(
            R.string.notes, 
            { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.notes)) }, 
            currentDestination?.hasRoute<Destination.Notes>() == true && listState.filteredLabel == null
        ) {
            onCloseDrawer()
            if (currentDestination?.hasRoute<Destination.Notes>() != true || listState.filteredLabel != null) {
                listViewModel.onEvent(NotesListEvent.FilterByLabel(null))
                navController.navigate(Destination.Notes()) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.projects, 
            { Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(id = R.string.projects)) }, 
            currentDestination?.hasRoute<Destination.Projects>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Projects) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        DrawerItem(
            R.string.archive, 
            { Icon(Icons.Default.Archive, contentDescription = stringResource(id = R.string.archive)) }, 
            currentDestination?.hasRoute<Destination.Archive>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Archive) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        DrawerItem(
            R.string.reminders, 
            { Icon(Icons.Default.Notifications, contentDescription = stringResource(id = R.string.reminders)) }, 
            currentDestination?.hasRoute<Destination.Reminder>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Reminder) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        DrawerItem(
            R.string.todos, 
            { Icon(Icons.Default.PlaylistAddCheck, contentDescription = stringResource(id = R.string.todos)) }, 
            currentDestination?.hasRoute<Destination.Todo>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Todo) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        DrawerItem(
            R.string.bin, 
            { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin)) }, 
            currentDestination?.hasRoute<Destination.Bin>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Bin) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        DrawerItem(
            R.string.settings, 
            { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) }, 
            currentDestination?.hasRoute<Destination.Settings>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Settings) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )

    items.forEach { item ->
        NavigationDrawerItem(
            icon = { item.icon() },
            label = { Text(stringResource(id = item.label), fontWeight = FontWeight.Bold) },
            selected = item.isSelected,
            onClick = item.onClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

    if (listState.labels.isEmpty()) {
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },
            label = { Text(stringResource(id = R.string.create_new_label), fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                onCloseDrawer()
                navController.navigate(Destination.EditLabels) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.labels_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = {
                onCloseDrawer()
                navController.navigate(Destination.EditLabels) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }, modifier = Modifier.size(24.dp).springPress()) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_labels),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        listState.labels.forEach { label ->
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },
                label = { Text(label, fontWeight = FontWeight.Medium) },
                selected = listState.filteredLabel == label,
                onClick = {
                    onCloseDrawer()
                    listViewModel.onEvent(NotesListEvent.FilterByLabel(label))
                    if (currentDestination?.hasRoute<Destination.Notes>() != true || listState.filteredLabel != label) {
                        navController.navigate(Destination.Notes()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).springPress()
            )
        }
    }
}

// ─── Shared NavHost ──────────────────────────────────────────────────

@Composable
private fun AppNavHost(
    navController: NavHostController,
    listViewModel: NotesListViewModel,
    editViewModel: NotesEditViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean,
    combinedEvents: SharedFlow<NotesUiEvent>
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Notes(),
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        notesRoute(navController, listViewModel, editViewModel, themeMode, settingsRepository, onMenuClick, isCompact, combinedEvents)
        sharedRoutes(navController, listViewModel, editViewModel, themeMode, windowSizeClass, settingsRepository, onMenuClick)
    }
}

private fun NavGraphBuilder.notesRoute(
    navController: NavHostController,
    listViewModel: NotesListViewModel,
    editViewModel: NotesEditViewModel,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean,
    combinedEvents: SharedFlow<NotesUiEvent>
) {
    composable<Destination.Notes>(
        enterTransition = { fadeIn(animationSpec = spring()) },
        exitTransition = { fadeOut(animationSpec = spring()) }
    ) { backStackEntry ->
        if (isCompact) {
            val route: Destination.Notes = backStackEntry.toRoute()
            val noteId = route.noteId
            LaunchedEffect(noteId) {
                if (noteId != -1) {
                    editViewModel.onEvent(NotesEditEvent.ExpandNote(noteId))
                }
            }
        }
        NotesScreen(
            listViewModel = listViewModel,
            editViewModel = editViewModel,
            onSettingsClick = { navController.navigate(Destination.Settings) },
            onArchiveClick = { navController.navigate(Destination.Archive) },
            onEditLabelsClick = { navController.navigate(Destination.EditLabels) },
            onBinClick = { navController.navigate(Destination.Bin) },
            themeMode = themeMode,
            settingsRepository = settingsRepository,
            onMenuClick = onMenuClick,
            onDrawingClick = { navController.navigate(Destination.Drawing) },
            onTodoClick = { navController.navigate(Destination.Todo) },
            events = combinedEvents
        )
    }
}

private fun NavGraphBuilder.sharedRoutes(
    navController: NavHostController,
    listViewModel: NotesListViewModel,
    editViewModel: NotesEditViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit
) {
    val slideEnter = slideInHorizontally(initialOffsetX = { it }, animationSpec = spring()) + fadeIn(spring())
    val slideExit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring()) + fadeOut(spring())

    composable<Destination.Settings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        SettingsScreen(
            onBackClick = { navController.popBackStack() },
            onNavigate = { route ->
                when(route) {
                    "backup" -> navController.navigate(Destination.Backup)
                    "about" -> navController.navigate(Destination.About)
                    "donate" -> navController.navigate(Destination.Donate)
                    "changelog" -> navController.navigate(Destination.Changelog)
                    "credits" -> navController.navigate(Destination.Credits)
                    "groq" -> navController.navigate(Destination.GroqSettings)
                    else -> {}
                }
            }
        )
    }

    composable<Destination.GroqSettings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        GroqSettingsScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Backup>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        BackupScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Archive>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ArchiveScreen(onMenuClick = onMenuClick)
    }

    composable<Destination.EditLabels>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        EditLabelsScreen(onBackPressed = { navController.popBackStack() })
    }

    composable<Destination.Bin>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        val binViewModel: BinViewModel = hiltViewModel()
        BinScreen(viewModel = binViewModel, onMenuClick = onMenuClick)
    }

    composable<Destination.Reminder>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
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
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AddEditReminderScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Projects>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ProjectScreen(
            onMenuClick = onMenuClick,
            onProjectClick = { projectId -> navController.navigate(Destination.ProjectNotes(projectId)) },
            navController = navController,
            settingsRepository = settingsRepository
        )
    }

    composable<Destination.About>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AboutScreen(
            onBackClick = { navController.popBackStack() },
            onDonateClick = { navController.navigate(Destination.Donate) },
            onCreditsClick = { navController.navigate(Destination.Credits) },
            onChangelogClick = { navController.navigate(Destination.Changelog) }
        )
    }

    composable<Destination.Credits>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        CreditsScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Changelog>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ChangelogScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Donate>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        DonationScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Todo>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        TodoScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.ProjectNotes>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ProjectNotesScreen(
            onBackClick = { navController.popBackStack() },
            themeMode = themeMode,
            settingsRepository = settingsRepository
        )
    }

    composable<Destination.AddEditNote>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        val viewModel: ProjectNotesViewModel = hiltViewModel()
        AddEditNoteScreen(
            state = viewModel.state.collectAsState().value.toNotesEditState(),
            labels = viewModel.state.collectAsState().value.labels,
            onEvent = { event ->
                viewModel.onEvent(event.toProjectNotesEvent())
            },
            onDismiss = { navController.popBackStack() },
            themeMode = themeMode,
            settingsRepository = settingsRepository,
            events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed())
        )
    }

    composable<Destination.Drawing>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        DrawingScreen(
            windowSizeClass = windowSizeClass,
            onSave = { uri ->
                editViewModel.onEvent(NotesEditEvent.ExpandNote(-1))
                editViewModel.onEvent(NotesEditEvent.ImportImage(uri))
                navController.popBackStack()
            },
            onDismiss = { navController.popBackStack() }
        )
    }

}
