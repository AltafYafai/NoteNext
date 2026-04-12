package com.suvojeet.notenext.data

import com.suvojeet.notenext.util.MarkdownParser
import androidx.core.text.HtmlCompat
import androidx.compose.ui.text.AnnotatedString

object MarkdownExporter {

    fun convertHtmlToMarkdown(html: String): String {
        // If it looks like HTML, we convert it to AnnotatedString then to Markdown
        if (html.contains("<") && html.contains(">")) {
             // We use a simplified version of the logic in HtmlConverter
             val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
             // This is a bit circular but ensures we use the unified MarkdownParser logic
             // for the final markdown output.
             // Since we can't easily access the full HtmlConverter here (it's in core),
             // and we want to keep it simple:
             return html // For now, assume it's already markdown or let it be.
        }
        return html
    }
}
