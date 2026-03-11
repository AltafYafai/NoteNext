@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.add_edit_note.components.*
import com.suvojeet.notenext.ui.components.AiThinkingIndicator
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.reminder.ReminderSheetContent
import com.suvojeet.notenext.data.RepeatOption
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class ImageViewerData(val uri: Uri, val tempId: String)

@Composable
fun AddEditNoteScreen(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    events: SharedFlow<NotesUiEvent>,
    modifier: Modifier = Modifier
) {
    // Local UI State
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val colorPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showFormatBar by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showSlashCommandSheet by remember { mutableStateOf(false) }
    val slashCommandSheetState = rememberModalBottomSheetState()

    var showMoreOptions by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageData by remember { mutableStateOf<ImageViewerData?>(null) }
    var isFocusMode by remember { mutableStateOf(false) }

    var clickedUrl by remember { mutableStateOf<String?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)

    // Auto-save on background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onEvent(NotesEvent.AutoSaveNote)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Logic
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
             if (isGranted) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                     if (alarmManager.canScheduleExactAlarms()) {
                         showReminderDialog = true
                     } else {
                         showExactAlarmDialog = true
                     }
                 } else {
                     showReminderDialog = true
                 }
             } else {
                 Toast.makeText(context, "Notifications are required for reminders", Toast.LENGTH_LONG).show()
             }
        }
    )

    val checkAndRequestReminderPermissions: () -> Unit = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                     if (alarmManager.canScheduleExactAlarms()) {
                         showReminderDialog = true
                     } else {
                         showExactAlarmDialog = true
                     }
                 } else {
                     showReminderDialog = true
                 }
             } else {
                 notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
             }
        } else {
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                 val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                 if (alarmManager.canScheduleExactAlarms()) {
                     showReminderDialog = true
                 } else {
                     showExactAlarmDialog = true
                 }
             } else {
                 showReminderDialog = true
             }
        }
    }

    // Image/Photo Pickers
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            onEvent(NotesEvent.ImportImage(uri))
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                onEvent(NotesEvent.ImportImage(uri))
            }
        }
    }

    BackHandler {
        if (showImageViewer) {
            showImageViewer = false
        } else if (isFocusMode) {
            isFocusMode = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, "Link preview removed", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    // Theme calculations
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
        else -> false
    }
    val colors = NoteGradients.getNoteColors(isDarkTheme)
    val adaptiveColor = NoteGradients.getAdaptiveColor(state.editingColor, isDarkTheme)
    val backgroundColor = if (adaptiveColor != 0) Color(adaptiveColor) else MaterialTheme.colorScheme.surface
    val contentColor = if (adaptiveColor != 0) NoteGradients.getContentColor(adaptiveColor) else MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = backgroundColor,
            topBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(spring()),
                    exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut(spring())
                ) {
                    AddEditNoteTopAppBar(
                        state = state,
                        onEvent = onEvent,
                        onDismiss = onDismiss,
                        showDeleteDialog = { showDeleteDialog = it },
                        editingNoteType = state.editingNoteType,
                        onToggleFocusMode = { isFocusMode = !isFocusMode },
                        isFocusMode = isFocusMode,

                        backgroundColor = backgroundColor,
                        contentColor = contentColor
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(spring()),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut(spring())
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp
                    ) {
                        AddEditNoteBottomAppBar(
                            state = state,
                            onEvent = onEvent,
                            showColorPicker = { showColorPicker = !showColorPicker },
                            showFormatBar = { showFormatBar = !showFormatBar },
                            showReminderDialog = { 
                                if (it) checkAndRequestReminderPermissions() else showReminderDialog = false 
                            },
                            showMoreOptions = { showMoreOptions = it },
                            onImageClick = { getContent.launch("image/*") },
                            onTakePhotoClick = {
                                try {
                                    val uri = createImageFile(context)
                                    photoUri = uri
                                    takePictureLauncher.launch(uri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onAudioClick = {
                                Toast.makeText(context, "Audio recording not implemented yet", Toast.LENGTH_SHORT).show()
                            },
                            themeMode = themeMode,
                            backgroundColor = Color.Transparent
                        )
                    }
                }
            }
        ) { padding ->
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (state.editingNoteType == "TEXT") {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(backgroundColor)
                                .verticalScroll(scrollState)
                        ) {
                            NoteAttachmentsList(
                                attachments = state.editingAttachments,
                                onEvent = onEvent,
                                onImageClick = { data ->
                                    selectedImageData = data
                                    showImageViewer = true
                                }
                            )

                            NoteTitleEditor(
                                state = state,
                                onEvent = onEvent,
                                onReminderClick = { checkAndRequestReminderPermissions() }
                            )
                            
                            NoteContentEditor(
                                state = state,
                                onEvent = onEvent,
                                onUrlClick = { url -> clickedUrl = url },
                                onSlashCommand = { showSlashCommandSheet = true }
                            )

                            if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                state.linkPreviews.forEach { linkPreview ->
                                    LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .background(backgroundColor)
                        ) {
                            item {
                                NoteTitleEditor(
                                    state = state,
                                    onEvent = onEvent,
                                    onReminderClick = { checkAndRequestReminderPermissions() }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            item {
                                 NoteAttachmentsList(
                                    attachments = state.editingAttachments,
                                    onEvent = onEvent,
                                    onImageClick = { data ->
                                        selectedImageData = data
                                        showImageViewer = true
                                    }
                                )
                            }

                            if (state.editingNoteType == "CHECKLIST") {
                                ChecklistEditor(
                                    state = state,
                                    onEvent = onEvent,
                                    isCheckedItemsExpanded = state.isCheckedItemsExpanded,
                                    onToggleCheckedItems = { onEvent(NotesEvent.ToggleCheckedItemsExpanded) }
                                )
                            } else {
                                item { 
                                    NoteContentEditor(
                                        state = state,
                                        onEvent = onEvent,
                                        onUrlClick = { url -> clickedUrl = url },
                                        onSlashCommand = { showSlashCommandSheet = true }
                                     )
                                }
                            }

                            if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(16.dp)) }
                                items(state.linkPreviews) { linkPreview ->
                                    LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showFormatBar && (state.editingNoteType == "TEXT" || state.editingNoteType == "CHECKLIST"),
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(spring()) + androidx.compose.animation.scaleIn(initialScale = 0.9f, animationSpec = spring()),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut(spring()) + androidx.compose.animation.scaleOut(targetScale = 0.9f, animationSpec = spring()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .padding(bottom = 120.dp)
        ) {
            Surface(
                shadowElevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                FormatToolbar(
                    state = state, 
                    onEvent = onEvent, 
                    onInsertLinkClick = { showInsertLinkDialog = true }, 
                    onGrammarFixClick = { onEvent(NotesEvent.FixGrammar) },
                    isFixingGrammar = state.isFixingGrammar,
                    themeMode = themeMode,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        var showAiChecklistSheet by remember { mutableStateOf(false) }
        val showAiButton = (state.editingNoteType == "TEXT" && state.editingContent.text.isEmpty()) || 
                           (state.editingNoteType == "CHECKLIST" && state.editingChecklist.isEmpty())
                           
        AnimatedVisibility(
            visible = showAiButton && !isFocusMode,
            enter = fadeIn(spring()) + slideInVertically(animationSpec = spring()) { it } + androidx.compose.animation.scaleIn(animationSpec = spring(), initialScale = 0.8f),
            exit = fadeOut(spring()) + slideOutVertically(animationSpec = spring()) { it } + androidx.compose.animation.scaleOut(animationSpec = spring(), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp) 
        ) {
            AiAssistantButton(
                onClick = { showAiChecklistSheet = true }
            )
        }

        AnimatedVisibility(
            visible = state.fixedContentPreview != null,
            enter = androidx.compose.animation.scaleIn(animationSpec = spring()) + fadeIn(spring()),
            exit = androidx.compose.animation.scaleOut(animationSpec = spring()) + fadeOut(spring()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    IconButton(onClick = { onEvent(NotesEvent.ApplyGrammarFix) }, modifier = Modifier.springPress()) {
                        Icon(Icons.Filled.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = { onEvent(NotesEvent.ClearGrammarFix) }, modifier = Modifier.springPress()) {
                        Icon(Icons.Filled.Close, contentDescription = "Discard", tint = Color(0xFFE57373))
                    }
                }
            }
        }

        AiChecklistSheet(
            isVisible = showAiChecklistSheet,
            isGenerating = state.isGeneratingChecklist,
            generatedItems = state.generatedChecklistPreview,
            onDismiss = { 
                showAiChecklistSheet = false
                onEvent(NotesEvent.ClearGeneratedChecklist)
            },
            onGenerate = { topic -> onEvent(NotesEvent.GenerateChecklist(topic)) },
            onInsert = { editedItems -> onEvent(NotesEvent.InsertGeneratedChecklist(editedItems)) },
            onRegenerate = { topic -> onEvent(NotesEvent.GenerateChecklist(topic)) }
        )
    }

    val createTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { onEvent(NotesEvent.ExportNote(it, "TXT")) }
    }
    val createMdLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let { onEvent(NotesEvent.ExportNote(it, "MD")) }
    }

    AddEditNoteDialogs(
        state = state,
        onEvent = onEvent,
        showDeleteDialog = showDeleteDialog,
        onShowDeleteDialogChange = { showDeleteDialog = it },
        showMoreOptions = showMoreOptions,
        onShowMoreOptionsChange = { showMoreOptions = it },
        showSaveAsDialog = showSaveAsDialog,
        onShowSaveAsDialogChange = { showSaveAsDialog = it },
        showHistoryDialog = showHistoryDialog,
        onShowHistoryDialogChange = { showHistoryDialog = it },
        showInsertLinkDialog = showInsertLinkDialog,
        onShowInsertLinkDialogChange = { showInsertLinkDialog = it },
        clickedUrl = clickedUrl,
        onClickedUrlChange = { clickedUrl = it },
        showExactAlarmDialog = showExactAlarmDialog,
        onShowExactAlarmDialogChange = { showExactAlarmDialog = it },
        settingsRepository = settingsRepository,
        scope = scope,
        onSaveAsPdf = {
            scope.launch {
                val htmlContent = com.suvojeet.notenext.util.HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
                val fullHtml = "<h1>${state.editingTitle}</h1><br>$htmlContent"
                com.suvojeet.notenext.util.printNote(context, fullHtml)
            }
        },
        onSaveAsTxt = {
            createTxtLauncher.launch("${state.editingTitle.ifBlank { "Untitled" }}.txt")
        },
        onSaveAsMd = {
             createMdLauncher.launch("${state.editingTitle.ifBlank { "Untitled" }}.md")
        }
    )
    if (showImageViewer) {
        selectedImageData?.let { data ->
            ImageViewerScreen(
                imageUri = data.uri,
                attachmentTempId = data.tempId,
                onDismiss = { showImageViewer = false },
                onEvent = onEvent
            )
        }
    }
    
    if (showReminderDialog) {
        ModalBottomSheet(
            onDismissRequest = { showReminderDialog = false },
            sheetState = reminderSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ReminderSheetContent(
                onDismissRequest = { showReminderDialog = false },
                onConfirm = { date: LocalDate, time: LocalTime, repeat: RepeatOption ->
                    onEvent(NotesEvent.SetReminderForSelectedNotes(date, time, repeat))
                    showReminderDialog = false
                }
            )
        }
    }

    if (showSlashCommandSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlashCommandSheet = false },
            sheetState = slashCommandSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SlashCommandSheetContent(
                onDismissRequest = { showSlashCommandSheet = false },
                onCommandSelected = { command ->
                    showSlashCommandSheet = false
                    when (command.title) {
                        "Heading 1" -> onEvent(NotesEvent.ApplyHeadingStyle(1))
                        "Checklist" -> if (state.editingNoteType == "TEXT") onEvent(NotesEvent.OnToggleNoteType)
                        "Image" -> getContent.launch("image/*")
                        "Bulleted List" -> { /* TODO: Implement Bullet list logic */ }
                    }
                }
            )
        }
    }

    if (showColorPicker) {
        ModalBottomSheet(
            onDismissRequest = { showColorPicker = false },
            sheetState = colorPickerSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ColorPicker(
                colors = colors,
                editingColor = state.editingColor,
                onEvent = onEvent
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
