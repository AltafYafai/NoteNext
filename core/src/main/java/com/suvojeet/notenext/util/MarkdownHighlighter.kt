package com.suvojeet.notenext.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * A Senior-level utility for real-time Markdown syntax highlighting.
 * Unlike a renderer, this preserves all markdown markers (**, _, etc.) 
 * while applying visual styles to the text and markers themselves.
 */
object MarkdownHighlighter {

    private val WikiLinkStyle = SpanStyle(
        color = Color(0xFFD0BCFF), // Light Purple
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium
    )

    private val MarkerStyle = SpanStyle(
        color = Color.Gray.copy(alpha = 0.7f),
        fontWeight = FontWeight.Normal
    )

    private val LinkStyle = SpanStyle(
        color = Color(0xFF64B5F6), // Light Blue
        textDecoration = TextDecoration.Underline
    )

    /**
     * Highlights markdown syntax within the provided text without modifying the content.
     */
    fun highlight(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            // 1. Headers (e.g., # Header)
            "(?m)^#{1,6}\\s.*$".toRegex().findAll(text).forEach { match ->
                val level = match.value.takeWhile { it == '#' }.length
                val fontSize = when (level) {
                    1 -> 24.sp; 2 -> 20.sp; 3 -> 18.sp
                    else -> 16.sp
                }
                addStyle(SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                // Dim the markers
                addStyle(MarkerStyle, match.range.first, match.range.first + level)
            }

            // 2. Bold (**text** or __text__)
            "(\\Q**\\E|\\Q__\\E)(.*?)\\1".toRegex().findAll(text).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 2)
                addStyle(MarkerStyle, match.range.last - 1, match.range.last + 1)
            }

            // 3. Italic (*text* or _text_)
            "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)(.*?)(?<!_)_(?!_)".toRegex().findAll(text).forEach { match ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 1)
                addStyle(MarkerStyle, match.range.last, match.range.last + 1)
            }

            // 4. Strikethrough (~~text~~)
            "~~(.*?)~~".toRegex().findAll(text).forEach { match ->
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 2)
                addStyle(MarkerStyle, match.range.last - 1, match.range.last + 1)
            }

            // 5. Underline (<u>text</u>)
            "<u>(.*?)</u>".toRegex().findAll(text).forEach { match ->
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 3)
                addStyle(MarkerStyle, match.range.last - 3, match.range.last + 1)
            }

            // 6. Wiki Links ([[Note Title]])
            "\\[\\[(.*?)\\]\\]".toRegex().findAll(text).forEach { match ->
                val title = match.groupValues[1]
                addStringAnnotation("NOTE_LINK", title, match.range.first, match.range.last + 1)
                addStyle(WikiLinkStyle, match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 2)
                addStyle(MarkerStyle, match.range.last - 1, match.range.last + 1)
            }

            // 7. Inline Code (`code`)
            "`(.*?)`".toRegex().findAll(text).forEach { match ->
                addStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.2f)), match.range.first, match.range.last + 1)
                addStyle(MarkerStyle, match.range.first, match.range.first + 1)
                addStyle(MarkerStyle, match.range.last, match.range.last + 1)
            }

            // 8. Standard Links ([text](url))
            "\\[(.*?)\\]\\((.*?)\\)".toRegex().findAll(text).forEach { match ->
                addStringAnnotation("URL", match.groupValues[2], match.range.first, match.range.last + 1)
                addStyle(LinkStyle, match.range.first, match.range.last + 1)
            }
        }
    }
}
