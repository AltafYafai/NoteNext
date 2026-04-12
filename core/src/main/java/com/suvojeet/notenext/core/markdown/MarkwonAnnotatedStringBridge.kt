package com.suvojeet.notenext.core.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

class MarkwonAnnotatedStringBridge {

    private val parser = Parser.builder().build()
    private val textRenderer = TextContentRenderer.builder().build()

    fun parse(text: String): AnnotatedString {
        val node = parser.parse(text)
        return buildAnnotatedString {
            val visitor = ComposeAnnotatedStringVisitor(this)
            node.accept(visitor)
        }
    }

    fun toPlainText(markdown: String): String {
        val node = parser.parse(markdown)
        return textRenderer.render(node)
    }

    private class ComposeAnnotatedStringVisitor(
        private val builder: AnnotatedString.Builder
    ) : AbstractVisitor() {

        override fun visit(text: Text) {
            builder.append(text.literal)
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

        override fun visit(heading: Heading) {
            val fontSize = when (heading.level) {
                1 -> 24.sp
                2 -> 20.sp
                3 -> 18.sp
                4 -> 16.sp
                5 -> 14.sp
                6 -> 12.sp
                else -> 16.sp
            }
            builder.withStyle(SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold)) {
                visitChildren(heading)
            }
            // Add a newline after headings
            if (heading.next != null) {
                builder.append("\n")
            }
        }

        override fun visit(link: Link) {
            builder.pushStringAnnotation(tag = "URL", annotation = link.destination)
            builder.withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                visitChildren(link)
            }
            builder.pop()
        }

        override fun visit(code: Code) {
            builder.withStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.3f))) {
                builder.append(code.literal)
            }
        }

        override fun visit(blockQuote: BlockQuote) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                visitChildren(blockQuote)
            }
        }

        override fun visit(bulletList: BulletList) {
            visitChildren(bulletList)
        }

        override fun visit(orderedList: OrderedList) {
            visitChildren(orderedList)
        }

        override fun visit(listItem: ListItem) {
            builder.append("• ")
            visitChildren(listItem)
            if (listItem.next != null) {
                builder.append("\n")
            }
        }

        override fun visit(paragraph: Paragraph) {
            visitChildren(paragraph)
            if (paragraph.next != null) {
                builder.append("\n\n")
            }
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append(" ")
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append("\n")
        }
    }
}
