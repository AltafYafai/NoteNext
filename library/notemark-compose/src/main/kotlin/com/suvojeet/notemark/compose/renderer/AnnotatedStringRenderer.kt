package com.suvojeet.notemark.compose.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.suvojeet.notemark.core.model.*

/**
 * Renders a NoteMark AST into a Compose [AnnotatedString].
 */
class AnnotatedStringRenderer(
    private val wikiLinkStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6200EE)),
    private val linkStyle: SpanStyle = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
    private val inlineCodeStyle: SpanStyle = SpanStyle(background = Color.LightGray.copy(alpha = 0.3f), fontStyle = FontStyle.Italic)
) {

    fun render(document: DocumentNode): AnnotatedString {
        return buildAnnotatedString {
            document.children.forEachIndexed { index, block ->
                renderBlock(this, block)
                if (index < document.children.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun renderBlock(builder: AnnotatedString.Builder, block: BlockNode) {
        when (block) {
            is HeaderNode -> {
                val style = getHeadingStyle(block.level)
                builder.withStyle(style) {
                    renderInlines(this, block.children)
                }
            }
            is ParagraphNode -> {
                renderInlines(builder, block.children)
            }
            is ListItemNode -> {
                builder.append("• ")
                block.children.forEach { child ->
                    renderBlock(builder, child)
                }
            }
            is BlockQuoteNode -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                    block.children.forEach { child ->
                        renderBlock(this, child)
                    }
                }
            }
            is CodeBlockNode -> {
                builder.withStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.2f))) {
                    append(block.content)
                }
            }
            HorizontalRuleNode -> {
                builder.append("---")
            }
        }
    }

    private fun renderInlines(builder: AnnotatedString.Builder, inlines: List<InlineNode>) {
        inlines.forEach { inline ->
            when (inline) {
                is TextNode -> builder.append(inline.text)
                is BoldNode -> {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        renderInlines(this, inline.children)
                    }
                }
                is ItalicNode -> {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        renderInlines(this, inline.children)
                    }
                }
                is UnderlineNode -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        renderInlines(this, inline.children)
                    }
                }
                is StrikeThroughNode -> {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        renderInlines(this, inline.children)
                    }
                }
                is InlineCodeNode -> {
                    builder.withStyle(inlineCodeStyle) {
                        append(inline.code)
                    }
                }
                is LinkNode -> {
                    builder.pushStringAnnotation(tag = "URL", annotation = inline.url)
                    builder.withStyle(linkStyle) {
                        renderInlines(this, inline.children)
                    }
                    builder.pop()
                }
                is WikiLinkNode -> {
                    builder.pushStringAnnotation(tag = "NOTE_LINK", annotation = inline.title)
                    builder.withStyle(wikiLinkStyle) {
                        append(inline.title)
                    }
                    builder.pop()
                }
            }
        }
    }

    private fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.sp(24))
            2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.sp(20))
            3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.sp(18))
            else -> if (level > 0) SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.sp(16)) else SpanStyle()
        }
    }
}
