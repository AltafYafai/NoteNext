package com.suvojeet.notenext.ui.settings

import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.height
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.suvojeet.notenext.data.backup.GoogleDriveManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R

import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.BugReport
import com.suvojeet.notenext.core.util.LogCollector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    // -- State Collection --
    val selectedThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)
    val enableAppLock by settingsRepository.enableAppLock.collectAsState(initial = false)
    val selectedLanguage by settingsRepository.language.collectAsState(initial = "en")
    val disallowScreenshots by settingsRepository.disallowScreenshots.collectAsState(initial = false)

    // -- Search State --
    var searchQuery by remember { mutableStateOf("") }

    // -- Dialog States --
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppLockInfoDialog by remember { mutableStateOf(false) }
    var showScreenshotInfoDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var isLoggingActive by remember { mutableStateOf(false) }
    var showLoggingDialog by remember { mutableStateOf(false) }
    var issueDescription by remember { mutableStateOf("") }
    var showImportSourceDialog by remember { mutableStateOf(false) }
    var showKeepInstructionsDialog by remember { mutableStateOf(false) }

    val backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
    val importKeepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupRestoreViewModel.importFromGoogleKeep(it) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.displaySmall, // Expressive Typography
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Search Section
            item {
                androidx.compose.material3.SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search settings...", style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {}
            }

            // Featured "Hero" Moments
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeaturedCard(
                        title = "App Lock",
                        subtitle = if (enableAppLock) "Active" else "Off",
                        icon = Icons.Rounded.Security,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { /* App Lock Logic handled by Switch in Security section, but this is a shortcut */ }
                    )
                    FeaturedCard(
                        title = "Backup",
                        subtitle = "Cloud Sync",
                        icon = Icons.Rounded.Backup,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("backup") }
                    )
                }
            }

            // Visual Section: Appearance
            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.display_section_title),
                    description = "Personalize your visual experience"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(id = R.string.theme),
                            subtitle = selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { showThemeDialog = true }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.Language,
                            title = stringResource(id = R.string.language),
                            subtitle = if (selectedLanguage == "hi") "Hindi (भारत)" else "English (US)",
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }

            // Security & Integrity
            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.security_section_title),
                    description = "Safety and data protection"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Security,
                            title = stringResource(id = R.string.app_lock),
                            subtitle = stringResource(id = R.string.app_lock_subtitle),
                            hasSwitch = true,
                            checked = enableAppLock,
                            iconColor = Color(0xFF4CAF50),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val biometricManager = androidx.biometric.BiometricManager.from(context)
                                    val canAuthenticate = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                    if (canAuthenticate == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                                        scope.launch { settingsRepository.saveEnableAppLock(true) }
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.biometric_setup_required), android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    scope.launch { settingsRepository.saveEnableAppLock(false) }
                                }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.Lock,
                            title = "Privacy Protection",
                            subtitle = "Disallow screenshots in app",
                            hasSwitch = true,
                            checked = disallowScreenshots,
                            iconColor = MaterialTheme.colorScheme.error,
                            onCheckedChange = { scope.launch { settingsRepository.saveDisallowScreenshots(it) } }
                        )
                    }
                }
            }

            // System & Data
            item {
                ExpressiveSection(
                    title = "System & Utilities",
                    description = "Maintenance and imports"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Delete,
                            title = "Auto Cleanup",
                            subtitle = "Bin clears after $autoDeleteDays days",
                            iconColor = MaterialTheme.colorScheme.secondary,
                            onClick = { showAutoDeleteDialog = true }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.ImportExport,
                            title = "Data Portability",
                            subtitle = "Import from Google Keep",
                            iconColor = Color(0xFFE91E63),
                            onClick = { showImportSourceDialog = true }
                        )
                    }
                }
            }

            // Community & Support
            item {
                ExpressiveSection(
                    title = "NoteNext Community",
                    description = "Help us improve and grow"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Star,
                            title = "Rate NoteNext",
                            subtitle = "Show some love on Play Store",
                            iconColor = Color(0xFFFFC107),
                            onClick = { showRateDialog = true }
                        )
                        CheckForUpdateItem(context = context)
                        SettingsItem(
                            icon = Icons.Rounded.BugReport,
                            title = "Technical Logging",
                            subtitle = if (isLoggingActive) "Logging active..." else "Help debug issues",
                            iconColor = if (isLoggingActive) Color.Red else Color(0xFF607D8B),
                            onClick = {
                                if (isLoggingActive) {
                                    val reportText = "Issue: $issueDescription\n\n${LogCollector.collectDeviceInfo(context)}\n\nLogs:\n${LogCollector.collectLogs()}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "NoteNext Debug Logs")
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Logs"))
                                    isLoggingActive = false
                                } else {
                                    showLoggingDialog = true
                                }
                            }
                        )
                    }
                }
            }

            // Footer / Legal
            item {
                SettingsGroupCard(modifier = Modifier.padding(top = 8.dp)) {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "About NoteNext",
                        subtitle = "v1.2.6 (Production Build)",
                        iconColor = MaterialTheme.colorScheme.outline,
                        onClick = { onNavigate("about") }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Code,
                        title = "Open Source",
                        subtitle = "Check us on GitHub",
                        iconColor = Color(0xFF24292E),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/NoteNext"))
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Dialogs (Unchanged implementation but cleaned up trigger) ---
    if (showThemeDialog) ThemeChooserDialog(selectedThemeMode, { theme -> scope.launch { settingsRepository.saveThemeMode(theme) }; showThemeDialog = false }, { showThemeDialog = false })
    if (showAutoDeleteDialog) AutoDeleteDialog(autoDeleteDays, { days -> scope.launch { settingsRepository.saveAutoDeleteDays(days) }; showAutoDeleteDialog = false }, { showAutoDeleteDialog = false })
    if (showLanguageDialog) LanguageChooserDialog(selectedLanguage, { lang -> scope.launch { settingsRepository.saveLanguage(lang) }; showLanguageDialog = false }, { showLanguageDialog = false })
    if (showImportSourceDialog) ImportSourceDialog({ showImportSourceDialog = false }, { showImportSourceDialog = false; showKeepInstructionsDialog = true })
    if (showKeepInstructionsDialog) KeepInstructionsDialog({ showKeepInstructionsDialog = false }, { showKeepInstructionsDialog = false; importKeepLauncher.launch(arrayOf("application/zip")) })
    if (showRateDialog) RateAppDialog(context) { showRateDialog = false }
    if (showChangelogDialog) ChangelogDialog { showChangelogDialog = false }
    if (showLoggingDialog) StartLoggingDialog(issueDescription, { issueDescription = it }, { isLoggingActive = true; showLoggingDialog = false }, { showLoggingDialog = false })
}

@Composable
private fun ExpressiveSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun FeaturedCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    alpha = 0.8f
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // Extra Large Rounded Corners
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    hasSwitch: Boolean = false,
    checked: Boolean = false,
    iconColor: Color,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) {
                    onCheckedChange(!checked)
                } else {
                    onClick?.invoke()
                }
            },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
        },
        trailingContent = {
            if (hasSwitch && onCheckedChange != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = iconColor
                    )
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ... rest of the helper dialog components (unchanged from previous valid logic)

    checked: Boolean = false,
    iconColor: Color,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) {
                    onCheckedChange(!checked)
                } else {
                    onClick?.invoke()
                }
            },
        headlineContent = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            ) 
        },
        supportingContent = if (subtitle != null) {
            { 
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            }
        } else null,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Rounded.Info, 
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (hasSwitch && onCheckedChange != null) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = iconColor,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else if (onClick != null) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

