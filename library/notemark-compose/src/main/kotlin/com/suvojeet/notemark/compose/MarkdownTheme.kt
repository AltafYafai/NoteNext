package com.suvojeet.notemark.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Immutable
data class MarkdownColors(
    val text: Color,
    val header: Color,
    val link: Color,
    val code: Color,
    val codeBackground: Color,
    val blockquoteBar: Color,
    val blockquoteBackground: Color,
    val divider: Color
)

@Immutable
data class MarkdownTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val h6: TextStyle,
    val body: TextStyle,
    val code: TextStyle,
    val blockquote: TextStyle
)

@Immutable
data class MarkdownSpacing(
    val block: Dp,
    val listIndent: Dp
)

@Immutable
data class MarkdownTheme(
    val colors: MarkdownColors,
    val typography: MarkdownTypography,
    val spacing: MarkdownSpacing
) {
    companion object {
        @Composable
        fun default(): MarkdownTheme {
            val m3Colors = MaterialTheme.colorScheme
            val m3Typography = MaterialTheme.typography
            
            return MarkdownTheme(
                colors = MarkdownColors(
                    text = m3Colors.onSurface,
                    header = m3Colors.onSurface,
                    link = m3Colors.primary,
                    code = m3Colors.secondary,
                    codeBackground = m3Colors.surfaceVariant,
                    blockquoteBar = m3Colors.outlineVariant,
                    blockquoteBackground = m3Colors.surfaceVariant.copy(alpha = 0.3f),
                    divider = m3Colors.outlineVariant
                ),
                typography = MarkdownTypography(
                    h1 = m3Typography.headlineLarge,
                    h2 = m3Typography.headlineMedium,
                    h3 = m3Typography.headlineSmall,
                    h4 = m3Typography.titleLarge,
                    h5 = m3Typography.titleMedium,
                    h6 = m3Typography.titleSmall,
                    body = m3Typography.bodyLarge,
                    code = m3Typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    blockquote = m3Typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                ),
                spacing = MarkdownSpacing(
                    block = 12.dp,
                    listIndent = 24.dp
                )
            )
        }
    }
}

internal val LocalMarkdownTheme = staticCompositionLocalOf<MarkdownTheme> {
    error("No MarkdownTheme provided")
}
