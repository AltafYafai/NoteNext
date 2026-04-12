package com.suvojeet.notenext.core.markdown

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon

interface MarkdownParser {
    fun setMarkdown(textView: TextView, markdown: String)
    fun getMarkwon(): Markwon
}

class MarkwonParserImpl(
    private val markwon: Markwon
) : MarkdownParser {
    override fun setMarkdown(textView: TextView, markdown: String) {
        markwon.setMarkdown(textView, markdown)
    }

    override fun getMarkwon(): Markwon = markwon
}
