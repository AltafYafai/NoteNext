package com.suvojeet.notemark.core.lexer

/**
 * Represents a token in the Markdown source.
 */
sealed class Token {
    /**
     * Plain text.
     */
    data class Text(val content: String) : Token()

    /**
     * Markdown delimiter (e.g., *, **, __, ~~, etc.).
     */
    data class Delimiter(val text: String) : Token()

    /**
     * Escaped character (e.g., \*).
     */
    data class EscapedChar(val char: Char) : Token()

    /**
     * Special characters for links ([ ] ( )).
     */
    data class Special(val char: Char) : Token()

    /**
     * Inline code backtick (`).
     */
    object Backtick : Token()

    /**
     * New line.
     */
    object NewLine : Token()
}

/**
 * A tokenizer that scans the Markdown input string and produces a stream of tokens.
 */
class NoteMarkLexer(private val input: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < input.length) {
            val char = input[pos]
            when {
                char == '\\' -> {
                    if (pos + 1 < input.length) {
                        tokens.add(Token.EscapedChar(input[pos + 1]))
                        pos += 2
                    } else {
                        tokens.add(Token.Text("\\"))
                        pos++
                    }
                }
                char == '`' -> {
                    tokens.add(Token.Backtick)
                    pos++
                }
                char == '\n' -> {
                    tokens.add(Token.NewLine)
                    pos++
                }
                isDelimiterStart(char) -> {
                    val delimiter = consumeDelimiter()
                    tokens.add(Token.Delimiter(delimiter))
                }
                isSpecial(char) -> {
                    tokens.add(Token.Special(char))
                    pos++
                }
                else -> {
                    val text = consumeText()
                    tokens.add(Token.Text(text))
                }
            }
        }
        return tokens
    }

    private fun isDelimiterStart(char: Char): Boolean {
        return char == '*' || char == '_' || char == '~'
    }

    private fun isSpecial(char: Char): Boolean {
        return char == '[' || char == ']' || char == '(' || char == ')'
    }

    private fun consumeDelimiter(): String {
        val start = pos
        val char = input[pos]
        
        // Handle __u__ specifically as it's a multi-character delimiter we've used for underline
        if (input.startsWith("__u__", pos)) {
            pos += 5
            return "__u__"
        }
        
        while (pos < input.length && input[pos] == char) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun consumeText(): String {
        val start = pos
        while (pos < input.length) {
            val char = input[pos]
            if (char == '\\' || char == '`' || char == '\n' || isDelimiterStart(char) || isSpecial(char)) {
                break
            }
            pos++
        }
        return input.substring(start, pos)
    }
}
