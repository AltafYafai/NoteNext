@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
package com.suvojeet.notenext.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.toRoute
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.MainActivity
import com.suvojeet.notenext.ui.archive.ArchiveScreen
import com.suvojeet.notenext.ui.bin.BinScreen
import com.suvojeet.notenext.ui.notes.NotesScreen
import com.suvojeet.notenext.ui.notes.NotesViewModel
import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.reminder.ReminderScreen
import com.suvojeet.notenext.ui.todo.TodoScreen
import com.suvojeet.notenext.ui.project.ProjectScreen
import com.suvojeet.notenext.ui.project.ProjectNotesScreen
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.project.toProjectNotesEvent
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.drawing.DrawingScreen
import com.suvojeet.notenext.ui.settings.GroqSettingsScreen
import com.suvojeet.notenext.ui.settings.BackupScreen
import com.suvojeet.notenext.ui.labels.EditLabelsScreen
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.bin.BinEvent
import com.suvojeet.notenext.ui.reminder.AddEditReminderScreen
import com.suvojeet.notenext.ui.donate.DonationScreen
import com.suvojeet.notenext.ui.credits.CreditsScreen
import com.suvojeet.notenext.ui.changelog.ChangelogScreen
import com.suvojeet.notenext.ui.project.ProjectNotesUiEvent
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.project.ProjectViewModel
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.ui.about.AboutScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isCompact || (currentDestination?.hasRoute<Destination.Notes>() == true),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                drawerTonalElevation = 0.dp,
                windowInsets = WindowInsets.statusBars
            ) {
                DrawerHeader()
                Spacer(modifier = Modifier.height(12.dp))
                DrawerItems(
                    currentDestination = currentDestination,
                    navController = navController,
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        AppNavHost(
            navController = navController,
            notesViewModel = notesViewModel,
            themeMode = themeMode,
            windowSizeClass = windowSizeClass,
            settingsRepository = settingsRepository,
            onMenuClick = { scope.launch { drawerState.open() } },
            isCompact = isCompact
        )
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your digital second brain",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerItems(
    currentDestination: androidx.navigation.NavDestination?,
    navController: NavHostController,
    onCloseDrawer: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        DrawerItem(
            R.string.notes, 
            { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = stringResource(id = R.string.notes)) }, 
            currentDestination?.hasRoute<Destination.Notes>() == true
        ) {
            onCloseDrawer()
            navController.navigate(Destination.Notes()) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        
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
            R.string.bin_title, 
            { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.bin_title)) }, 
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
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
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
    }
}

@Composable
private fun DrawerItem(
    labelRes: Int,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = stringResource(id = labelRes), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        onClick = onClick,
        icon = icon,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Destination.Notes(),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            notesRoute(navController, notesViewModel, themeMode, settingsRepository, onMenuClick, isCompact)
            sharedRoutes(navController, notesViewModel, themeMode, windowSizeClass, settingsRepository, onMenuClick, this@SharedTransitionLayout)
        }
    }
}

private fun NavGraphBuilder.notesRoute(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean
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
                    notesViewModel.onEvent(NotesEvent.ExpandNote(noteId))
                }
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
            onMenuClick = onMenuClick,
            onDrawingClick = { navController.navigate(Destination.Drawing) },
            onTodoClick = { navController.navigate(Destination.Todo) },
            events = notesViewModel.events
        )
    }
}

private fun NavGraphBuilder.sharedRoutes(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
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
        AnimatedVisibility(visible = true) {
            ProjectNotesScreen(
                onBackClick = { navController.popBackStack() },
                themeMode = themeMode,
                settingsRepository = settingsRepository,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@AnimatedVisibility
            )
        }
    }

    composable<Destination.AddEditNote>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        val viewModel: ProjectNotesViewModel = hiltViewModel()
        AnimatedVisibility(visible = true) {
            AddEditNoteScreen(
                state = viewModel.state.collectAsState().value.toNotesState(),
                onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                onDismiss = { navController.popBackStack() },
                themeMode = themeMode,
                settingsRepository = settingsRepository,
                events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed()),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@AnimatedVisibility
            )
        }
    }

    composable<Destination.Drawing>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        DrawingScreen(
            windowSizeClass = windowSizeClass,
            onSave = { uri ->
                notesViewModel.onEvent(NotesEvent.ExpandNote(-1))
                notesViewModel.onEvent(NotesEvent.ImportImage(uri))
                navController.popBackStack()
            },
            onDismiss = { navController.popBackStack() }
        )
    }
}
