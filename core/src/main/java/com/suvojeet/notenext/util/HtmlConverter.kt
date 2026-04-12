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

    suspend fun annotatedStringToHtml(annotatedString: AnnotatedString): String = withContext(Dispatchers.Default) {
        MarkdownParser.toMarkdown(annotatedString)
    }

    suspend fun htmlToPlainText(html: String): String = withContext(Dispatchers.Default) {
        // If it's Markdown, commonmark can convert it to text content, 
        // but for now, we'll use HtmlCompat to strip any HTML tags if present.
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }

    suspend fun htmlToAnnotatedString(html: String): AnnotatedString = withContext(Dispatchers.Default) {
        // We first use HtmlCompat to handle legacy HTML notes.
        // If the note is already Markdown, HtmlCompat.fromHtml will just return it as is.
        val legacySpanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val textContent = legacySpanned.toString()
        
        // Then we use MarkdownParser to handle all Markdown features and styling.
        MarkdownParser.toAnnotatedString(textContent)
    }
}