// --- Dialogs remain mostly the same, just re-included ---

@Composable
private fun LanguageChooserDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_language)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLanguageSelected("en") }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (selectedLanguage == "en"), onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_english))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLanguageSelected("hi") }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (selectedLanguage == "hi"), onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_hindi))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}



@Composable
private fun ThemeChooserDialog(
    selectedThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_theme)) },
        text = {
            Column {
                ThemeMode.values().forEach { themeMode ->
                    ListItem(
                        headlineContent = {
                            Text(
                                when (themeMode) {
                                    ThemeMode.AMOLED -> stringResource(id = R.string.theme_amoled)
                                    else -> themeMode.name.lowercase().replaceFirstChar { it.uppercase() }
                                }
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = (themeMode == selectedThemeMode),
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(themeMode) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun AutoDeleteDialog(
    currentDays: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentDays.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.auto_delete_after)) },
        text = {
            Column {
                Text(
                    text = "${sliderPosition.roundToInt()} days",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..60f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderPosition.roundToInt()) }) {
                Text(stringResource(id = R.string.save))
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
fun ImportSourceDialog(onDismiss: () -> Unit, onSelectKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose which app to import from") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportOptionItem(
                    text = "Google Keep",
                    icon = Icons.Rounded.Description, // Using generic Description icon
                    color = Color(0xFFFFBB00), // Keep Yellow
                    onClick = onSelectKeep
                )
                ImportOptionItem(
                    text = "Evernote",
                    icon = Icons.Rounded.Description,
                    color = Color(0xFF00A82D), // Evernote Green
                    enabled = false
                )
                ImportOptionItem(
                    text = "Markdown/Plain Text Files",
                    icon = Icons.Rounded.Description,
                    enabled = false
                )
                ImportOptionItem(
                    text = "JSON Files",
                    icon = Icons.Rounded.Description, // Use Code or DataObject if available
                    enabled = false
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportOptionItem(
    text: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}

@Composable
fun KeepInstructionsDialog(onDismiss: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Google Keep") },
        text = {
            Column {
                Text("In order to import your Notes from Google Keep you must download your Google Takeout ZIP file.")
                Spacer(Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://takeout.google.com/"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Open Google Takeout", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))
                // Using Description icon as a placeholder for Info if needed, but text is fine.
                Text("Only select the \"Keep\" data. Click Help to get more information.")
                Spacer(Modifier.height(16.dp))
                Text("If you already have a Takeout ZIP file, click Import and choose the ZIP file.")
            }
        },
        confirmButton = {
            TextButton(onClick = onImport) { Text("Import") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://support.google.com/keep/answer/10017039"))
                    context.startActivity(intent)
                }) { Text("Help") }
            }
        }
    )
}

@Composable
private fun CheckForUpdateItem(context: android.content.Context) {
    var isChecking by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<com.suvojeet.notenext.util.UpdateChecker.UpdateResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val updateChecker = remember { com.suvojeet.notenext.util.UpdateChecker(context) }
    val scope = rememberCoroutineScope()
    
    // Get current version  
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Check for Updates")
                if (isChecking) {
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        supportingContent = { 
            Text(
                if (errorMessage != null) errorMessage!! 
                else "Current: v$currentVersionName"
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        modifier = Modifier.clickable(enabled = !isChecking) {
            isChecking = true
            errorMessage = null
            scope.launch {
                updateChecker.checkForUpdate()
                    .onSuccess { result ->
                        updateResult = result
                        showResultDialog = true
                        isChecking = false
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Check failed"
                        isChecking = false
                    }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    
    // Result Dialog
    if (showResultDialog && updateResult != null) {
        val result = updateResult!!
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            icon = {
                Icon(
                    imageVector = if (result.isUpdateAvailable) 
                        Icons.Rounded.Info 
                    else 
                        Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (result.isUpdateAvailable) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (result.isUpdateAvailable) "Update Available!" else "You're Up to Date!",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (result.isUpdateAvailable) {
                        Text(
                            text = "A new version is available on the Play Store.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New version code: ${result.availableVersionCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        result.stalenessDays?.let { days ->
                            if (days > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Released $days days ago",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "You're running the latest version of NoteNext!",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (result.isUpdateAvailable) {
                    androidx.compose.material3.Button(
                        onClick = {
                            showResultDialog = false
                            val activity = context as? android.app.Activity
                            activity?.let { updateChecker.openPlayStore(it) }
                        }
                    ) {
                        Text("Update Now")
                    }
                } else {
                    TextButton(onClick = { showResultDialog = false }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (result.isUpdateAvailable) {
                    TextButton(onClick = { showResultDialog = false }) {
                        Text("Later")
                    }
                }
            }
        )
    }
}