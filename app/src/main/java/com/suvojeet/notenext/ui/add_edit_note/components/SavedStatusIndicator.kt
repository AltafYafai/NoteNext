
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
    // Determine icon and tint based on status
    // We want a subtle indicator.
    // SAVING -> Circular Progress (Small)
    // SAVED -> Cloud Done (Fade out)
    // ERROR -> Cloud Off (Red) - Assuming SaveStatus might have error, or just handle basic states for now. Since SaveStatus wasn't shown fully, I'll stick to SAVING/SAVED.
    
    // We'll keep the logic of showing "Saved" state for a moment, but since it's an icon, we can just keep it visible or fade it out.
    // Let's make it always visible if SAVING, and briefly visible if SAVED.

    var showSavedIcon by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == SaveStatus.SAVED) {
            showSavedIcon = true
            delay(2000)
            showSavedIcon = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        AnimatedVisibility(
            visible = status == SaveStatus.SAVING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
             CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor.copy(alpha = 0.7f),
                trackColor = Color.Transparent
            )
        }

        AnimatedVisibility(
            visible = status == SaveStatus.SAVED && showSavedIcon,
            enter = fadeIn() + androidx.compose.animation.scaleIn(),
            exit = fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Saved",
                modifier = Modifier.size(24.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
