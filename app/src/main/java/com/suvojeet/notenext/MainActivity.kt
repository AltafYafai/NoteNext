package com.suvojeet.notenext

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val _startNoteIdFlow = MutableStateFlow(-1)
    private val _isSetupCompleteLoaded = MutableStateFlow<Boolean?>(null)
    private val _enableAppLockLoaded = MutableStateFlow<Boolean?>(null)
    
    private lateinit var updateChecker: UpdateChecker

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        splashScreen.setKeepOnScreenCondition {
            _isSetupCompleteLoaded.value == null || _enableAppLockLoaded.value == null
        }
        
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = android.view.animation.AlphaAnimation(1f, 0f).apply {
                duration = 400L
                fillAfter = true
            }
            val scaleOut = android.view.animation.ScaleAnimation(
                1f, 1.3f, 1f, 1.3f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 400L
                fillAfter = true
            }

            val animationSet = android.view.animation.AnimationSet(true).apply {
                addAnimation(fadeOut)
                addAnimation(scaleOut)
                setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        splashScreenView.remove()
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
            }
            splashScreenView.view.startAnimation(animationSet)
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        updateChecker = UpdateChecker(this)

        val initialNoteId = intent.getIntExtra("NOTE_ID", -1)
        _startNoteIdFlow.value = initialNoteId

        val startAddNote = intent.getBooleanExtra("START_ADD_NOTE", false)
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && "text/plain" == intent.type -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }

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
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            val enableAppLockLoaded by _enableAppLockLoaded.collectAsState()
            val isSetupCompleteLoaded by _isSetupCompleteLoaded.collectAsState()

            var unlocked by remember { mutableStateOf(false) }

            // In-App Update Handling
            val updateStatus by updateChecker.updateStatus.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            val updateAvailableText = stringResource(R.string.update_downloaded_ready)
            val restartText = stringResource(R.string.restart_to_update)

            LaunchedEffect(Unit) {
                updateChecker.checkForUpdate()
                updateChecker.resumeUpdateCheck(this@MainActivity)
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
                            } else if (enableAppLockLoaded!! && !unlocked) {
                                LockScreen(onUnlock = { unlocked = true })
                            } else {
                                val startNoteId by _startNoteIdFlow.collectAsState()
                                NavGraph(themeMode = themeMode, windowSizeClass = windowSizeClass, startNoteId = startNoteId, startAddNote = startAddNote, sharedText = sharedText)
                            }
                        }
                    }
                }
            }
        }
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
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        if (noteId != -1) {
             _startNoteIdFlow.value = noteId
        }
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
