@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

import com.suvojeet.notenext.util.MarkdownParser

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = content.split("\n")
        
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = MarkdownParser.toAnnotatedString(line.removePrefix("# ").trim()),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = MarkdownParser.toAnnotatedString(line.removePrefix("## ").trim()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = MarkdownParser.toAnnotatedString(line.removePrefix("### ").trim()),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = MarkdownParser.toAnnotatedString(line.substring(2).trim()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = MarkdownParser.toAnnotatedString(line.removePrefix("> ").trim()),
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                line.trim() == "---" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = MarkdownParser.toAnnotatedString(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
