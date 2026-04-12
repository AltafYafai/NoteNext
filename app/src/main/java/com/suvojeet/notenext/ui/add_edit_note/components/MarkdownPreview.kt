package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.components.MarkdownText

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    MarkdownText(
        markdown = content,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
