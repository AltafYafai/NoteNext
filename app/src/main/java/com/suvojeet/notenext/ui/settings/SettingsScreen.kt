@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.settings

import org.acra.ACRA
import androidx.core.content.ContextCompat
import android.app.ActivityManager
import android.content.Context
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.UpdateChecker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.biometric.BiometricManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import com.suvojeet.notenext.util.LogcatManager
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val selectedThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)
    val enableAppLock by settingsRepository.enableAppLock.collectAsState(initial = false)
    val selectedLanguage by settingsRepository.language.collectAsState(initial = "en")
    val disallowScreenshots by settingsRepository.disallowScreenshots.collectAsState(initial = false)

    var searchQuery by remember { mutableStateOf("") }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var issueDescription by remember { mutableStateOf("") }
    var showImportSourceDialog by remember { mutableStateOf(false) }
    var showKeepInstructionsDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    
    var isLoggingActive by remember { mutableStateOf(LogcatManager.isLogging()) }

    val backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
    val importKeepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupRestoreViewModel.importFromGoogleKeep(it) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val sections = remember(
        selectedThemeMode, autoDeleteDays, enableRichLinkPreview, enableAppLock, 
        selectedLanguage, disallowScreenshots, showBugReportDialog, isLoggingActive
    ) {
        listOf(
            SettingsSectionData(
                title = context.getString(R.string.display_section_title),
                description = "Visuals and language preferences",
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Palette,
                        title = context.getString(R.string.theme),
                        subtitle = selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        iconColor = Color(0xFF2196F3),
                        onClick = { showThemeDialog = true }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Link,
                        title = context.getString(R.string.rich_link_preview),
                        subtitle = context.getString(R.string.rich_link_preview_subtitle),
                        hasSwitch = true,
                        checked = enableRichLinkPreview,
                        iconColor = Color(0xFF03A9F4),
                        onCheckedChange = { scope.launch { settingsRepository.saveEnableRichLinkPreview(it) } }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Language,
                        title = context.getString(R.string.language),
                        subtitle = if (selectedLanguage == "hi") "Hindi (भारत)" else "English (US)",
                        iconColor = Color(0xFF3F51B5),
                        onClick = { showLanguageDialog = true }
                    )
                )
            ),
            SettingsSectionData(
                title = context.getString(R.string.security_section_title),
                description = "Security and data privacy",
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Security,
                        title = context.getString(R.string.app_lock),
                        subtitle = context.getString(R.string.app_lock_subtitle),
                        hasSwitch = true,
                        checked = enableAppLock,
                        iconColor = Color(0xFF4CAF50),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val biometricManager = BiometricManager.from(context)
                                val canAuthenticate = biometricManager.canAuthenticate(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                )
                                
                                when (canAuthenticate) {
                                    BiometricManager.BIOMETRIC_SUCCESS -> {
                                        scope.launch { settingsRepository.saveEnableAppLock(true) }
                                    }
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                        val enrollIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                            android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                                putExtra(
                                                    android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                                    BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                                )
                                            }
                                        } else {
                                            android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                        }
                                        
                                        try {
                                            context.startActivity(enrollIntent)
                                            android.widget.Toast.makeText(context, context.getString(R.string.biometric_setup_required), android.widget.Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Please enable a screen lock or biometrics in system settings.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    else -> {
                                        android.widget.Toast.makeText(context, "Biometric authentication is not available on this device.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                scope.launch { settingsRepository.saveEnableAppLock(false) }
                            }
                        }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Lock,
                        title = "Disallow Screenshots",
                        subtitle = "Protect content from screen capture",
                        hasSwitch = true,
                        checked = disallowScreenshots,
                        iconColor = Color(0xFFF44336),
                        onCheckedChange = { scope.launch { settingsRepository.saveDisallowScreenshots(it) } }
                    )
                )
            ),
            SettingsSectionData(
                title = "Data & Maintenance",
                description = "Backup, imports and cleanup",
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Delete,
                        title = "Auto Cleanup",
                        subtitle = "Clean bin after $autoDeleteDays days",
                        iconColor = Color(0xFF607D8B),
                        onClick = { showAutoDeleteDialog = true }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Backup,
                        title = "Backup & Restore",
                        subtitle = "Cloud and local data management",
                        iconColor = Color(0xFFFF9800),
                        onClick = { onNavigate("backup") }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.ImportExport,
                        title = "Import Notes",
                        subtitle = "Import from Google Keep ZIP",
                        iconColor = Color(0xFFE91E63),
                        onClick = { showImportSourceDialog = true }
                    )
                )
            ),
            SettingsSectionData(
                title = "Support & Logging",
                description = "Information and bug reproduction",
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Info,
                        title = "App Info",
                        subtitle = "v1.2.7 Stable Build",
                        iconColor = Color(0xFF9C27B0),
                        onClick = { onNavigate("about") }
                    ),
                    SettingsItemData(
                        icon = if (isLoggingActive) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                        title = if (isLoggingActive) "Stop Logging" else "Start Logging",
                        subtitle = if (isLoggingActive) "Logging active. Reproduce bug now." else "Record app logs to report bugs",
                        iconColor = if (isLoggingActive) Color(0xFFF44336) else Color(0xFF4CAF50),
                        onClick = {
                            if (isLoggingActive) {
                                val logFile = LogcatManager.stopLogging()
                                if (logFile != null) {
                                    LogcatManager.shareLogFile(context, logFile)
                                    Toast.makeText(context, "Log saved and ready to share", Toast.LENGTH_SHORT).show()
                                }
                                isLoggingActive = false
                            } else {
                                LogcatManager.startLogging(context)
                                isLoggingActive = true
                                Toast.makeText(context, "Logging started. Reproduce the bug now.", Toast.LENGTH_LONG).show()
                            }
                        }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Code,
                        title = "Source Code",
                        subtitle = "Check out our GitHub repository",
                        iconColor = Color(0xFF24292E),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/NoteNext"))
                            context.startActivity(intent)
                        }
                    )
                )
            )
        )
    }

    val filteredSections = remember(searchQuery, sections) {
        if (searchQuery.isEmpty()) sections
        else {
            sections.mapNotNull { section ->
                val filteredItems = section.items.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    (it.subtitle?.contains(searchQuery, ignoreCase = true) == true) 
                }
                if (filteredItems.isNotEmpty()) section.copy(items = filteredItems) else null
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                androidx.compose.material3.SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search settings...", style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { 
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.springPress()) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {}
            }

            if (searchQuery.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeaturedCard(
                            title = "Privacy",
                            subtitle = if (enableAppLock) "Protected" else "Secure now",
                            icon = Icons.Rounded.Security,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { /* Security section logic */ }
                        )
                        FeaturedCard(
                            title = "Sync",
                            subtitle = "Backup Data",
                            icon = Icons.Rounded.CloudUpload,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("backup") }
                        )
                    }
                }
            }

            filteredSections.forEach { section ->
                item {
                    ExpressiveSection(
                        title = section.title,
                        description = section.description
                    ) {
                        SettingsGroupCard {
                            section.items.forEach { item ->
                                SettingsItem(
                                    icon = item.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    hasSwitch = item.hasSwitch,
                                    checked = item.checked,
                                    iconColor = item.iconColor,
                                    onCheckedChange = item.onCheckedChange,
                                    onClick = item.onClick
                                )
                            }
                        }
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                item {
                    ExpressiveSection(
                        title = "Support & Updates",
                        description = "Keep NoteNext running smoothly"
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
                                icon = Icons.Rounded.NewReleases,
                                title = "What's New",
                                subtitle = "View latest changelog",
                                iconColor = Color(0xFFE91E63),
                                onClick = { showChangelogDialog = true }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showThemeDialog) ThemeChooserDialog(selectedThemeMode, { theme -> scope.launch { settingsRepository.saveThemeMode(theme) }; showThemeDialog = false }, { showThemeDialog = false })
    if (showAutoDeleteDialog) AutoDeleteDialog(autoDeleteDays, { days -> scope.launch { settingsRepository.saveAutoDeleteDays(days) }; showAutoDeleteDialog = false }, { showAutoDeleteDialog = false })
    if (showLanguageDialog) LanguageChooserDialog(selectedLanguage, { lang -> scope.launch { settingsRepository.saveLanguage(lang) }; showLanguageDialog = false }, { showLanguageDialog = false })
    if (showImportSourceDialog) ImportSourceDialog({ showImportSourceDialog = false }, { showImportSourceDialog = false; showKeepInstructionsDialog = true })
    if (showKeepInstructionsDialog) KeepInstructionsDialog({ showKeepInstructionsDialog = false }, { showKeepInstructionsDialog = false; importKeepLauncher.launch(arrayOf("application/zip")) })
    if (showRateDialog) RateAppDialog(context) { showRateDialog = false }
    if (showChangelogDialog) ChangelogDialog { showChangelogDialog = false }
    if (showBugReportDialog) BugReportDialog(issueDescription, { issueDescription = it }, { 
        showBugReportDialog = false 
        ACRA.errorReporter.putCustomData("IssueDescription", issueDescription)
        ACRA.errorReporter.handleSilentException(Exception("Manual Bug Report: $issueDescription"))
        issueDescription = ""
    }, { showBugReportDialog = false })
}

private data class SettingsSectionData(
    val title: String,
    val description: String,
    val items: List<SettingsItemData>
)

private data class SettingsItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val hasSwitch: Boolean = false,
    val checked: Boolean = false,
    val iconColor: Color,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

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
            .height(115.dp)
            .springPress()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.8f))
            }
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
            .springPress()
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) onCheckedChange(!checked) else onClick?.invoke()
            },
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconColor)
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
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LanguageChooserDialog(selectedLanguage: String, onLanguageSelected: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.choose_language)) },
        text = {
            Column {
                listOf("en" to R.string.language_english, "hi" to R.string.language_hindi).forEach { (code, resId) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onLanguageSelected(code) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (selectedLanguage == code), onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(id = resId))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
private fun ThemeChooserDialog(selectedThemeMode: ThemeMode, onThemeSelected: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.choose_theme)) },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    ListItem(
                        headlineContent = { Text(if (mode == ThemeMode.AMOLED) stringResource(id = R.string.theme_amoled) else mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingContent = { RadioButton(selected = (mode == selectedThemeMode), onClick = null) },
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onThemeSelected(mode) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
private fun AutoDeleteDialog(currentDays: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var pos by remember { mutableFloatStateOf(currentDays.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.auto_delete_after)) },
        text = {
            Column {
                Text("${pos.roundToInt()} days", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                Slider(value = pos, onValueChange = { pos = it }, valueRange = 1f..60f, steps = 58)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pos.roundToInt()) }, modifier = Modifier.springPress()) { Text(stringResource(id = R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
private fun CheckForUpdateItem(context: android.content.Context) {
    var isChecking by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateChecker.UpdateResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val updateChecker = remember { UpdateChecker(context) }
    val scope = rememberCoroutineScope()
    
    val currentVersionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown" }
        catch (e: Exception) { "Unknown" }
    }

    SettingsItem(
        icon = Icons.Rounded.Update,
        title = stringResource(R.string.check_for_updates),
        subtitle = if (isChecking) stringResource(R.string.checking_for_updates) else if (errorMessage != null) errorMessage!! else "Current: v$currentVersionName",
        iconColor = Color(0xFF2196F3),
        onClick = {
            if (isChecking) return@SettingsItem
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
        }
    )
    
    if (showResultDialog && updateResult != null) {
        val result = updateResult!!
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(if (result.isUpdateAvailable) stringResource(R.string.update_available) else stringResource(R.string.you_are_up_to_date)) },
            text = { 
                Column {
                    Text(if (result.isUpdateAvailable) 
                        "A new version of NoteNext is available on the Play Store." 
                        else "You're running the latest version of NoteNext.")
                    
                    if (result.isUpdateAvailable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("New Version: ${result.availableVersionCode}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (result.isUpdateAvailable) {
                    Button(onClick = {
                        showResultDialog = false
                        (context as? android.app.Activity)?.let { updateChecker.startUpdate(it) }
                    }, modifier = Modifier.springPress()) { Text(stringResource(R.string.update_now)) }
                } else {
                    TextButton(onClick = { showResultDialog = false }, modifier = Modifier.springPress()) { Text(stringResource(R.string.ok)) }
                }
            },
            dismissButton = if (result.isUpdateAvailable) { { TextButton(onClick = { showResultDialog = false }, modifier = Modifier.springPress()) { Text(stringResource(R.string.cancel)) } } } else null
        )
    }
}

@Composable
fun ImportSourceDialog(onDismiss: () -> Unit, onSelectKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Import from...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportOptionItem("Google Keep", Icons.Rounded.Description, Color(0xFFFFBB00), onSelectKeep)
                ImportOptionItem("Evernote", Icons.Rounded.Description, Color(0xFF00A82D), enabled = false)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text("Cancel") } }
    )
}

@Composable
fun ImportOptionItem(text: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}, enabled: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
    }
}

@Composable
fun KeepInstructionsDialog(onDismiss: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Google Keep Import") },
        text = { Text("Please select your Google Takeout ZIP file to import your Keep notes.") },
        confirmButton = { TextButton(onClick = onImport, modifier = Modifier.springPress()) { Text("Select ZIP") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text("Cancel") } }
    )
}

@Composable
private fun BugReportDialog(desc: String, onDescChange: (String) -> Unit, onSend: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Bug Report") },
        text = { 
            Column {
                Text("Describe the issue you're facing. System logs will be attached automatically.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = onDescChange, label = { Text("Issue description") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraSmall) 
            }
        },
        confirmButton = { TextButton(onClick = onSend, modifier = Modifier.springPress()) { Text("Send Report") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text("Cancel") } }
    )
}

@Composable
fun RateAppDialog(context: android.content.Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Rate NoteNext") },
        text = { Text("Loving the app? Help us by rating it on the Play Store!") },
        confirmButton = { TextButton(onClick = { 
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))) }
            catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))) }
            onDismiss()
        }, modifier = Modifier.springPress()) { Text("Rate Now") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { Text("Later") } }
    )
}
