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

    private val _startNoteIdFlow = MutableStateFlow(-1)
    private val _startAddNoteFlow = MutableStateFlow(false)
    private val _sharedTextFlow = MutableStateFlow<String?>(null)
    private val _initialTitleFlow = MutableStateFlow<String?>(null)
    private val _searchQueryFlow = MutableStateFlow<String?>(null)
    private val _externalUriFlow = MutableStateFlow<android.net.Uri?>(null)

    private val _isSetupCompleteLoaded = MutableStateFlow<Boolean?>(null)
    private val _enableAppLockLoaded = MutableStateFlow<Boolean?>(null)
    private val _lockTrigger = MutableStateFlow(0L)

    private var unlocked = false
    private var lastPauseTime: Long = 0
    
    private lateinit var updateChecker: UpdateChecker
    private lateinit var reviewManager: ReviewManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        splashScreen.setKeepOnScreenCondition {
            _isSetupCompleteLoaded.value == null || _enableAppLockLoaded.value == null
        }
        
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val splashView = splashScreenView.view
            splashView.animate()
                .alpha(0f)
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(400L)
                .setInterpolator(android.view.animation.AnticipateInterpolator())
                .withEndAction {
                    // Safety check to ensure activity is still alive and view is attached
                    if (!isFinishing && !isDestroyed) {
                        splashScreenView.remove()
                    }
                }
                .start()
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        updateChecker = UpdateChecker(this)
        reviewManager = ReviewManager(this)
        
        // Trigger review check
        reviewManager.checkAndRequestReview(this)

        handleIntent(intent)

        lifecycleScope.launch {
            settingsRepository.isSetupComplete.collect { _isSetupCompleteLoaded.value = it }
        }
        lifecycleScope.launch {
            settingsRepository.enableAppLock.collect { _enableAppLockLoaded.value = it }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.disallowScreenshots.collect { disallow ->
                    if (disallow) {
                        window.setFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SECURE,
                            android.view.WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        lifecycleScope.launch {
            settingsRepository.language.collect { languageCode ->
                val appLocales = LocaleListCompat.forLanguageTags(languageCode)
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AMOLED -> true
            }

            androidx.compose.runtime.LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme }
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

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
                                })
                            } else {
                                val startNoteId by _startNoteIdFlow.collectAsStateWithLifecycle()
                                val startAddNote by _startAddNoteFlow.collectAsStateWithLifecycle()
                                val sharedText by _sharedTextFlow.collectAsStateWithLifecycle()
                                val initialTitle by _initialTitleFlow.collectAsStateWithLifecycle()
                                val searchQuery by _searchQueryFlow.collectAsStateWithLifecycle()
                                val externalUri by _externalUriFlow.collectAsStateWithLifecycle()

                                NavGraph(
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
        _startAddNoteFlow.value = intent.getBooleanExtra("START_ADD_NOTE", false) || isCreateNoteAction
        
        _initialTitleFlow.value = intent.getStringExtra("TITLE") ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        _searchQueryFlow.value = intent.getStringExtra("QUERY")
        
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            _externalUriFlow.value = intent.data
        } else {
            _externalUriFlow.value = null
        }

        val sharedText = when {
            intent.action == Intent.ACTION_SEND && "text/plain" == intent.type -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            intent.hasExtra(Intent.EXTRA_TEXT) -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }
        _sharedTextFlow.value = sharedText
    }

    override fun onStart() {
        super.onStart()
        // If app was in background for more than 2 minutes, re-lock
        if (_enableAppLockLoaded.value == true && lastPauseTime > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPauseTime > 120_000) { // 2 minutes
                unlocked = false
                _lockTrigger.value = currentTime
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastPauseTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        updateChecker.resumeUpdateCheck(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateChecker.unregisterListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onActionModeStarted(mode: android.view.ActionMode?) {
        val menu = mode?.menu
        if (menu != null) {
            menu.add("Bold").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    )
                }
                mode.finish() 
                true
            }
            menu.add("Italic").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
                mode.finish()
                true
            }
            menu.add("Underline").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    )
                }
                mode.finish()
                true
            }
            menu.add("Strike").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    )
                }
                mode.finish()
                true
            }
        }
        super.onActionModeStarted(mode)
    }
}
