package com.suvojeet.notemark.core.model

/**
 * The base class for all nodes in the NoteMark Abstract Syntax Tree (AST).
 */
sealed class NoteMarkNode {
    /**
     * A list of child nodes. This allows for nested structures (e.g., Bold inside a Paragraph).
     */
    abstract val children: List<NoteMarkNode>
}

/**
 * Represents a top-level block of Markdown (e.g., a Header, a Paragraph).
 */
sealed class BlockNode : NoteMarkNode()

/**
 * Represents inline content within a block (e.g., Bold text, Italic text).
 */
sealed class InlineNode : NoteMarkNode()

/**
 * The root node of a Markdown document.
 */
data class DocumentNode(
    override val children: List<BlockNode>
) : NoteMarkNode()

/**
 * Represents a header (e.g., # Header 1).
 */
data class HeaderNode(
    val level: Int,
    override val children: List<InlineNode>
) : BlockNode()

/**
 * Represents a standard paragraph of text.
 */
data class ParagraphNode(
    override val children: List<InlineNode>
) : BlockNode()

/**
 * Represents a blockquote (> Quote).
 */
data class BlockQuoteNode(
    override val children: List<BlockNode>
) : BlockNode()

/**
 * Represents an unordered list item.
 */
data class ListItemNode(
    override val children: List<BlockNode>
) : BlockNode()

/**
 * Represents a fenced code block (```kotlin ... ```).
 */
data class CodeBlockNode(
    val language: String?,
    val content: String
) : BlockNode() {
    override val children: List<NoteMarkNode> = emptyList()
}

/**
 * Represents a horizontal rule (---).
 */
object HorizontalRuleNode : BlockNode() {
    override val children: List<NoteMarkNode> = emptyList()
}

/**
 * Represents raw plain text.
 */
data class TextNode(val text: String) : InlineNode() {
    override val children: List<NoteMarkNode> = emptyList()
}

/**
 * Represents bold text (**bold**).
 */
data class BoldNode(
    override val children: List<InlineNode>
) : InlineNode()

/**
 * Represents italic text (*italic*).
 */
data class ItalicNode(
    override val children: List<InlineNode>
) : InlineNode()

/**
 * Represents underlined text (__u__underline__u__).
 */
data class UnderlineNode(
    override val children: List<InlineNode>
) : InlineNode()

/**
 * Represents inline code (`code`).
 */
data class InlineCodeNode(val code: String) : InlineNode() {
    override val children: List<NoteMarkNode> = emptyList()
}

/**
 * Represents a link [text](url).
 */
data class LinkNode(
    val url: String,
    override val children: List<InlineNode>
) : InlineNode()

/**
 * Represents a custom WikiLink [[Title]].
 */
data class WikiLinkNode(
    val title: String
) : InlineNode() {
    override val children: List<NoteMarkNode> = emptyList()
}
