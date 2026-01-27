
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == SaveStatus.SAVED) {
            showSavedMessage = true
            delay(2000)
            showSavedMessage = false
        } else {
            showSavedMessage = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = status == SaveStatus.SAVING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = " Saving...",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showSavedMessage && status == SaveStatus.SAVED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
