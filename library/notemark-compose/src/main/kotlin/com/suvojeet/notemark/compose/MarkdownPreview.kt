package com.suvojeet.notemark.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.suvojeet.notemark.core.NoteMarkParser
import com.suvojeet.notemark.core.model.*

/**
 * The main Composable to render Markdown content.
 */
@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.default(),
    onLinkClick: (String) -> Unit = {}
) {
    val document = remember(content) { NoteMarkParser.parse(content) }
    
    CompositionLocalProvider(LocalMarkdownTheme provides theme) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.block)
        ) {
            document.children.forEach { block ->
                MarkdownBlock(block, onLinkClick)
            }
        }
    }
}

@Composable
internal fun MarkdownBlock(
    block: BlockNode,
    onLinkClick: (String) -> Unit
) {
    val theme = LocalMarkdownTheme.current
    
    when (block) {
        is HeaderNode -> {
            val style = when (block.level) {
                1 -> theme.typography.h1
                2 -> theme.typography.h2
                3 -> theme.typography.h3
                4 -> theme.typography.h4
                5 -> theme.typography.h5
                else -> theme.typography.h6
            }
            Text(
                text = renderInlines(block.children),
                style = style,
                color = theme.colors.header
            )
        }
        is ParagraphNode -> {
            MarkdownRichText(
                annotatedString = renderInlines(block.children),
                onLinkClick = onLinkClick
            )
        }
        is BlockQuoteNode -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = theme.colors.blockquoteBar,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .background(theme.colors.blockquoteBackground)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
            ) {
                block.children.forEach { childBlock ->
                    MarkdownBlock(childBlock, onLinkClick)
                }
            }
        }
        is CodeBlockNode -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.colors.codeBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = block.content,
                    modifier = Modifier.padding(12.dp),
                    style = theme.typography.code,
                    color = theme.colors.code
                )
            }
        }
        is HorizontalRuleNode -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = theme.colors.divider
            )
        }
        else -> { /* Handle others like list items later */ }
    }
}

@Composable
internal fun MarkdownRichText(
    annotatedString: AnnotatedString,
    onLinkClick: (String) -> Unit
) {
    val theme = LocalMarkdownTheme.current
    
    // In M3 1.5.0-alpha17, we use SelectionContainer or just Text if clickable not needed.
    // For clickable links, we use a specialized Text with onClick in newer Compose versions.
    
    Text(
        text = annotatedString,
        style = theme.typography.body,
        color = theme.colors.text
    )
}

@Composable
internal fun renderInlines(inlines: List<InlineNode>): AnnotatedString {
    val theme = LocalMarkdownTheme.current
    
    return buildAnnotatedString {
        inlines.forEach { inline ->
            when (inline) {
                is TextNode -> append(inline.text)
                is BoldNode -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(renderInlines(inline.children))
                    }
                }
                is ItalicNode -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(renderInlines(inline.children))
                    }
                }
                is UnderlineNode -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(renderInlines(inline.children))
                    }
                }
                is InlineCodeNode -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = theme.typography.code.fontFamily,
                            background = theme.colors.codeBackground,
                            color = theme.colors.code
                        )
                    ) {
                        append(inline.code)
                    }
                }
                is LinkNode -> {
                    pushStringAnnotation(tag = "URL", annotation = inline.url)
                    withStyle(
                        SpanStyle(
                            color = theme.colors.link,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(renderInlines(inline.children))
                    }
                    pop()
                }
                is WikiLinkNode -> {
                     pushStringAnnotation(tag = "WIKI", annotation = inline.title)
                     withStyle(
                        SpanStyle(
                            color = theme.colors.link,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("[[${inline.title}]]")
                    }
                    pop()
                }
            }
        }
    }
}
