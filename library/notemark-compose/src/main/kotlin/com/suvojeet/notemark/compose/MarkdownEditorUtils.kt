package com.suvojeet.notemark.compose

import androidx.compose.ui.graphics.Color
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
import com.suvojeet.notemark.compose.renderer.AnnotatedStringRenderer
import com.suvojeet.notemark.core.model.*
import com.suvojeet.notemark.core.parser.NoteMarkParserV2
import com.suvojeet.notemark.core.renderer.MarkdownRenderer

/**
 * Utility functions for Markdown editing in Compose.
 */
object MarkdownEditorUtils {

    private val WikiLinkStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
    
    private val parser = NoteMarkParserV2()
    private val annotatedStringRenderer = AnnotatedStringRenderer(wikiLinkStyle = WikiLinkStyle)
    private val markdownRenderer = MarkdownRenderer()

    /**
     * Converts a Markdown string to an [AnnotatedString] with styles.
     */
    fun markdownToAnnotatedString(text: String, theme: MarkdownTheme? = null): AnnotatedString {
        val document = parser.parse(text)
        return annotatedStringRenderer.render(document)
    }

    private fun appendInlineMarkdown(builder: AnnotatedString.Builder, text: String) {
        val inlines = parser.parseInline(text)
        // Helper to render inlines manually if needed by existing logic, 
        // but we've centralized this in AnnotatedStringRenderer now.
        // We'll keep this for backward compatibility if other methods use it.
        renderInlinesToBuilder(builder, inlines)
    }

