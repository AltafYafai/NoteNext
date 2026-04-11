package com.suvojeet.notemark.core

import com.suvojeet.notemark.core.model.*

object NoteMarkParser {

    /**
     * Parses a Markdown string into a [DocumentNode].
     */
    fun parse(content: String): DocumentNode {
        val lines = content.split("\n")
        val blocks = mutableListOf<BlockNode>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                line.startsWith("#") -> {
                    blocks.add(parseHeader(line))
                }
                line.startsWith(">") -> {
                    blocks.add(parseBlockQuote(line))
                }
                line.trim() == "---" -> {
                    blocks.add(HorizontalRuleNode)
                }
                line.startsWith("```") -> {
                    val (codeBlock, nextIndex) = parseCodeBlock(lines, i)
                    blocks.add(codeBlock)
                    i = nextIndex - 1
                }
                line.isNotBlank() -> {
                    blocks.add(ParagraphNode(parseInline(line)))
                }
            }
            i++
        }
        
        return DocumentNode(blocks)
    }

    private fun parseHeader(line: String): HeaderNode {
        val level = line.takeWhile { it == '#' }.length
        val text = line.dropWhile { it == '#' || it == ' ' }
        return HeaderNode(level, parseInline(text))
    }

    private fun parseBlockQuote(line: String): BlockQuoteNode {
        val text = line.drop(1).trim()
        // Simplified: only one level of blockquote for now
        return BlockQuoteNode(listOf(ParagraphNode(parseInline(text))))
    }

    private fun parseCodeBlock(lines: List<String>, startIndex: Int): Pair<CodeBlockNode, Int> {
        val lang = lines[startIndex].removePrefix("```").trim()
        val content = mutableListOf<String>()
        var i = startIndex + 1
        while (i < lines.size && !lines[i].startsWith("```")) {
            content.add(lines[i])
            i++
        }
        return CodeBlockNode(lang.ifBlank { null }, content.joinToString("\n")) to (i + 1)
    }

    /**
     * Parses inline Markdown (bold, italic, links, etc.).
     */
    private fun parseInline(text: String): List<InlineNode> {
        val nodes = mutableListOf<InlineNode>()
        
        // Use a simple state-based or regex-based parser for inlines
        // For the first step, we'll use a regex-based approach similar to what you had,
        // but we'll return a list of nodes instead of an AnnotatedString.
        
        val boldRegex = "(\\s|^)(\\*\\*|__)(.+?)\\2".toRegex()
        val italicRegex = "(\\s|^)(\\*|_)(.+?)\\2".toRegex()
        val underlineRegex = "__u__(.+?)__u__".toRegex()
        val linkRegex = "\\[(.+?)\\]\\((.+?)\\)".toRegex()
        val wikiLinkRegex = "\\[\\[(.+?)\\]\\]".toRegex()
        val inlineCodeRegex = "`(.+?)`".toRegex()

        var lastIndex = 0
        
        // Tag each match with its type to avoid guessing from match.value
        val underlineMatches = underlineRegex.findAll(text).map { it to "underline" }
        val wikiLinkMatches = wikiLinkRegex.findAll(text).map { it to "wikilink" }
        val linkMatches = linkRegex.findAll(text).map { it to "link" }
        val boldMatches = boldRegex.findAll(text).map { it to "bold" }
        val italicMatches = italicRegex.findAll(text).map { it to "italic" }
        val inlineCodeMatches = inlineCodeRegex.findAll(text).map { it to "code" }

        val allMatches = (underlineMatches + wikiLinkMatches + linkMatches + boldMatches + italicMatches + inlineCodeMatches)
            .sortedBy { it.first.range.first }

        allMatches.forEach { (match, type) ->
            if (match.range.first >= lastIndex) {
                val plainText = text.substring(lastIndex, match.range.first)
                if (plainText.isNotEmpty()) nodes.add(TextNode(plainText))
                
                when (type) {
                    "bold" -> {
                         // Extract the actual content group (index 3 because index 1 is prefix space)
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        if (prefix.isNotEmpty()) nodes.add(TextNode(prefix))
                        nodes.add(BoldNode(parseInline(content)))
                    }
                    "italic" -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        if (prefix.isNotEmpty()) nodes.add(TextNode(prefix))
                        nodes.add(ItalicNode(parseInline(content)))
                    }
                    "underline" -> {
                        val content = match.groupValues[1]
                        nodes.add(UnderlineNode(parseInline(content)))
                    }
                    "wikilink" -> {
                        nodes.add(WikiLinkNode(match.groupValues[1]))
                    }
                    "link" -> {
                        nodes.add(LinkNode(match.groupValues[2], parseInline(match.groupValues[1])))
                    }
                    "code" -> {
                        nodes.add(InlineCodeNode(match.groupValues[1]))
                    }
                }
                lastIndex = match.range.last + 1
            }
        }
        
        if (lastIndex < text.length) {
            nodes.add(TextNode(text.substring(lastIndex)))
        }
        
        return if (nodes.isEmpty() && text.isNotEmpty()) listOf(TextNode(text)) else nodes
    }
}
