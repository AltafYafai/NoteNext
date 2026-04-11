package com.suvojeet.notenext.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Utility for converting between [AnnotatedString] and Markdown.
 * Replaces the deprecated HtmlConverter.
 */
object MarkdownConverter {

    /**
     * Converts an [AnnotatedString] to a Markdown string.
     */
    fun annotatedStringToMarkdown(annotatedString: AnnotatedString): String {
        val text = annotatedString.text
        val sb = StringBuilder()
        
        // This is a simplified conversion. For a full WYSIWYG experience, 
        // we map the SpanStyles back to Markdown symbols.
        
        var currentIndex = 0
        
        // Sort spans by start index
        val spans = annotatedString.spanStyles.sortedBy { it.start }
        
        // We'll process the text character by character or in chunks to insert markers.
        // A more robust way is to use a tree structure, but for simple styles, 
        // we can track active styles.
        
        val activeSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        
        for (i in text.indices) {
            // Close spans that end here (in reverse order of opening)
            val endingSpans = activeSpans.filter { it.end == i }.sortedByDescending { it.start }
            endingSpans.forEach { span ->
                sb.append(getMarkdownSuffix(span.item))
                activeSpans.remove(span)
            }
            
            // Open spans that start here
            val startingSpans = spans.filter { it.start == i }
            startingSpans.forEach { span ->
                sb.append(getMarkdownPrefix(span.item))
                activeSpans.add(span)
            }
            
            sb.append(text[i])
        }
        
        // Close any remaining active spans
        activeSpans.sortedByDescending { it.start }.forEach { span ->
            sb.append(getMarkdownSuffix(span.item))
        }
        
        return sb.toString()
    }

    private fun getMarkdownPrefix(style: SpanStyle): String {
        return when {
            style.fontWeight == FontWeight.Bold -> "**"
            style.fontStyle == FontStyle.Italic -> "*"
            style.textDecoration == TextDecoration.Underline -> "__u__"
            else -> ""
        }
    }

    private fun getMarkdownSuffix(style: SpanStyle): String {
        return when {
            style.fontWeight == FontWeight.Bold -> "**"
            style.fontStyle == FontStyle.Italic -> "*"
            style.textDecoration == TextDecoration.Underline -> "__u__"
            else -> ""
        }
    }

    /**
     * Converts a Markdown string to an [AnnotatedString].
     * This implementation uses the logic from RichTextController for WYSIWYG.
     */
    fun markdownToAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                var currentLine = line
                
                // Handle Headers
                val headerMatch = "^(#+)\\s*(.*)".toRegex().find(currentLine)
                if (headerMatch != null) {
                    val level = headerMatch.groupValues[1].length
                    val content = headerMatch.groupValues[2]
                    withStyle(getHeadingStyle(level)) {
                        appendInlineMarkdown(this, content)
                    }
                } 
                // Handle Bullet Points
                else if (currentLine.trimStart().startsWith("• ") || currentLine.trimStart().startsWith("- ") || currentLine.trimStart().startsWith("* ")) {
                    val bullet = if (currentLine.trimStart().startsWith("• ")) "• " else if (currentLine.trimStart().startsWith("- ")) "- " else "* "
                    val content = currentLine.trimStart().removePrefix(bullet)
                    val leadingSpaces = currentLine.takeWhile { it.isWhitespace() }
                    append(leadingSpaces)
                    append("• ") 
                    appendInlineMarkdown(this, content)
                }
                // Handle Blockquotes
                else if (currentLine.trimStart().startsWith("> ")) {
                    val content = currentLine.trimStart().removePrefix("> ")
                    val leadingSpaces = currentLine.takeWhile { it.isWhitespace() }
                    append(leadingSpaces)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                        appendInlineMarkdown(this, content)
                    }
                }
                else {
                    appendInlineMarkdown(this, currentLine)
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
                            append(content)
                        }
                    }
                    match.value.startsWith("*") || match.value.startsWith("_") || (match.value.trim().startsWith("*")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        builder.append(prefix)
                        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("__u__") -> {
                        val content = match.groupValues[1]
                        builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("[[") -> {
                        val linkText = match.groupValues[1]
                        builder.pushStringAnnotation(tag = "NOTE_LINK", annotation = linkText)
                        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))) {
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

    private fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
    
    /**
     * Extracts plain text from Markdown by removing symbols.
     */
    fun markdownToPlainText(markdown: String): String {
        return markdown
            .replace(Regex("(?m)^#+\\s+"), "") // Headers at start of line
            .replace(Regex("(\\*\\*|__)(.*?)\\1"), "$2") // Bold
            .replace(Regex("(\\*|_)(.*?)\\1"), "$2") // Italic
            .replace(Regex("__u__(.*?)__u__"), "$1") // Underline
            .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "$1") // Links
            .replace(Regex("\\[\\[(.*?)\\]\\]"), "$1") // Wiki links
            .replace(Regex("(?m)^[•\\-*]\\s+"), "") // Bullet points at start of line
            .replace(Regex("(?m)^>\\s+"), "") // Blockquotes at start of line
            .replace(Regex("`{1,3}(.*?)\\1"), "$2") // Inline code and code blocks
            .replace(Regex("~~(.*?)~~"), "$1") // Strikethrough
            .replace(Regex("<[^>]*>"), "") // Remaining HTML tags
            .trim()
    }
}
