
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.notes.SaveStatus
import kotlinx.coroutines.delay

@Composable
fun SavedStatusIndicator(
    status: SaveStatus, 
    contentColor: Color
) {
    // Local state to control the visibility of the "Saved" message
    // It should appear for a short duration after status changes to SAVED
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == SaveStatus.SAVED) {
            showSavedMessage = true
            delay(2000)
            showSavedMessage = false
        } else {
            // Immediately hide saved message if status changes to something else (e.g. SAVING)
            if (status != SaveStatus.SAVED) {
                showSavedMessage = false
            }
        }
    }

    // Use a subtle container background for better contrast if needed, 
    // or just clean text/icon on the app bar background.
    // Here we use a clean Row with animations.
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = status,
            label = "SaveStatusAnimation"
        ) { targetStatus ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (targetStatus) {
                    SaveStatus.SAVING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    SaveStatus.SAVED -> {
                        // Only show if the ephemeral message shoud be visible, 
                        // OR if we want it to stay visible. 
                        // The user request implied "ajeeb garib" likely meant it was jumping around.
                        // Let's show "Saved" with a Check icon.
                        if (showSavedMessage) {
                             Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Saved",
                                modifier = Modifier.size(14.dp),
                                tint = contentColor.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.8f)
                            )
                        } else {
                            // When idle/not saved recently, show nothing or just a small dot?
                            // For now, let's show an empty box to collapse the size smoothly, 
                            // or keep it hidden.
                            // Providing a small spacer to prevent complete collapse if desired,
                            // but usually hidden is better.
                            Spacer(modifier = Modifier.size(1.dp)) 
                        }
                    }
                    else -> {
                         Spacer(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}
