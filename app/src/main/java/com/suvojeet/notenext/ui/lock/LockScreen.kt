@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    
    val activity = context.findActivity() as? FragmentActivity
    val biometricAuthFailedString = stringResource(id = R.string.biometric_auth_failed)

    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(
                context = context,
                activity = activity
            )
        }
    } else {
        null
    }

    val canAuthenticateResult = biometricAuthManager?.canAuthenticate()

    LaunchedEffect(biometricAuthManager) {
        if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricAuthManager?.showBiometricPrompt(
                onAuthSuccess = { _ -> onUnlock() },
                onAuthError = {
                    if (it != "Authentication error: User Canceled") {
                        error = it
                    }
                },
                onAuthFailed = { error = biometricAuthFailedString }
            )
        } else {
             error = "Biometric authentication not available"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.4f))
            
            Surface(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(id = R.string.app_name), 
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Protected with biometric security",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
                 FilledTonalButton(
                    onClick = {
                        biometricAuthManager?.showBiometricPrompt(
                            onAuthSuccess = { _ -> onUnlock() },
                            onAuthError = {
                                if (it != "Authentication error: User Canceled") {
                                    error = it
                                }
                            },
                            onAuthFailed = { error = biometricAuthFailedString }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .springPress(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.unlock_with_biometrics), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                         text = "Biometric authentication is not available on this device", 
                         color = MaterialTheme.colorScheme.onErrorContainer,
                         modifier = Modifier.padding(16.dp),
                         textAlign = TextAlign.Center,
                         style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = error != null, 
                enter = fadeIn(animationSpec = spring()), 
                exit = fadeOut(animationSpec = spring())
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(0.6f))
            
            Text(
                text = "Secure · Private · Local",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
