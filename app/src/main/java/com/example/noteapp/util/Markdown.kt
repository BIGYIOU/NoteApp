package com.example.noteapp.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    return buildAnnotatedString {
        var inCodeBlock = false

        markdown.lines().forEachIndexed { index, rawLine ->
            if (index > 0) append("\n")

            val line = rawLine

            // Check for code block fence
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEachIndexed
            }

            if (inCodeBlock) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    background = colorScheme.surfaceVariant,
                    color = colorScheme.onSurfaceVariant
                )) {
                    append(line)
                }
                return@forEachIndexed
            }

            when {
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colorScheme.onSurface
                    )) {
                        append(line.removePrefix("### "))
                    }
                }
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colorScheme.onSurface
                    )) {
                        append(line.removePrefix("## "))
                    }
                }
                line.startsWith("# ") -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colorScheme.primary
                    )) {
                        append(line.removePrefix("# "))
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    withStyle(SpanStyle(color = colorScheme.onSurface)) {
                        append("  •  ")
                        append(parseInline(line.removePrefix("- ").removePrefix("* ")))
                    }
                }
                line.startsWith("> ") -> {
                    withStyle(SpanStyle(
                        color = colorScheme.outline,
                        fontStyle = FontStyle.Italic
                    )) {
                        append("▎")
                        append(parseInline(line.removePrefix("> ")))
                    }
                }
                line.isBlank() -> { /* skip blank lines but keep newline */ }
                else -> {
                    withStyle(SpanStyle(color = colorScheme.onSurface)) {
                        append(parseInline(line))
                    }
                }
            }
        }
    }
}

fun parseInline(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) && i + 1 < text.length && text[i + 1] != ' ' -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            background = androidx.compose.ui.graphics.Color(0xFFE8E0EC)
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
