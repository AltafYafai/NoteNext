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
        val document = parser.parse(markdown)
        val result = buildAnnotatedString {
            val visitor = AnnotatedStringVisitor(this)
            document.accept(visitor)
        }
        
        // Simple trim for trailing newlines
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
        
        // Sort spans by start position in descending order to avoid index shifts
        val sortedSpans = annotatedString.spanStyles.sortedByDescending { it.start }
        
        sortedSpans.forEach { range ->
            val start = range.start
            val end = range.end
            val style = range.item
            
            when {
                style.fontWeight == FontWeight.Bold -> {
                    result.insert(end, "**")
                    result.insert(start, "**")
                }
                style.fontStyle == FontStyle.Italic -> {
                    result.insert(end, "*")
                    result.insert(start, "*")
                }
                style.textDecoration == TextDecoration.Underline -> {
                    result.insert(end, "++")
                    result.insert(start, "++")
                }
                style.textDecoration == TextDecoration.LineThrough -> {
                    result.insert(end, "~~")
                    result.insert(start, "~~")
                }
            }
        }
        
        return result.toString()
    }

    private class AnnotatedStringVisitor(private val builder: AnnotatedString.Builder) : AbstractVisitor() {
        
        override fun visit(text: Text) {
            val content = text.literal
            // Simple Wiki Link detection within text nodes
            val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
            var lastIndex = 0
            
            wikiLinkRegex.findAll(content).forEach { match ->
                builder.append(content.substring(lastIndex, match.range.first))
                val title = match.groupValues[1]
                builder.pushStringAnnotation("NOTE_LINK", title)
                builder.withStyle(WikiLinkStyle) {
                    append(title)
                }
                builder.pop()
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
