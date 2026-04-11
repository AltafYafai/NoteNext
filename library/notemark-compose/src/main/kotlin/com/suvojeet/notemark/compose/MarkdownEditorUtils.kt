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
import com.suvojeet.notemark.core.NoteMarkParser
import com.suvojeet.notemark.core.model.*

/**
 * Utility functions for Markdown editing in Compose.
 */
object MarkdownEditorUtils {

    private val WikiLinkStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))

    /**
     * Converts a Markdown string to an [AnnotatedString] with styles.
     */
    fun markdownToAnnotatedString(text: String, theme: MarkdownTheme? = null): AnnotatedString {
        return buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                val headerMatch = "^(#+)\\s*(.*)".toRegex().find(line)
                if (headerMatch != null) {
                    val level = headerMatch.groupValues[1].length
                    val content = headerMatch.groupValues[2]
                    withStyle(getHeadingStyle(level)) {
                        appendInlineMarkdown(this, content)
                    }
                } else if (line.trimStart().startsWith("• ") || line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                    val bullet = if (line.trimStart().startsWith("• ")) "• " else if (line.trimStart().startsWith("- ")) "- " else "* "
                    val content = line.trimStart().removePrefix(bullet)
                    val leadingSpaces = line.takeWhile { it.isWhitespace() }
                    append(leadingSpaces)
                    append("• ")
                    appendInlineMarkdown(this, content)
                } else if (line.trimStart().startsWith("> ")) {
                    val content = line.trimStart().removePrefix("> ")
                    val leadingSpaces = line.takeWhile { it.isWhitespace() }
                    append(leadingSpaces)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                        appendInlineMarkdown(this, content)
                    }
                } else {
                    appendInlineMarkdown(this, line)
                }

                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun appendInlineMarkdown(builder: AnnotatedString.Builder, text: String) {
        val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
        val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
        val underlineRegex = "__u__(.*?)__u__".toRegex()
        val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
        val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()

        var lastIndex = 0
        val allMatches = (boldRegex.findAll(text) + italicRegex.findAll(text) + underlineRegex.findAll(text) + linkRegex.findAll(text) + wikiLinkRegex.findAll(text))
            .sortedBy { it.range.first }

        allMatches.forEach { match ->
            if (match.range.first >= lastIndex) {
                builder.append(text.substring(lastIndex, match.range.first))

                when {
                    match.value.startsWith("**") || match.value.startsWith("__") || (match.value.trim().startsWith("**")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        builder.append(prefix)
                        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            appendInlineMarkdown(this, content)
                        }
                    }
                    match.value.startsWith("*") || match.value.startsWith("_") || (match.value.trim().startsWith("*")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        builder.append(prefix)
                        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            appendInlineMarkdown(this, content)
                        }
                    }
                    match.value.startsWith("__u__") -> {
                        val content = match.groupValues[1]
                        builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            appendInlineMarkdown(this, content)
                        }
                    }
                    match.value.startsWith("[[") -> {
                        val linkText = match.groupValues[1]
                        builder.pushStringAnnotation(tag = "NOTE_LINK", annotation = linkText)
                        builder.withStyle(WikiLinkStyle) {
                            append(linkText)
                        }
                        builder.pop()
                    }
                    match.value.startsWith("[") -> {
                        val linkText = match.groupValues[1]
                        val url = match.groupValues[2]
                        builder.pushStringAnnotation(tag = "URL", annotation = url)
                        builder.withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append(linkText)
                        }
                        builder.pop()
                    }
                }
                lastIndex = match.range.last + 1
            }
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
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
        return sb.toString()
    }

    private fun getMarkdownSuffix(style: SpanStyle): String {
        val sb = StringBuilder()
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
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                val styleToApply = when {
                    styleToToggle.fontWeight == FontWeight.Bold -> if (isBoldActive) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                    styleToToggle.fontStyle == FontStyle.Italic -> if (isItalicActive) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                    styleToToggle.textDecoration == TextDecoration.Underline -> if (isUnderlineActive) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                    else -> styleToToggle
                }
                addStyle(styleToApply, selection.min, selection.max)
            }.toAnnotatedString()
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

        return markdownToAnnotatedString(newAnnotatedString.text).let {
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
