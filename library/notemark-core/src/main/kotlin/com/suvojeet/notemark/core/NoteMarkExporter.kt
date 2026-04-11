package com.suvojeet.notemark.core

import com.suvojeet.notemark.core.model.*
import com.suvojeet.notemark.core.parser.NoteMarkParserV2
import com.suvojeet.notemark.core.renderer.MarkdownRenderer

/**
 * Utility for converting NoteMark AST [DocumentNode] back to a Markdown string.
 * This modern version uses the robust [MarkdownRenderer] for consistent output.
 */
object NoteMarkExporter {

    private val parser = NoteMarkParserV2()
    private val renderer = MarkdownRenderer()

    /**
     * Converts a [DocumentNode] to a Markdown string.
     */
    fun export(document: DocumentNode): String {
        return renderer.render(document)
    }

    /**
     * Converts a raw string (potentially containing some Markdown) 
     * into a fully normalized NoteMark Markdown string.
     */
    fun normalize(text: String): String {
        val document = parser.parse(text)
        return renderer.render(document)
    }

    /**
     * Legacy support: Converts a list of inlines to string.
     */
    fun exportInlines(inlines: List<InlineNode>): String {
        val doc = DocumentNode(listOf(ParagraphNode(inlines)))
        return renderer.render(doc).trim()
    }
}
