package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import javax.inject.Inject

import androidx.compose.ui.graphics.Color

import com.suvojeet.notenext.util.MarkdownParser

class RichTextController @Inject constructor() {

    private val WikiLinkStyle = SpanStyle(
        color = Color(0xFFD0BCFF), // Light Purple
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium
    )

    fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }
    }

    fun processContentChange(
        oldContent: TextFieldValue,
        newContent: TextFieldValue,
        activeStyles: Set<SpanStyle>,
        activeHeadingStyle: Int
    ): TextFieldValue {
        if (newContent.text == oldContent.text) {
            return oldContent.copy(selection = newContent.selection)
        }

        // Re-parse the entire text with MarkdownParser for real-time syntax highlighting
        val highlightedAnnotatedString = MarkdownParser.toAnnotatedString(newContent.text)
        
        return newContent.copy(annotatedString = highlightedAnnotatedString)
    }

    data class StyleToggleResult(
        val updatedContent: TextFieldValue? = null,
        val updatedActiveStyles: Set<SpanStyle>? = null
    )

    fun toggleStyle(
        content: TextFieldValue,
        styleToToggle: SpanStyle,
        currentActiveStyles: Set<SpanStyle>,
        isBoldActive: Boolean,
        isItalicActive: Boolean,
        isUnderlineActive: Boolean
    ): StyleToggleResult {
        val selection = content.selection
        if (selection.collapsed) {
            val activeStyles = currentActiveStyles.toMutableSet()

            val isBold = styleToToggle.fontWeight == FontWeight.Bold
            val isItalic = styleToToggle.fontStyle == FontStyle.Italic
            val isUnderline = styleToToggle.textDecoration == TextDecoration.Underline

            val wasBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
            val wasItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
            val wasUnderline = activeStyles.any { it.textDecoration == TextDecoration.Underline }

            if (isBold) {
                if (wasBold) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
            }
            if (isItalic) {
                if (wasItalic) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
            }
            if (isUnderline) {
                if (wasUnderline) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            return StyleToggleResult(updatedActiveStyles = activeStyles)
        } else {
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                val isApplyingBold = styleToToggle.fontWeight == FontWeight.Bold
                val isApplyingItalic = styleToToggle.fontStyle == FontStyle.Italic
                val isApplyingUnderline = styleToToggle.textDecoration == TextDecoration.Underline

                val styleToApply = when {
                    isApplyingBold -> if (isBoldActive) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                    isApplyingItalic -> if (isItalicActive) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                    isApplyingUnderline -> if (isUnderlineActive) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                    else -> styleToToggle
                }
                addStyle(styleToApply, selection.start, selection.end)
            }.toAnnotatedString()

            return StyleToggleResult(updatedContent = content.copy(annotatedString = newAnnotatedString))
        }
    }

    fun applyHeading(
        content: TextFieldValue,
        level: Int
    ): TextFieldValue? {
        val selection = content.selection
        
        val headingStyle = when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }

        if (!selection.collapsed) {
             val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(headingStyle, selection.start, selection.end)
            }.toAnnotatedString()
            return content.copy(annotatedString = newAnnotatedString)
        }
        return null
    }

    fun toggleBulletedList(content: TextFieldValue): TextFieldValue {
        val text = content.text
        val selection = content.selection
        val lines = text.split("\n")
        
        var currentOffset = 0
        val newLines = lines.map { line ->
            val lineStart = currentOffset
            val lineEnd = currentOffset + line.length
            val isLineSelected = if (selection.collapsed) {
                selection.start in lineStart..lineEnd
            } else {
                val overlapStart = maxOf(selection.start, lineStart)
                val overlapEnd = minOf(selection.end, lineEnd)
                overlapStart < overlapEnd || (selection.start == selection.end && selection.start in lineStart..lineEnd)
            }

            val newLine = if (isLineSelected) {
                if (line.trimStart().startsWith("• ")) {
                    line.replaceFirst("• ", "")
                } else {
                    "• $line"
                }
            } else {
                line
            }
            
            currentOffset += line.length + 1
            newLine
        }

        val newText = newLines.joinToString("\n")
        
        // Basic selection adjustment (could be more sophisticated)
        val diff = newText.length - text.length
        val newSelection = if (selection.collapsed) {
             TextRange(selection.start + if (diff > 0) 2 else if (diff < 0) -2 else 0)
        } else {
            TextRange(selection.start, selection.end + diff)
        }

        return content.copy(
            annotatedString = parseMarkdownToAnnotatedString(newText),
            selection = TextRange(
                newSelection.start.coerceIn(0, newText.length),
                newSelection.end.coerceIn(0, newText.length)
            )
        )
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return a.substring(a.length - i)
            }
        }
        return a.substring(a.length - minLength)
    }

    fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
        return MarkdownParser.toAnnotatedString(text)
    }
}
