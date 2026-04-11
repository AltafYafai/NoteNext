package com.suvojeet.notemark.core

import com.suvojeet.notemark.core.model.*

/**
 * Utility for converting NoteMark AST [DocumentNode] back to a Markdown string.
 */
object NoteMarkExporter {

    /**
     * Converts a [DocumentNode] to a Markdown string.
     */
    fun export(document: DocumentNode): String {
        return document.children.joinToString("\n") { exportBlock(it) }
    }

    private fun exportBlock(block: BlockNode): String {
        return when (block) {
            is HeaderNode -> {
                "#".repeat(block.level) + " " + exportInlines(block.children)
            }
            is ParagraphNode -> {
                exportInlines(block.children)
            }
            is BlockQuoteNode -> {
                block.children.joinToString("\n") { "> " + exportBlock(it) }
            }
            is ListItemNode -> {
                // Assuming unordered list for now
                "• " + block.children.joinToString("\n") { exportBlock(it) }
            }
            is CodeBlockNode -> {
                val lang = block.language ?: ""
                "```$lang\n${block.content}\n```"
            }
            is HorizontalRuleNode -> "---"
        }
    }

    private fun exportInlines(inlines: List<InlineNode>): String {
        val sb = StringBuilder()
        inlines.forEach { inline ->
            when (inline) {
                is TextNode -> sb.append(inline.text)
                is BoldNode -> {
                    sb.append("**")
                    sb.append(exportInlines(inline.children))
                    sb.append("**")
                }
                is ItalicNode -> {
                    sb.append("*")
                    sb.append(exportInlines(inline.children))
                    sb.append("*")
                }
                is UnderlineNode -> {
                    sb.append("__u__")
                    sb.append(exportInlines(inline.children))
                    sb.append("__u__")
                }
                is InlineCodeNode -> {
                    sb.append("`")
                    sb.append(inline.code)
                    sb.append("`")
                }
                is LinkNode -> {
                    sb.append("[")
                    sb.append(exportInlines(inline.children))
                    sb.append("](")
                    sb.append(inline.url)
                    sb.append(")")
                }
                is WikiLinkNode -> {
                    sb.append("[[")
                    sb.append(inline.title)
                    sb.append("]]")
                }
            }
        }
        return sb.toString()
    }
}
