@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
package com.suvojeet.notenext.ui.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.data.NoteSummary

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var noteToRestore by remember { mutableStateOf<NoteSummary?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(id = R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (state.notes.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Archive,
                    message = stringResource(id = R.string.no_archived_notes)
                )
            } else {
                ExpressiveSection(
                    title = "Archived Notes",
                    description = "Notes you've put away for safekeeping"
                ) {
                    SharedTransitionLayout {
                        AnimatedVisibility(visible = true) {
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
                                        isSelected = false,
                                        onNoteClick = {
                                            noteToRestore = noteWithAttachments.note
                                            showRestoreDialog = true
                                        },
                                        onNoteLongClick = { /* Handle long click if needed */ },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedVisibility
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRestoreDialog && noteToRestore != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreDialog = false
                    noteToRestore = null
                },
                shape = MaterialTheme.shapes.extraLarge,
                title = { Text(stringResource(id = R.string.restore_note_title)) },
                text = { Text(stringResource(id = R.string.restore_note_confirmation)) },
                confirmButton = {
                    TextButton(onClick = {
                        noteToRestore?.let { viewModel.onEvent(ArchiveEvent.UnarchiveNote(it)) }
                        showRestoreDialog = false
                        noteToRestore = null
                    }) {
                        Text(stringResource(id = R.string.restore))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreDialog = false
                        noteToRestore = null
                    }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }
    }
}
