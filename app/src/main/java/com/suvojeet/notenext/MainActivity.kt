package com.suvojeet.notenext

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.navigation.NavGraph
import com.suvojeet.notenext.ui.theme.NoteNextTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.suvojeet.notenext.ui.lock.LockScreen
import androidx.compose.runtime.LaunchedEffect
import com.suvojeet.notenext.ui.setup.SetupScreen
import java.util.Locale
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import com.suvojeet.notenext.util.UpdateChecker
import com.suvojeet.notenext.util.ReviewManager
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var updateChecker: UpdateChecker

    @Inject
    lateinit var reviewManager: ReviewManager

    private val _startNoteIdFlow = MutableStateFlow(-1)
    private val _startAddNoteFlow = MutableStateFlow(false)
    private val _sharedTextFlow = MutableStateFlow<String?>(null)
    private val _initialTitleFlow = MutableStateFlow<String?>(null)
    private val _searchQueryFlow = MutableStateFlow<String?>(null)
    private val _externalUriFlow = MutableStateFlow<android.net.Uri?>(null)

    private val _enableAppLockLoaded = MutableStateFlow<Boolean?>(null)
    private val _isSetupCompleteLoaded = MutableStateFlow<Boolean?>(null)
    private val _lockTrigger = MutableStateFlow(0)

    private var unlocked = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.enableAppLock.collect {
                    _enableAppLockLoaded.value = it
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.isSetupComplete.collect {
                    _isSetupCompleteLoaded.value = it
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.language.collect { languageCode ->
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }

        if (savedInstanceState != null) {
            unlocked = savedInstanceState.getBoolean("unlocked", false)
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val enableAppLockLoaded by _enableAppLockLoaded.collectAsStateWithLifecycle()
            val isSetupCompleteLoaded by _isSetupCompleteLoaded.collectAsStateWithLifecycle()
            val lockTrigger by _lockTrigger.collectAsStateWithLifecycle()

            var unlockedByAuth by remember(lockTrigger) { mutableStateOf(unlocked) }

            // In-App Update Handling
            val updateStatus by updateChecker.updateStatus.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            val updateAvailableText = stringResource(R.string.update_downloaded_ready)
            val restartText = stringResource(R.string.restart_to_update)

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000L)
                updateChecker.checkForUpdate()
            }

            LaunchedEffect(updateStatus) {
                when (updateStatus) {
                    is UpdateChecker.UpdateStatus.UpdateAvailable -> {
                        showUpdateDialog = true
                    }
                    is UpdateChecker.UpdateStatus.Downloaded -> {
                        val result = snackbarHostState.showSnackbar(
                            message = updateAvailableText,
                            actionLabel = restartText,
                            duration = androidx.compose.material3.SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            updateChecker.completeUpdate()
                        }
                    }
                    else -> {}
                }
            }

            if (showUpdateDialog) {
                com.suvojeet.notenext.ui.components.UpdateAvailableDialog(
                    onUpdateClick = {
                        showUpdateDialog = false
                        updateChecker.startUpdate(this@MainActivity)
                    },
                    onDismiss = { showUpdateDialog = false }
                )
            }

            NoteNextTheme(themeMode = themeMode) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val unusedPadding = paddingValues
                        
                        androidx.compose.animation.AnimatedContent(
                            targetState = enableAppLockLoaded == null || isSetupCompleteLoaded == null,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.92f))
                                    .togetherWith(androidx.compose.animation.fadeOut())
                            },
                            label = "AppStartupTransition"
                        ) { isInitialLoading ->
                            if (isInitialLoading) {
                                // Keep showing background color while waiting for splash screen to dismiss
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                            } else if (isSetupCompleteLoaded == false) {
                                SetupScreen { }
                            } else if (enableAppLockLoaded!! && !unlockedByAuth) {
                                LockScreen(onUnlock = { 
                                    unlocked = true
                                    unlockedByAuth = true 
                                    _lockTrigger.value += 1
                                })
                            } else {
                                val notesViewModel: com.suvojeet.notenext.ui.notes.NotesViewModel = hiltViewModel()
                                val startNoteId by _startNoteIdFlow.collectAsStateWithLifecycle()
                                val startAddNote by _startAddNoteFlow.collectAsStateWithLifecycle()
                                val sharedText by _sharedTextFlow.collectAsStateWithLifecycle()
                                val initialTitle by _initialTitleFlow.collectAsStateWithLifecycle()
                                val searchQuery by _searchQueryFlow.collectAsStateWithLifecycle()
                                val externalUri by _externalUriFlow.collectAsStateWithLifecycle()

                                NavGraph(
                                    notesViewModel = notesViewModel,
                                    themeMode = themeMode,
                                    windowSizeClass = windowSizeClass,
                                    settingsRepository = settingsRepository,
                                    startNoteId = startNoteId,
                                    startAddNote = startAddNote,
                                    sharedText = sharedText,
                                    initialTitle = initialTitle,
                                    searchQuery = searchQuery,
                                    externalUri = externalUri
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        _startNoteIdFlow.value = noteId

        // Extract Assistant related actions
        val isCreateNoteAction = intent.action == "android.intent.action.CREATE_NOTE"
        if (isCreateNoteAction) {
            _startAddNoteFlow.value = true
            _sharedTextFlow.value = intent.getStringExtra(Intent.EXTRA_TEXT)
            _initialTitleFlow.value = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        }

        // Standard SHARE intent
        if (intent.action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                _sharedTextFlow.value = intent.getStringExtra(Intent.EXTRA_TEXT)
                _initialTitleFlow.value = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                _startAddNoteFlow.value = true
            } else if (intent.type?.startsWith("image/") == true) {
                (intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? android.net.Uri)?.let {
                    _externalUriFlow.value = it
                    _startAddNoteFlow.value = true
                }
            }
        }

        // Search intent
        if (intent.action == Intent.ACTION_SEARCH || intent.action == "com.google.android.gms.actions.SEARCH_ACTION") {
            _searchQueryFlow.value = intent.getStringExtra("query")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
    }

    override fun onPause() {
        super.onPause()
        // If app lock is enabled, we might want to re-lock when app goes to background
        // but typically it's better to do it on stop or after a timeout.
    }

    override fun onStop() {
        super.onStop()
        // Optional: unlocked = false if you want to lock every time app stops
    }
}
