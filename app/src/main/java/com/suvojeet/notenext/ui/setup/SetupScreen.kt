@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.setup

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.setup.components.PermissionItem
import com.suvojeet.notenext.ui.settings.BackupRestoreViewModel
import com.suvojeet.notenext.ui.components.PasswordInputDialog
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class SignInAction {
    RESTORE, ENABLE_BACKUP, CONNECT_ONLY
}

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val viewModel: SetupViewModel = hiltViewModel()
    val backupViewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val backupState by backupViewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(SetupEvent.ExactAlarmPermissionResult)
    }

    var postNotificationsGranted by remember { mutableStateOf(false) }
    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) postNotificationsGranted = true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            postNotificationsGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        backupViewModel.setGoogleAccount(account)
    }
    
    var signInAction by remember { mutableStateOf<SignInAction?>(null) }
    
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
        .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA))
        .build()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let {
                backupViewModel.setGoogleAccount(it)
                when (signInAction) {
                    SignInAction.RESTORE -> { }
                    SignInAction.ENABLE_BACKUP -> backupViewModel.toggleAutoBackup(true, it.email)
                    else -> { }
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
        signInAction = null
    }

    if (backupState.isPasswordRequired) {
        PasswordInputDialog(
            onDismiss = { backupViewModel.cancelPasswordEntry() },
            onConfirm = { password ->
                backupViewModel.restoreEncryptedBackup(password)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val canContinue = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || postNotificationsGranted) && state.exactAlarmGranted
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .imePadding()
                ) {
                     Button(
                        onClick = {
                            viewModel.onEvent(SetupEvent.CompleteSetup)
                            onSetupComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .springPress(),
                        enabled = canContinue,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                             text = stringResource(id = R.string.continue_button),
                             style = MaterialTheme.typography.titleLarge,
                             fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome to NoteNext",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your secure, expressive, and local notepad.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Cloud Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Google Drive Backup",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = if(backupState.googleAccountEmail != null) "Connected to ${backupState.googleAccountEmail}" else "Keep your notes synced across devices with secure cloud storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (backupState.googleAccountEmail == null) {
                        Button(
                            onClick = {
                                signInAction = SignInAction.CONNECT_ONLY
                                val signInIntent = GoogleSignIn.getClient(context, gso).signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Connect Google Account", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Daily Auto Backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text("Highly recommended", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = backupState.isAutoBackupEnabled,
                                        onCheckedChange = { enabled -> 
                                            backupState.googleAccountEmail?.let { 
                                                 backupViewModel.toggleAutoBackup(enabled, it)
                                            }
                                        },
                                        thumbContent = if (backupState.isAutoBackupEnabled) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                        } else null
                                    )
                                }
                            }

                            if (backupState.isRestoring) {
                                ExpressiveLoading(modifier = Modifier.height(100.dp))
                            } else if (backupState.restoreResult?.contains("successful", true) == true) {
                                 Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = Color(0xFFE8F5E9)
                                ) {
                                     Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Data Restored Successfully", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                 OutlinedButton(
                                    onClick = {
                                        val account = GoogleSignIn.getLastSignedInAccount(context)
                                        account?.let { backupViewModel.restoreFromDrive(it) }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                                    shape = MaterialTheme.shapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Restore Previous Notes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "System Access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        title = "Notifications",
                        description = "Required for reminders and sync status.",
                        isGranted = postNotificationsGranted,
                        onRequestClick = { postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }

                PermissionItem(
                    title = "Exact Alarms",
                    description = "Ensures reminders are triggered precisely.",
                    isGranted = state.exactAlarmGranted,
                    onRequestClick = {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                            exactAlarmPermissionLauncher.launch(it)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
