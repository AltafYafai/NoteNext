package com.suvojeet.notemark.core.renderer

import com.suvojeet.notemark.core.model.*

/**
 * Renders a NoteMark AST into plain text (stripping Markdown styles).
 */
class PlainTextRenderer {

    fun render(document: DocumentNode): String {
        val sb = StringBuilder()
        document.children.forEachIndexed { index, block ->
            sb.append(renderBlock(block))
            if (index < document.children.size - 1) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun renderBlock(block: BlockNode): String {
        return when (block) {
            is HeaderNode -> renderInlines(block.children)
            is ParagraphNode -> renderInlines(block.children)
            is ListItemNode -> "• " + block.children.joinToString("\n") { renderBlock(it) }
            is BlockQuoteNode -> block.children.joinToString("\n") { renderBlock(it) }
            is CodeBlockNode -> block.content
            HorizontalRuleNode -> "---"
        }
    }

    private fun renderInlines(inlines: List<InlineNode>): String {
        val sb = StringBuilder()
        inlines.forEach { inline ->
            sb.append(renderInline(inline))
        }
        return sb.toString()
    }

    private fun renderInline(inline: InlineNode): String {
        return when (inline) {
            is TextNode -> inline.text
            is BoldNode -> renderInlines(inline.children)
            is ItalicNode -> renderInlines(inline.children)
            is UnderlineNode -> renderInlines(inline.children)
            is StrikeThroughNode -> renderInlines(inline.children)
            is InlineCodeNode -> inline.code
            is LinkNode -> renderInlines(inline.children)
            is WikiLinkNode -> inline.title
        }
    }
}
