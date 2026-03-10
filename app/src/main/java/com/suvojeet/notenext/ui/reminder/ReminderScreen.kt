package com.suvojeet.notenext.ui.reminder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Note
import java.text.SimpleDateFormat
import java.util.*

import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    onBackClick: () -> Unit,
    onNoteClick: (Note) -> Unit, // Added to navigate to note
    reminderViewModel: ReminderViewModel = hiltViewModel()
) {
    val allReminders by reminderViewModel.allReminders.collectAsState()
    val upcomingReminders by reminderViewModel.upcomingReminders.collectAsState()
    val elapsedReminders by reminderViewModel.elapsedReminders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Upcoming, 2: Elapsed

    val currentList = when (selectedTab) {
        0 -> allReminders
        1 -> upcomingReminders
        2 -> elapsedReminders
        else -> allReminders
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.reminders_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs (Filter Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Upcoming") }
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Elapsed") }
                )
            }

            if (currentList.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    message = "No reminders found"
                )
            } else {
                ExpressiveSection(
                    title = when(selectedTab) {
                        0 -> "All Reminders"
                        1 -> "Upcoming"
                        2 -> "Elapsed"
                        else -> "Reminders"
                    },
                    description = "Manage your time-sensitive notes"
                ) {
                    SettingsGroupCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentList) { note ->
                                ReminderItem(
                                    note = note, 
                                    onClick = { onNoteClick(note) },
                                    onDeleteClick = { reminderViewModel.deleteReminder(note) }
                                )
                                if (currentList.last() != note) {
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
    }
}

@Composable
fun ReminderItem(
    note: Note,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifEmpty { "No Title" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                 maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            note.reminderTime?.let { time ->
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                Text(
                    text = "Next: ${sdf.format(Date(time))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notification Icon and count (mock count 1 for now or use repeat logic)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "1", // Hardcoded 1 as per simple reminder logic
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        // Delete Reminder Icon
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Reminder",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}