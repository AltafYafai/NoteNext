@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.suvojeet.notenext.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.components.*
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.data.SortType
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import com.suvojeet.notenext.ui.reminder.ReminderSetDialog
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
    onBinClick: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    onDrawingClick: () -> Unit = {},
    onTodoClick: () -> Unit = {},
    events: SharedFlow<NotesUiEvent>
) {
    val listState by viewModel.listState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    var isFabExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
        else -> false
    }

    val isSelectionModeActive = listState.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showMoveToProjectDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentVersion = 16 
        settingsRepository.lastSeenVersion.collect { lastSeen ->
            if (currentVersion > lastSeen) {
                showWhatsNewDialog = true
                settingsRepository.saveLastSeenVersion(currentVersion)
            }
        }
    }

    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            com.suvojeet.notenext.util.BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.send_notes_via))
                    context.startActivity(chooser)
                }

                is NotesUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, context.getString(R.string.link_preview_removed), Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.ProjectCreated -> {
                    Toast.makeText(context, context.getString(R.string.project_created, event.projectName), Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.NavigateToNoteByTitle -> {
                    val noteId = viewModel.getNoteIdByTitle(event.title)
                    if (noteId != null) {
                        viewModel.onEvent(NotesEvent.ExpandNote(noteId as Int))
                    } else {
                        Toast.makeText(context, "Note \"${event.title}\" not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive || isSelectionModeActive || editState.expandedNoteId != null) {
        when {
            isSearchActive -> {
                isSearchActive = false
                focusManager.clearFocus()
            }
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            editState.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    val lazyListState = rememberLazyListState()

    SharedTransitionLayout {
        AnimatedContent(
            targetState = editState.expandedNoteId,
            label = "NoteTransition",
            transitionSpec = {
                val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
                if (targetState != null) {
                    (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = springSpec))
                        .togetherWith(fadeOut(spring()))
                } else {
                    fadeIn(spring())
                        .togetherWith(fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = springSpec))
                }
            }
        ) { expandedId ->
            if (expandedId == null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            AnimatedContent(
                                targetState = isSelectionModeActive,
                                transitionSpec = {
                                    fadeIn(animationSpec = spring()).togetherWith(fadeOut(animationSpec = spring()))
                                },
                                label = "TopAppBar Animation"
                            ) { targetState ->
                                if (targetState) {
                                    ContextualTopAppBar(
                                        selectedItemCount = listState.selectedNoteIds.size,
                                        onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                                        onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                                        onReminderClick = { showReminderSetDialog = true },
                                        onColorClick = { showColorPickerDialog = true },
                                        onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                                        onDeleteClick = { showDeleteDialog = true },
                                        onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                                        onSendClick = { showShareOptionsDialog = true },
                                        onLabelClick = { showLabelDialog = true },
                                        onMoveToProjectClick = { showMoveToProjectDialog = true },
                                        onLockClick = { 
                                            biometricAuthManager?.showBiometricPrompt(
                                                onAuthSuccess = { viewModel.onEvent(NotesEvent.ToggleLockForSelectedNotes) },
                                                onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                            )
                                        },
                                        onSelectAllClick = { viewModel.onEvent(NotesEvent.SelectAllNotes) }
                                    )
                                } else {
                                    TopAppBar(
                                        title = {
                                            SearchBar(
                                                searchQuery = listState.searchQuery,
                                                onSearchQueryChange = { viewModel.onEvent(NotesEvent.OnSearchQueryChange(it)) },
                                                isSearchActive = isSearchActive,
                                                onSearchActiveChange = { isSearchActive = it },
                                                onLayoutToggleClick = { viewModel.onEvent(NotesEvent.ToggleLayout) },
                                                onSortClick = { showSortMenu = true },
                                                layoutType = listState.layoutType,
                                                sortMenuExpanded = showSortMenu,
                                                onSortMenuDismissRequest = { showSortMenu = false },
                                                onSortOptionClick = { sortType ->
                                                    val newSortType = if (sortType == listState.sortType) {
                                                        SortType.DATE_MODIFIED
                                                    } else {
                                                        sortType
                                                    }
                                                    viewModel.onEvent(NotesEvent.SortNotes(newSortType))
                                                },
                                                currentSortType = listState.sortType
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = onMenuClick) {
                                                Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                                            }
                                        },
                                        scrollBehavior = scrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            val isFabScrollExpanded = when (listState.layoutType) {
                                LayoutType.GRID -> gridState.firstVisibleItemIndex == 0
                                LayoutType.LIST -> lazyListState.firstVisibleItemIndex == 0
                            }

                            MultiActionFab(
                                isExpanded = isFabExpanded,
                                onExpandedChange = { isFabExpanded = it },
                                onNoteClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1))
                                    isFabExpanded = false
                                },
                                onChecklistClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1, "CHECKLIST"))
                                    isFabExpanded = false
                                },
                                onProjectClick = {
                                    showCreateProjectDialog = true
                                    isFabExpanded = false
                                },
                                onDrawingClick = {
                                    onDrawingClick()
                                    isFabExpanded = false
                                },
                                onTodoClick = {
                                    onTodoClick()
                                    isFabExpanded = false
                                },
                                themeMode = themeMode,
                                isScrollExpanded = isFabScrollExpanded
                            )
                        }
                    ) { padding ->
                        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                shape = MaterialTheme.shapes.extraLarge,
                                title = { Text(stringResource(id = R.string.move_to_bin_question)) },
                                text = { Text(stringResource(id = R.string.move_to_bin_message, autoDeleteDays)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onEvent(NotesEvent.DeleteSelectedNotes)
                                            showDeleteDialog = false
                                        },
                                        modifier = Modifier.springPress()
                                    ) {
                                        Text(stringResource(id = R.string.move_to_bin))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }, modifier = Modifier.springPress()) {
                                        Text(stringResource(id = R.string.cancel))
                                    }
                                }
                            )
                        }
                        if (showLabelDialog) {
                            LabelDialog(
                                labels = listState.labels,
                                onDismiss = { showLabelDialog = false },
                                onConfirm = { label ->
                                    viewModel.onEvent(NotesEvent.SetLabelForSelectedNotes(label))
                                    showLabelDialog = false
                                }
                            )
                        }
                        if (showReminderSetDialog) {
                            ReminderSetDialog(
                                onDismissRequest = { showReminderSetDialog = false },
                                onConfirm = { date, time, repeatOption ->
                                    viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(date, time, repeatOption))
                                    showReminderSetDialog = false
                                }
                            )
                        }

                        if (showColorPickerDialog) {
                            ColorSelectionDialog(
                                onDismiss = { showColorPickerDialog = false },
                                onColorSelected = { color ->
                                    viewModel.onEvent(NotesEvent.ChangeColorForSelectedNotes(color))
                                    showColorPickerDialog = false
                                },
                                themeMode = themeMode
                            )
                        }

                        if (showCreateProjectDialog) {
                            CreateProjectDialog(
                                onDismiss = { showCreateProjectDialog = false },
                                onConfirm = { projectName ->
                                    viewModel.onEvent(NotesEvent.CreateProject(projectName))
                                    showCreateProjectDialog = false
                                }
                            )
                        }

                        if (showMoveToProjectDialog) {
                            MoveToProjectDialog(
                                projects = listState.projects,
                                onDismiss = { showMoveToProjectDialog = false },
                                onConfirm = { projectId ->
                                    viewModel.onEvent(NotesEvent.MoveSelectedNotesToProject(projectId))
                                    showMoveToProjectDialog = false
                                }
                            )
                        }

                        if (showWhatsNewDialog) {
                            WhatsNewDialog(onDismiss = { showWhatsNewDialog = false })
                        }

                        if (showShareOptionsDialog) {
                            ShareOptionsDialog(
                                onDismiss = { 
                                    showShareOptionsDialog = false 
                                },
                                onShareAsText = {
                                    viewModel.onEvent(NotesEvent.SendSelectedNotes)
                                    showShareOptionsDialog = false
                                }
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val pagedNotes = listState.pagedNotes.collectAsLazyPagingItems()
                            val pinnedNotes = listState.pinnedNotes

                            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val isNotesEmpty = pinnedNotes.isEmpty() && 
                                                 pagedNotes.itemCount == 0 && 
                                                 pagedNotes.loadState.refresh is androidx.paging.LoadState.NotLoading
                                
                                val isLoading = listState.isLoading || pagedNotes.loadState.refresh is androidx.paging.LoadState.Loading

                                if (isLoading) {
                                    ExpressiveLoading()
                                } else if (isNotesEmpty) {
                                    val currentLabel = listState.filteredLabel
                                    val emptyMessage = if (currentLabel != null) {
                                        stringResource(id = R.string.no_notes_found_label, currentLabel)
                                    } else if (listState.searchQuery.isNotEmpty()) {
                                        stringResource(id = R.string.no_notes_found)
                                    } else {
                                        stringResource(id = R.string.no_notes_yet)
                                    }
                                    
                                    val emptyIcon = if (listState.searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Note

                                    EmptyState(
                                        icon = emptyIcon,
                                        message = emptyMessage,
                                        description = if (listState.searchQuery.isEmpty()) stringResource(id = R.string.create_your_first_note) else null
                                    )
                                } else {
                                    val onNoteClickAction: (com.suvojeet.notenext.data.NoteWithAttachments) -> Unit = { note ->
                                        if (isSelectionModeActive) {
                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                        } else {
                                            if (note.note.isLocked) {
                                                try {
                                                    val ivs = note.note.iv?.split(":")
                                                    if (ivs != null && ivs.size == 2) {
                                                        val ivTitle = android.util.Base64.decode(ivs[0], android.util.Base64.DEFAULT)
                                                        val ivContent = android.util.Base64.decode(ivs[1], android.util.Base64.DEFAULT)
                                                        
                                                        val cipherTitle = com.suvojeet.notenext.util.CryptoUtils.getDecryptionCipher(ivTitle, true)
                                                        val cipherContent = com.suvojeet.notenext.util.CryptoUtils.getDecryptionCipher(ivContent, true)
                                                        
                                                        biometricAuthManager?.showBiometricPrompt(
                                                            cryptoObject = androidx.biometric.BiometricPrompt.CryptoObject(cipherTitle),
                                                            onAuthSuccess = { result -> 
                                                                viewModel.onEvent(NotesEvent.ExpandNote(
                                                                    noteId = note.note.id,
                                                                    authenticatedCipherTitle = result.cryptoObject?.cipher,
                                                                    authenticatedCipherContent = cipherContent // We can't bind two ciphers to one prompt, but unlocking one key unlocks all for a duration or we just use the first one. 
                                                                    // Actually, since they use the same key, once the key is unlocked in the KeyStore for this process/duration, the second cipher might work too, 
                                                                    // OR we just decrypt the title with the first one and then use the same key for content.
                                                                )) 
                                                            },
                                                            onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                                        ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        // Legacy or corrupted IV, try without CryptoObject (might fail if key requires it)
                                                        biometricAuthManager?.showBiometricPrompt(
                                                            onAuthSuccess = { viewModel.onEvent(NotesEvent.ExpandNote(note.note.id)) },
                                                            onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error initializing security: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                            }
                                        }
                                    }

                                    when (listState.layoutType) {
                                        LayoutType.GRID -> {
                                            LazyVerticalStaggeredGrid(
                                                columns = StaggeredGridCells.Fixed(2),
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                                state = gridState,
                                                contentPadding = PaddingValues(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalItemSpacing = 8.dp
                                            ) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item(span = StaggeredGridItemSpan.FullLine) {
                                                        Text(
                                                            text = stringResource(id = R.string.pinned),
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    StaggeredGridItems(
                                                        items = pinnedNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        val noteModifier = Modifier.sharedElement(
                                                            rememberSharedContentState(key = "note-${note.note.id}"),
                                                            animatedVisibilityScope = this@AnimatedContent
                                                        )
                                                        NoteItem(
                                                            modifier = noteModifier,
                                                            note = note,
                                                            isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                            searchQuery = listState.searchQuery,
                                                            onNoteClick = { onNoteClickAction(note) },
                                                            onNoteLongClick = {
                                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                            },
                                                            isDarkTheme = isDarkTheme
                                                        )
                                                    }
                                                }

                                                if (pagedNotes.itemCount > 0) {
                                                    if (pinnedNotes.isNotEmpty()) {
                                                        item(span = StaggeredGridItemSpan.FullLine) {
                                                            Text(
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Paged items
                                                    items(
                                                        count = pagedNotes.itemCount,
                                                        key = pagedNotes.itemKey { it.note.id },
                                                        contentType = pagedNotes.itemContentType { it.note.noteType }
                                                    ) { index ->
                                                        pagedNotes[index]?.let { note ->
                                                            val noteModifier = Modifier.sharedElement(
                                                                rememberSharedContentState(key = "note-${note.note.id}"),
                                                                animatedVisibilityScope = this@AnimatedContent
                                                            )
                                                            NoteItem(
                                                                modifier = noteModifier,
                                                                note = note,
                                                                isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                                searchQuery = listState.searchQuery,
                                                                onNoteClick = { onNoteClickAction(note) },
                                                                onNoteLongClick = {
                                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                                },
                                                                isDarkTheme = isDarkTheme
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        LayoutType.LIST -> {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                                state = lazyListState,
                                                contentPadding = PaddingValues(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item {
                                                        Text(
                                                            text = stringResource(id = R.string.pinned),
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    items(
                                                        items = pinnedNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        val noteModifier = Modifier.sharedElement(
                                                            rememberSharedContentState(key = "note-${note.note.id}"),
                                                            animatedVisibilityScope = this@AnimatedContent
                                                        )
                                                        NoteItem(
                                                            modifier = noteModifier,
                                                            note = note,
                                                            isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                            searchQuery = listState.searchQuery,
                                                            onNoteClick = { onNoteClickAction(note) },
                                                            onNoteLongClick = {
                                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                            },
                                                            isDarkTheme = isDarkTheme
                                                        )
                                                    }
                                                }

                                                if (pagedNotes.itemCount > 0) {
                                                    if (pinnedNotes.isNotEmpty()) {
                                                        item {
                                                            Text(
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Paged items
                                                    items(
                                                        count = pagedNotes.itemCount,
                                                        key = pagedNotes.itemKey { it.note.id },
                                                        contentType = pagedNotes.itemContentType { it.note.noteType }
                                                    ) { index ->
                                                        pagedNotes[index]?.let { note ->
                                                            val noteModifier = Modifier.sharedElement(
                                                                rememberSharedContentState(key = "note-${note.note.id}"),
                                                                animatedVisibilityScope = this@AnimatedContent
                                                            )
                                                            NoteItem(
                                                                modifier = noteModifier,
                                                                note = note,
                                                                isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                                searchQuery = listState.searchQuery,
                                                                onNoteClick = { onNoteClickAction(note) },
                                                                onNoteLongClick = {
                                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                                },
                                                                isDarkTheme = isDarkTheme
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isFabExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            onClick = { isFabExpanded = false },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                 AddEditNoteScreen(
                    state = viewModel.state.collectAsState().value,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(NotesEvent.CollapseNote) },
                    themeMode = themeMode,
                    settingsRepository = settingsRepository,
                    events = viewModel.events,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "note-${expandedId}"),
                        animatedVisibilityScope = this@AnimatedContent
                    )
                )
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.create_new_project)) },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(id = R.string.project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(projectName) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.springPress()
            ) {
                Text(stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun MoveToProjectDialog(
    projects: List<com.suvojeet.notenext.data.Project>,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedProject by remember { mutableStateOf<com.suvojeet.notenext.data.Project?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.move_to_project)) },
        text = {
            Column {
                projects.forEach { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProject = project }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProject == project),
                            onClick = { selectedProject = project }
                        )
                        Text(text = project.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProject = null }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedProject == null),
                        onClick = { selectedProject = null }
                    )
                    Text(text = stringResource(id = R.string.none_remove_from_project), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProject?.id) },
                modifier = Modifier.springPress()
            ) {
                Text(stringResource(id = R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.whats_new_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.whats_new_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                WhatsNewItem(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(id = R.string.whats_new_feature_1_title),
                    description = stringResource(id = R.string.whats_new_feature_1_desc),
                    color = MaterialTheme.colorScheme.tertiary
                )

                WhatsNewItem(
                    icon = Icons.Default.CloudUpload,
                    title = stringResource(id = R.string.whats_new_feature_2_title),
                    description = stringResource(id = R.string.whats_new_feature_2_desc),
                    color = MaterialTheme.colorScheme.secondary
                )

                WhatsNewItem(
                    icon = Icons.Default.Security,
                    title = stringResource(id = R.string.whats_new_feature_3_title),
                    description = stringResource(id = R.string.whats_new_feature_3_desc),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().springPress()
            ) {
                Text(stringResource(id = R.string.dismiss))
            }
        }
    )
}

@Composable
private fun WhatsNewItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
