package com.suvojeet.notenext.util

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HtmlConverter {

    suspend fun annotatedStringToMarkdown(annotatedString: AnnotatedString): String = withContext(Dispatchers.Default) {
        MarkdownParser.toMarkdown(annotatedString)
    }

    suspend fun htmlToPlainText(html: String): String = withContext(Dispatchers.Default) {
        // If it's Markdown, commonmark can convert it to text content, 
        // but for now, we'll use HtmlCompat to strip any HTML tags if present.
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }

    suspend fun htmlToAnnotatedString(html: String): AnnotatedString = withContext(Dispatchers.Default) {
        // If it looks like HTML, we convert basic formatting tags to Markdown symbols
        // to preserve legacy note formatting in the new Markdown system.
        val processedText = if (html.contains("<") && html.contains(">")) {
             html.replace("<b>", "**").replace("</b>", "**")
                 .replace("<strong>", "**").replace("</strong>", "**")
                 .replace("<i>", "*").replace("</i>", "*")
                 .replace("<em>", "*").replace("</em>", "*")
                 .replace("<u>", "<u>").replace("</u>", "</u>")
                 .replace("<strike>", "~~").replace("</strike>", "~~")
                 .replace("<del>", "~~").replace("</del>", "~~")
                 .replace("<s>", "~~").replace("</s>", "~~")
                 .replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
                 .let { 
                     // Strip any remaining HTML tags
                     HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                 }
        } else {
            html
        }
        
        MarkdownHighlighter.highlight(processedText)
    }
}
