package com.suvojeet.notemark.core.renderer

import com.suvojeet.notemark.core.model.*

/**
 * Renders a NoteMark AST back into a Markdown string.
 */
class MarkdownRenderer {

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
            is HeaderNode -> {
                "#".repeat(block.level) + " " + renderInlines(block.children)
            }
            is ParagraphNode -> {
                renderInlines(block.children)
            }
            is ListItemNode -> {
                "• " + block.children.joinToString("\n") { renderBlock(it) }
            }
            is BlockQuoteNode -> {
                "> " + block.children.joinToString("\n") { renderBlock(it) }
            }
            is CodeBlockNode -> {
                "```${block.language ?: ""}\n${block.content}\n```"
            }
            HorizontalRuleNode -> {
                "---"
            }
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
            is TextNode -> escapeMarkdown(inline.text)
            is BoldNode -> "**" + renderInlines(inline.children) + "**"
            is ItalicNode -> "*" + renderInlines(inline.children) + "*"
            is UnderlineNode -> "__u__" + renderInlines(inline.children) + "__u__"
            is StrikeThroughNode -> "~~" + renderInlines(inline.children) + "~~"
            is InlineCodeNode -> "`" + inline.code + "`"
            is LinkNode -> "[" + renderInlines(inline.children) + "](" + inline.url + ")"
            is WikiLinkNode -> "[[" + inline.title + "]]"
        }
    }

    private fun escapeMarkdown(text: String): String {
        // Simple escape: characters that start a delimiter or special structure
        val specialChars = setOf('*', '_', '~', '`', '[', ']', '(', ')', '#', '\\')
        val sb = StringBuilder()
        for (char in text) {
            if (char in specialChars) {
                sb.append('\\')
            }
            sb.append(char)
        }
        return sb.toString()
    }
}
