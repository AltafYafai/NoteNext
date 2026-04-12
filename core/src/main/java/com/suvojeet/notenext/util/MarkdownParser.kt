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
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.ins.Ins
import org.commonmark.ext.ins.InsExtension
import org.commonmark.node.*
import org.commonmark.parser.Parser

object MarkdownParser {

    private val extensions = listOf(
        AutolinkExtension.create(),
        StrikethroughExtension.create(),
        InsExtension.create()
    )

    private val parser = Parser.builder()
        .extensions(extensions)
        .build()

    private val WikiLinkStyle = SpanStyle(
        color = Color(0xFFD0BCFF), // Light Purple
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium
    )

    private val LinkStyle = SpanStyle(
        color = Color(0xFF64B5F6), // Light Blue
        textDecoration = TextDecoration.Underline
    )

    fun toAnnotatedString(markdown: String): AnnotatedString {
        // Pre-handle <u> tags before markdown parsing
        val document = parser.parse(markdown)
        val result = buildAnnotatedString {
            val visitor = AnnotatedStringVisitor(this)
            document.accept(visitor)
        }
        
        // Only trim the VERY last newline added by the last paragraph if it exists
        // to avoid infinite growth, but don't trim spaces or meaningful newlines.
        return if (result.text.endsWith("\n\n")) {
            result.subSequence(0, result.text.length - 2) as AnnotatedString
        } else if (result.text.endsWith("\n")) {
            result.subSequence(0, result.text.length - 1) as AnnotatedString
        } else {
            result
        }
    }

    fun toMarkdown(annotatedString: AnnotatedString): String {
        val result = StringBuilder(annotatedString.text)
        
        // Group spans by their range to handle multiple styles on the same text
        val spansByRange = annotatedString.spanStyles
            .groupBy { it.start to it.end }
            .toList()
            .sortedByDescending { it.first.first } // Sort by start index descending
        
        spansByRange.forEach { (range, spans) ->
            val start = range.first
            val end = range.second
            
            var isBold = false
            var isItalic = false
            var isUnderline = false
            var isStrikethrough = false
            
            spans.forEach { span ->
                val style = span.item
                if (style.fontWeight == FontWeight.Bold) isBold = true
                if (style.fontStyle == FontStyle.Italic) isItalic = true
                style.textDecoration?.let { decoration ->
                    if (decoration.contains(TextDecoration.Underline)) isUnderline = true
                    if (decoration.contains(TextDecoration.LineThrough)) isStrikethrough = true
                }
            }
            
            // Apply markers in a specific order to avoid confusion
            if (isStrikethrough) {
                result.insert(end, "~~")
                result.insert(start, "~~")
            }
            if (isUnderline) {
                result.insert(end, "</u>")
                result.insert(start, "<u>")
            }
            
            // Combine Bold and Italic markers
            val marker = when {
                isBold && isItalic -> "***"
                isBold -> "**"
                isItalic -> "*"
                else -> ""
            }
            
            if (marker.isNotEmpty()) {
                result.insert(end, marker)
                result.insert(start, marker)
            }
        }
        
        return result.toString()
    }

    private class AnnotatedStringVisitor(private val builder: AnnotatedString.Builder) : AbstractVisitor() {
        
        override fun visit(text: Text) {
            val content = text.literal
            
            // 1. Wiki Link detection
            val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
            // 2. Underline detection
            val underlineRegex = "<u>(.*?)</u>".toRegex()
            
            var lastIndex = 0
            
            // Combine all tags for sequential processing
            val matches = (wikiLinkRegex.findAll(content).map { it to "WIKI" } + 
                           underlineRegex.findAll(content).map { it to "UNDERLINE" })
                           .sortedBy { it.first.range.first }

            matches.forEach { (match, type) ->
                builder.append(content.substring(lastIndex, match.range.first))
                val innerText = match.groupValues[1]
                
                when (type) {
                    "WIKI" -> {
                        builder.pushStringAnnotation("NOTE_LINK", innerText)
                        builder.withStyle(WikiLinkStyle) { append(innerText) }
                        builder.pop()
                    }
                    "UNDERLINE" -> {
                        builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(innerText)
                        }
                    }
                }
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < content.length) {
                builder.append(content.substring(lastIndex))
            }
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                visitChildren(strongEmphasis)
            }
        }

        override fun visit(emphasis: Emphasis) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                visitChildren(emphasis)
            }
        }

        override fun visit(link: Link) {
            builder.pushStringAnnotation("URL", link.destination)
            builder.withStyle(LinkStyle) {
                visitChildren(link)
            }
            builder.pop()
        }

        override fun visit(code: Code) {
            builder.withStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.3f))) {
                append(code.literal)
            }
        }

        override fun visit(customNode: CustomNode) {
            when (customNode) {
                is Strikethrough -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        visitChildren(customNode)
                    }
                }
                is Ins -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        visitChildren(customNode)
                    }
                }
                else -> super.visit(customNode)
            }
        }

        override fun visit(heading: Heading) {
            val fontSize = when (heading.level) {
                1 -> 24
                2 -> 20
                3 -> 18
                4 -> 16
                5 -> 14
                6 -> 12
                else -> 16
            }
            builder.withStyle(SpanStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)) {
                visitChildren(heading)
            }
            builder.append("\n")
        }

        override fun visit(paragraph: Paragraph) {
            visitChildren(paragraph)
            if (paragraph.parent !is ListItem) {
                builder.append("\n\n")
            }
        }

        override fun visit(blockQuote: BlockQuote) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                visitChildren(blockQuote)
            }
            builder.append("\n")
        }

        override fun visit(bulletList: BulletList) {
            visitChildren(bulletList)
            builder.append("\n")
        }

        override fun visit(orderedList: OrderedList) {
            visitChildren(orderedList)
            builder.append("\n")
        }

        override fun visit(listItem: ListItem) {
            builder.append("• ") // Simple bullet for all list items for now
            visitChildren(listItem)
            builder.append("\n")
        }
        
        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append("\n")
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append(" ")
        }
    }
}
