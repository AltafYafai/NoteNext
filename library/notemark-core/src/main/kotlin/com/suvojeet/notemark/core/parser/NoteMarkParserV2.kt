package com.suvojeet.notemark.core.parser

import com.suvojeet.notemark.core.lexer.NoteMarkLexer
import com.suvojeet.notemark.core.lexer.Token
import com.suvojeet.notemark.core.model.*

/**
 * A robust Markdown parser that uses a stack-based approach to handle nested styles.
 */
class NoteMarkParserV2 {

    fun parse(input: String): DocumentNode {
        val lines = input.split("\n")
        val blocks = mutableListOf<BlockNode>()
        
        for (line in lines) {
            blocks.add(parseBlock(line))
        }
        
        return DocumentNode(blocks)
    }

    private fun parseBlock(line: String): BlockNode {
        // Handle Header
        val headerMatch = "^(#+)\\s*(.*)".toRegex().find(line)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            val content = headerMatch.groupValues[2]
            return HeaderNode(level, parseInline(content))
        }
        
        // Handle List Item
        if (line.trimStart().startsWith("• ") || line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
             val bullet = if (line.trimStart().startsWith("• ")) "• " else if (line.trimStart().startsWith("- ")) "- " else "* "
             val content = line.trimStart().removePrefix(bullet)
             // For simplicity in this V2, we'll treat list items as having inline content
             // In a full implementation, we'd handle nested blocks.
             return ListItemNode(listOf(ParagraphNode(parseInline(content))))
        }

        // Default to Paragraph
        return ParagraphNode(parseInline(line))
    }

    fun parseInline(text: String): List<InlineNode> {
        val lexer = NoteMarkLexer(text)
        val tokens = lexer.tokenize()
        val nodes = mutableListOf<InlineNode>()
        val stack = mutableListOf<MutableList<InlineNode>>()
        val delimiters = mutableListOf<String>()
        
        stack.add(nodes)
        
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                is Token.Text -> stack.last().add(TextNode(token.content))
                is Token.EscapedChar -> stack.last().add(TextNode(token.char.toString()))
                is Token.Backtick -> {
                    // Handle inline code: scan for next backtick
                    var j = i + 1
                    val sb = StringBuilder()
                    while (j < tokens.size && tokens[j] !is Token.Backtick) {
                        val t = tokens[j]
                        sb.append(when(t) {
                            is Token.Text -> t.content
                            is Token.Delimiter -> t.text
                            is Token.EscapedChar -> "\\" + t.char
                            is Token.Special -> t.char
                            is Token.Backtick -> "`"
                            is Token.NewLine -> "\n"
                        })
                        j++
                    }
                    if (j < tokens.size) {
                        stack.last().add(InlineCodeNode(sb.toString()))
                        i = j
                    } else {
                        // Unclosed backtick, treat as text
                        stack.last().add(TextNode("`"))
                    }
                }
                is Token.Delimiter -> {
                    val d = token.text
                    if (delimiters.isNotEmpty() && delimiters.last() == d) {
                        // Closing delimiter
                        val children = stack.removeAt(stack.size - 1)
                        delimiters.removeAt(delimiters.size - 1)
                        val node = when (d) {
                            "**", "__" -> BoldNode(children)
                            "*", "_" -> ItalicNode(children)
                            "__u__" -> UnderlineNode(children)
                            "~~" -> StrikeThroughNode(children)
                            else -> TextNode(d + children.joinToString("") { it.toString() } + d) // Should not happen with current lexer
                        }
                        stack.last().add(node)
                    } else {
                        // Opening delimiter (simplified: always try to open if not already open)
                        // In a real parser, we'd check if it's "flanking"
                        delimiters.add(d)
                        stack.add(mutableListOf())
                    }
                }
                is Token.Special -> {
                    if (token.char == '[') {
                        // Try to parse link or wiki-link
                        if (i + 1 < tokens.size && tokens[i+1] is Token.Special && (tokens[i+1] as Token.Special).char == '[') {
                            // Possible WikiLink [[Title]]
                            var j = i + 2
                            val sb = StringBuilder()
                            while (j + 1 < tokens.size && !(tokens[j] is Token.Special && (tokens[j] as Token.Special).char == ']' && tokens[j+1] is Token.Special && (tokens[j+1] as Token.Special).char == ']')) {
                                sb.append(tokenToString(tokens[j]))
                                j++
                            }
                            if (j + 1 < tokens.size) {
                                stack.last().add(WikiLinkNode(sb.toString()))
                                i = j + 1
                            } else {
                                stack.last().add(TextNode("[["))
                                i++
                            }
                        } else {
                            // Possible Link [Text](URL)
                            var j = i + 1
                            var bracketCount = 1
                            val linkTextTokens = mutableListOf<Token>()
                            while (j < tokens.size && bracketCount > 0) {
                                val t = tokens[j]
                                if (t is Token.Special && t.char == '[') bracketCount++
                                if (t is Token.Special && t.char == ']') bracketCount--
                                if (bracketCount > 0) linkTextTokens.add(t)
                                j++
                            }
                            
                            if (j < tokens.size && tokens[j] is Token.Special && (tokens[j] as Token.Special).char == '(') {
                                var k = j + 1
                                val urlSb = StringBuilder()
                                while (k < tokens.size && !(tokens[k] is Token.Special && (tokens[k] as Token.Special).char == ')')) {
                                    urlSb.append(tokenToString(tokens[k]))
                                    k++
                                }
                                if (k < tokens.size) {
                                    // Valid link
                                    val linkText = parseInlineFromTokens(linkTextTokens)
                                    stack.last().add(LinkNode(urlSb.toString(), linkText))
                                    i = k
                                } else {
                                    stack.last().add(TextNode("["))
                                }
                            } else {
                                stack.last().add(TextNode("["))
                            }
                        }
                    } else {
                        stack.last().add(TextNode(token.char.toString()))
                    }
                }
                Token.NewLine -> stack.last().add(TextNode("\n"))
            }
            i++
        }
        
        // Handle unclosed delimiters
        while (stack.size > 1) {
            val children = stack.removeAt(stack.size - 1)
            val d = delimiters.removeAt(delimiters.size - 1)
            stack.last().add(TextNode(d))
            stack.last().addAll(children)
        }
        
        return stack[0]
    }

    private fun parseInlineFromTokens(tokens: List<Token>): List<InlineNode> {
        // This is a bit inefficient but for simplicity we'll just reconstruct the string
        // In a real parser we'd work directly with tokens.
        val sb = StringBuilder()
        for (t in tokens) {
            sb.append(tokenToString(t))
        }
        return parseInline(sb.toString())
    }

    private fun tokenToString(token: Token): String {
        return when (token) {
            is Token.Text -> token.content
            is Token.Delimiter -> token.text
            is Token.EscapedChar -> "\\" + token.char
            is Token.Special -> token.char.toString()
            is Token.Backtick -> "`"
            is Token.NewLine -> "\n"
        }
    }
}