    private fun renderInlinesToBuilder(builder: AnnotatedString.Builder, inlines: List<InlineNode>) {
        inlines.forEach { inline ->
            when (inline) {
                is TextNode -> builder.append(inline.text)
                is BoldNode -> {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        renderInlinesToBuilder(this, inline.children)
                    }
                }
                is ItalicNode -> {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        renderInlinesToBuilder(this, inline.children)
                    }
                }
                is UnderlineNode -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        renderInlinesToBuilder(this, inline.children)
                    }
                }
                is StrikeThroughNode -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        renderInlinesToBuilder(this, inline.children)
                    }
                }
                is InlineCodeNode -> {
                    builder.withStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.3f), fontStyle = FontStyle.Italic)) {
                        append(inline.code)
                    }
                }
                is LinkNode -> {
                    builder.pushStringAnnotation(tag = "URL", annotation = inline.url)
                    builder.withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                        renderInlinesToBuilder(this, inline.children)
                    }
                    builder.pop()
                }
                is WikiLinkNode -> {
                    builder.pushStringAnnotation(tag = "NOTE_LINK", annotation = inline.title)
                    builder.withStyle(WikiLinkStyle) {
                        append(inline.title)
                    }
                    builder.pop()
                }
            }
        }
    }

    fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else -> if (level > 0) SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp) else SpanStyle()
        }
    }

    /**
     * Converts [AnnotatedString] to Markdown.
     */
    fun annotatedStringToMarkdown(annotatedString: AnnotatedString): String {
        // Since we're moving to a robust AST-based approach, 
        // the conversion from AnnotatedString back to Markdown 
        // should ideally go through the same logic.
        // For now, we'll keep the existing manual logic as it's specifically 
        // designed to handle Compose SpanStyles which might not perfectly map to a fresh AST.
        val text = annotatedString.text
        val sb = StringBuilder()
        val spans = annotatedString.spanStyles.sortedBy { it.start }
        val activeSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        for (i in text.indices) {
            val endingSpans = activeSpans.filter { it.end == i }.sortedByDescending { it.start }
            endingSpans.forEach { span ->
                sb.append(getMarkdownSuffix(span.item))
                activeSpans.remove(span)
            }

            val startingSpans = spans.filter { it.start == i }
            startingSpans.forEach { span ->
                sb.append(getMarkdownPrefix(span.item))
                activeSpans.add(span)
            }
            sb.append(text[i])
        }

        activeSpans.sortedByDescending { it.start }.forEach { span ->
            sb.append(getMarkdownSuffix(span.item))
        }

        return sb.toString()
    }

    private fun getMarkdownPrefix(style: SpanStyle): String {
        val sb = StringBuilder()
        if (style.fontWeight == FontWeight.Bold) sb.append("**")
        if (style.fontStyle == FontStyle.Italic) sb.append("*")
        if (style.textDecoration == TextDecoration.Underline) sb.append("__u__")
        if (style.textDecoration == TextDecoration.LineThrough) sb.append("~~")
        return sb.toString()
    }

    private fun getMarkdownSuffix(style: SpanStyle): String {
        val sb = StringBuilder()
        if (style.textDecoration == TextDecoration.LineThrough) sb.append("~~")
        if (style.textDecoration == TextDecoration.Underline) sb.append("__u__")
        if (style.fontStyle == FontStyle.Italic) sb.append("*")
        if (style.fontWeight == FontWeight.Bold) sb.append("**")
        return sb.toString()
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

            if (isBold) {
                if (activeStyles.any { it.fontWeight == FontWeight.Bold }) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
            }
            if (isItalic) {
                if (activeStyles.any { it.fontStyle == FontStyle.Italic }) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
            }
            if (isUnderline) {
                if (activeStyles.any { it.textDecoration == TextDecoration.Underline }) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            return StyleToggleResult(updatedActiveStyles = activeStyles)
        } else {
            val newAnnotatedString = buildAnnotatedString {
                append(content.annotatedString.text)
                
                val styleToToggleIsBold = styleToToggle.fontWeight == FontWeight.Bold
                val styleToToggleIsItalic = styleToToggle.fontStyle == FontStyle.Italic
                val styleToToggleIsUnderline = styleToToggle.textDecoration == TextDecoration.Underline

                // Copy all existing spans, but remove or split those we're toggling off
                content.annotatedString.spanStyles.forEach { span ->
                    val isBoldSpan = span.item.fontWeight == FontWeight.Bold
                    val isItalicSpan = span.item.fontStyle == FontStyle.Italic
                    val isUnderlineSpan = span.item.textDecoration == TextDecoration.Underline

                    val togglingOff = (styleToToggleIsBold && isBoldActive && isBoldSpan) ||
                                     (styleToToggleIsItalic && isItalicActive && isItalicSpan) ||
                                     (styleToToggleIsUnderline && isUnderlineActive && isUnderlineSpan)

                    if (togglingOff) {
                        // Subtract selection from span range
                        if (span.start < selection.min) {
                            addStyle(span.item, span.start, selection.min)
                        }
                        if (span.end > selection.max) {
                            addStyle(span.item, selection.max, span.end)
                        }
                    } else {
                        addStyle(span.item, span.start, span.end)
                    }
                }

                // If we're toggling ON, add the new style
                val togglingOn = (styleToToggleIsBold && !isBoldActive) ||
                                (styleToToggleIsItalic && !isItalicActive) ||
                                (styleToToggleIsUnderline && !isUnderlineActive) ||
                                (!styleToToggleIsBold && !styleToToggleIsItalic && !styleToToggleIsUnderline)

                if (togglingOn) {
                    addStyle(styleToToggle, selection.min, selection.max)
                }

                // Copy other attributes
                content.annotatedString.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
                content.annotatedString.getStringAnnotations(0, content.text.length).forEach { 
                    addStringAnnotation(it.tag, it.item, it.start, it.end)
                }
            }
            return StyleToggleResult(updatedContent = content.copy(annotatedString = newAnnotatedString))
        }
    }

    fun applyHeading(content: TextFieldValue, level: Int): TextFieldValue? {
        val selection = content.selection
        if (!selection.collapsed) {
             val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(getHeadingStyle(level), selection.min, selection.max)
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
            val isLineSelected = if (selection.collapsed) selection.start in lineStart..lineEnd
            else maxOf(selection.min, lineStart) < minOf(selection.max, lineEnd)

            val newLine = if (isLineSelected) {
                if (line.trimStart().startsWith("• ")) line.replaceFirst("• ", "") else "• $line"
            } else line
            currentOffset += line.length + 1
            newLine
        }
        val newText = newLines.joinToString("\n")
        val diff = newText.length - text.length
        val newSelection = if (selection.collapsed) TextRange(selection.start + if (diff > 0) 2 else if (diff < 0) -2 else 0)
        else TextRange(selection.min, selection.max + diff)

        return content.copy(
            annotatedString = markdownToAnnotatedString(newText),
            selection = TextRange(newSelection.min.coerceIn(0, newText.length), newSelection.max.coerceIn(0, newText.length))
        )
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

        val oldText = oldContent.text
        val newText = decodeHtmlEntities(newContent.text)
        val fixedNewContent = newContent.copy(text = newText)

        val prefixLength = commonPrefixWith(oldText, newText).length
        val oldRemainder = oldText.substring(prefixLength)
        val newRemainder = newText.substring(prefixLength)

        val maxSuffixLength = minOf(oldRemainder.length, newRemainder.length)
        val suffixLength = commonSuffixWith(oldRemainder, newRemainder).length.coerceAtMost(maxSuffixLength)

        val newChangedPart = newRemainder.substring(0, (newRemainder.length - suffixLength).coerceAtLeast(0))

        val newAnnotatedString = buildAnnotatedString {
            val prefixEnd = prefixLength.coerceAtMost(oldContent.annotatedString.length)
            append(oldContent.annotatedString.subSequence(0, prefixEnd))

            val headingSpanStyle = getHeadingStyle(activeHeadingStyle)
            val styleToApply = (activeStyles + headingSpanStyle).reduceOrNull { a, b -> a.merge(b) } ?: SpanStyle()

            withStyle(styleToApply) {
                append(newChangedPart)
            }

            val suffixStart = (oldText.length - suffixLength).coerceIn(0, oldContent.annotatedString.length)
            val suffixEnd = oldText.length.coerceIn(suffixStart, oldContent.annotatedString.length)
            append(oldContent.annotatedString.subSequence(suffixStart, suffixEnd))
        }

        // Convert the styled string to markdown first, then re-parse to ensure all tags are in sync
        val markdown = annotatedStringToMarkdown(newAnnotatedString)
        return markdownToAnnotatedString(markdown).let {
            fixedNewContent.copy(annotatedString = it)
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        if (!text.contains("&")) return text

        var result = text
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")

        val numericEntityRegex = Regex("&#(\\d+);")
        result = numericEntityRegex.replace(result) { match ->
            try {
                val code = match.groupValues[1].toInt()
                code.toChar().toString()
            } catch (e: Exception) { match.value }
        }
        return result
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) if (a[i] != b[i]) return a.substring(0, i)
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) if (a[a.length - 1 - i] != b[b.length - 1 - i]) return a.substring(a.length - i)
        return a.substring(a.length - minLength)
    }

    fun markdownToPlainText(markdown: String): String {
        return markdown
            .replace(Regex("(?m)^#+\\s+"), "")
            .replace(Regex("(\\*\\*|__)(.*?)\\1"), "$2")
            .replace(Regex("(\\*|_)(.*?)\\1"), "$2")
            .replace(Regex("__u__(.*?)__u__"), "$1")
            .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "$1")
            .replace(Regex("\\[\\[(.*?)\\]\\]"), "$1")
            .replace(Regex("(?m)^[•\\-*]\\s+"), "")
            .replace(Regex("(?m)^>\\s+"), "")
            .replace(Regex("`{1,3}(.*?)\\1"), "$2")
            .replace(Regex("~~(.*?)~~"), "$1")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }
}
