package com.suvojeet.notenext.ui.components

import android.graphics.Color
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.suvojeet.notenext.core.markdown.MarkdownParser
import com.suvojeet.notenext.core.markdown.LocalMarkwon
import io.noties.markwon.Markwon

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    markwon: Markwon? = null
) {
    val context = LocalContext.current
    val contentColor = LocalContentColor.current
    val textStyle = MaterialTheme.typography.bodyLarge
    
    val localMarkwon = LocalMarkwon.current
    val effectiveMarkwon = remember(markwon, localMarkwon) {
        markwon ?: localMarkwon ?: Markwon.create(context)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(contentColor.toArgb())
                textSize = textStyle.fontSize.value
            }
        },
        update = { textView ->
            effectiveMarkwon.setMarkdown(textView, markdown)
        }
    )
}
