package com.suvojeet.notenext.core.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object MarkdownConverter {

    fun fromAnnotatedString(annotatedString: AnnotatedString): String {
        val text = annotatedString.text
        if (text.isEmpty()) return ""

        val styles = Array(text.length) { mutableSetOf<StyleType>() }

        annotatedString.spanStyles.forEach { range ->
            val style = range.item
            val start = range.start.coerceIn(0, text.length)
            val end = range.end.coerceIn(0, text.length)
            
            for (i in start until end) {
                if (style.fontWeight == FontWeight.Bold) styles[i].add(StyleType.BOLD)
                if (style.fontStyle == FontStyle.Italic) styles[i].add(StyleType.ITALIC)
                if (style.textDecoration == TextDecoration.LineThrough) styles[i].add(StyleType.STRIKE)
                if (style.textDecoration == TextDecoration.Underline) styles[i].add(StyleType.UNDERLINE)
            }
        }

        val result = StringBuilder()
        val activeStyles = mutableSetOf<StyleType>()

        for (i in 0 until text.length) {
            val charStyles = styles[i]

            // Close styles that are no longer active
            // Order matters for nesting: Strike -> Italic -> Bold (arbitrary but consistent)
            if (StyleType.UNDERLINE in activeStyles && StyleType.UNDERLINE !in charStyles) {
                result.append("</u>"); activeStyles.remove(StyleType.UNDERLINE)
            }
            if (StyleType.STRIKE in activeStyles && StyleType.STRIKE !in charStyles) {
                result.append("~~"); activeStyles.remove(StyleType.STRIKE)
            }
            if (StyleType.ITALIC in activeStyles && StyleType.ITALIC !in charStyles) {
                result.append("_"); activeStyles.remove(StyleType.ITALIC)
            }
            if (StyleType.BOLD in activeStyles && StyleType.BOLD !in charStyles) {
                result.append("**"); activeStyles.remove(StyleType.BOLD)
            }

            // Open new styles
            if (StyleType.BOLD in charStyles && StyleType.BOLD !in activeStyles) {
                result.append("**"); activeStyles.add(StyleType.BOLD)
            }
            if (StyleType.ITALIC in charStyles && StyleType.ITALIC !in activeStyles) {
                result.append("_"); activeStyles.add(StyleType.ITALIC)
            }
            if (StyleType.STRIKE in charStyles && StyleType.STRIKE !in activeStyles) {
                result.append("~~"); activeStyles.add(StyleType.STRIKE)
            }
            if (StyleType.UNDERLINE in charStyles && StyleType.UNDERLINE !in activeStyles) {
                result.append("<u>"); activeStyles.add(StyleType.UNDERLINE)
            }

            result.append(text[i])
        }

        // Close any remaining open styles
        if (StyleType.UNDERLINE in activeStyles) result.append("</u>")
        if (StyleType.STRIKE in activeStyles) result.append("~~")
        if (StyleType.ITALIC in activeStyles) result.append("_")
        if (StyleType.BOLD in activeStyles) result.append("**")

        return result.toString()
    }

    private enum class StyleType {
        BOLD, ITALIC, STRIKE, UNDERLINE
    }
}
