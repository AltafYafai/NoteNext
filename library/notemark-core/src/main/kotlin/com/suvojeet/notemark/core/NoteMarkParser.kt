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
        
        val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
        val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
        val underlineRegex = "__u__(.*?)__u__".toRegex()
        val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
        val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
        val inlineCodeRegex = "`(.*?)`".toRegex()

        var lastIndex = 0
        val allMatches = (boldRegex.findAll(text) + 
                          italicRegex.findAll(text) + 
                          underlineRegex.findAll(text) +
                          linkRegex.findAll(text) + 
                          wikiLinkRegex.findAll(text) +
                          inlineCodeRegex.findAll(text))
            .sortedBy { it.range.first }

        allMatches.forEach { match ->
            if (match.range.first >= lastIndex) {
                val plainText = text.substring(lastIndex, match.range.first)
                if (plainText.isNotEmpty()) nodes.add(TextNode(plainText))
                
                val matchValue = match.value
                when {
                    matchValue.startsWith("**") || matchValue.startsWith("__") || matchValue.trim().startsWith("**") -> {
                         // Extract the actual content group (index 3 because index 1 is prefix space)
                        val content = match.groupValues[3]
                        nodes.add(BoldNode(parseInline(content)))
                    }
                    matchValue.startsWith("*") || matchValue.startsWith("_") || matchValue.trim().startsWith("*") -> {
                        val content = match.groupValues[3]
                        nodes.add(ItalicNode(parseInline(content)))
                    }
                    matchValue.startsWith("__u__") -> {
                        val content = match.groupValues[1]
                        nodes.add(UnderlineNode(parseInline(content)))
                    }
                    matchValue.startsWith("[[") -> {
                        nodes.add(WikiLinkNode(match.groupValues[1]))
                    }
                    matchValue.startsWith("[") -> {
                        nodes.add(LinkNode(match.groupValues[2], parseInline(match.groupValues[1])))
                    }
                    matchValue.startsWith("`") -> {
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
