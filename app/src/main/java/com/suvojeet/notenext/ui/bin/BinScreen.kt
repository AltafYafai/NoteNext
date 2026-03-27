@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.bin

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.*
import com.suvojeet.notenext.ui.components.springPress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BinScreen(
    viewModel: BinViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showEmptyBinDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (isSelectionModeActive) {
                    BinContextualTopAppBar(
                        selectedItemCount = state.selectedNoteIds.size,
                        onClearSelection = { viewModel.onEvent(BinEvent.ClearSelection) },
                        onRestoreClick = { viewModel.onEvent(BinEvent.RestoreSelectedNotes) },
                        onDeletePermanentlyClick = { viewModel.onEvent(BinEvent.DeleteSelectedNotesPermanently) }
                    )
                } else {
                    LargeTopAppBar(
                        title = { 
                            Text(
                                text = stringResource(id = R.string.bin_title),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick, modifier = Modifier.springPress()) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                            }
                        },
                        actions = {
                            if (state.notes.isNotEmpty()) {
                                IconButton(onClick = { showEmptyBinDialog = true }, modifier = Modifier.springPress()) {
                                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Empty Bin")
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (state.notes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Delete,
                        message = stringResource(id = R.string.bin_empty_message)
                    )
                } else {
                    ExpressiveSection(
                        title = "Deleted Notes",
                        description = "Notes in the bin will be automatically deleted after ${state.autoDeleteDays} days"
                    ) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp
                        ) {
                            items(
                                items = state.notes,
                                key = { it.note.id },
                                contentType = { it.note.noteType }
                            ) { noteWithAttachments ->
                                NoteItem(
                                    modifier = Modifier.animateItem(),
                                    note = noteWithAttachments,
                                    onNoteClick = {
                                        if (isSelectionModeActive) {
                                            viewModel.onEvent(BinEvent.ToggleNoteSelection(noteWithAttachments.note.id))
                                        } else {
                                            viewModel.onEvent(BinEvent.ExpandNote(noteWithAttachments.note.id))
                                        }
                                    },
                                    onNoteLongClick = { viewModel.onEvent(BinEvent.ToggleNoteSelection(noteWithAttachments.note.id)) },
                                    isSelected = state.selectedNoteIds.contains(noteWithAttachments.note.id),
                                    binnedDaysRemaining = if (noteWithAttachments.note.isBinned) {
                                        val binnedOn = noteWithAttachments.note.binnedOn
                                        if (binnedOn != null) {
                                            val daysSinceBinned = (System.currentTimeMillis() - binnedOn) / (1000 * 60 * 60 * 24)
                                            (state.autoDeleteDays - daysSinceBinned).toInt().coerceAtLeast(0)
                                        } else {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(initialScale = 0.85f, animationSpec = spring()) + fadeIn(animationSpec = spring()),
            exit = scaleOut(targetScale = 0.85f, animationSpec = spring()) + fadeOut(animationSpec = spring())
        ) {
            BinnedNoteScreen(
                state = state,
                onDismiss = { viewModel.onEvent(BinEvent.CollapseNote) }
            )
        }
    }

    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Empty Bin") },
            text = { Text("Are you sure you want to permanently delete all notes in the bin? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.springPress(),
                    onClick = {
                        viewModel.onEvent(BinEvent.EmptyBin)
                        showEmptyBinDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinDialog = false }, modifier = Modifier.springPress()) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BinContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = selectedItemCount,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()).togetherWith(
                        slideOutVertically { it } + fadeOut()
                    )
                },
                label = "SelectedItemCount"
            ) { count ->
                Text(text = stringResource(id = R.string.x_selected, count))
            }
        },
        navigationIcon = {
            AnimatedIconButton(
                onClick = onClearSelection,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.clear_selection)
            )
        },
        actions = {
            AnimatedIconButton(
                onClick = onRestoreClick,
                icon = Icons.Default.Restore,
                contentDescription = stringResource(id = R.string.restore),
                delay = 50
            )
            Box {
                AnimatedIconButton(
                    onClick = { showMenu = !showMenu },
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options),
                    delay = 100
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    AnimatedDropdownItem(
                        text = stringResource(id = R.string.delete_permanently),
                        onClick = {
                            onDeletePermanentlyClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    )
}
