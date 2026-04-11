package com.suvojeet.notemark.core.util

import com.suvojeet.notemark.core.parser.NoteMarkParserV2
import com.suvojeet.notemark.core.renderer.MarkdownRenderer

/**
 * Utilities for AI integration with Markdown.
 */
object AIUtils {
    private val parser = NoteMarkParserV2()
    private val renderer = MarkdownRenderer()

    /**
     * Cleans up AI-generated Markdown to ensure it follows the NoteMark AST structure.
     * This is useful if the AI introduces unsupported tags or malformed structures.
     */
    fun sanitizeAIMarkdown(rawMarkdown: String): String {
        val document = parser.parse(rawMarkdown)
        return renderer.render(document)
    }

    /**
     * Prepares content for AI by stripping potentially confusing Markdown 
     * but keeping the structure readable.
     */
    fun prepareContentForAI(markdown: String): String {
        // We can use PlainTextRenderer here if we want absolute raw text,
        // or a specialized version that keeps some structure.
        // For now, let's just sanitize it.
        return sanitizeAIMarkdown(markdown)
    }
}
