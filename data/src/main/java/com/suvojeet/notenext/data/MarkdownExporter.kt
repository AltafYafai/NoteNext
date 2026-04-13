package com.suvojeet.notenext.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

object MarkdownExporter {

    /**
     * Converts legacy HTML content to Markdown.
     * Essential for migrating older notes to the new Markdown-first system.
     */
    fun convertHtmlToMarkdown(html: String): String {
        if (!html.contains("<") || !html.contains(">")) return html
        
        val doc = Jsoup.parseBodyFragment(html)
        val sb = StringBuilder()
        traverse(doc.body(), sb)
        return sb.toString().trim()
    }

    private fun traverse(element: Element, sb: StringBuilder) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> sb.append(node.text())
                is Element -> {
                    when (node.tagName()) {
                        "b", "strong" -> {
                            sb.append("**")
                            traverse(node, sb)
                            sb.append("**")
                        }
                        "i", "em" -> {
                            sb.append("*")
                            traverse(node, sb)
                            sb.append("*")
                        }
                        "u" -> {
                            sb.append("<u>")
                            traverse(node, sb)
                            sb.append("</u>")
                        }
                        "strike", "del", "s" -> {
                            sb.append("~~")
                            traverse(node, sb)
                            sb.append("~~")
                        }
                        "br" -> sb.append("\n")
                        "p", "div" -> {
                            traverse(node, sb)
                            sb.append("\n")
                        }
                        "h1" -> { sb.append("# "); traverse(node, sb); sb.append("\n") }
                        "h2" -> { sb.append("## "); traverse(node, sb); sb.append("\n") }
                        "h3" -> { sb.append("### "); traverse(node, sb); sb.append("\n") }
                        "ul" -> { traverse(node, sb); sb.append("\n") }
                        "li" -> {
                            sb.append("- ")
                            traverse(node, sb)
                            sb.append("\n")
                        }
                        else -> traverse(node, sb)
                    }
                }
            }
        }
    }
}
