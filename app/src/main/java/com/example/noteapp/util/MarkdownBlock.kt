package com.example.noteapp.util

sealed class MarkdownBlock {
    data class Heading(val level: Int, val rawText: String) : MarkdownBlock()
    data class Paragraph(val rawText: String) : MarkdownBlock()
    data class Image(val alt: String, val src: String) : MarkdownBlock()
    data class Audio(val src: String, val durationMs: Long = 0) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
    data class Quote(val rawText: String) : MarkdownBlock()
    data class ListItem(val rawText: String, val ordered: Boolean) : MarkdownBlock()
    data object BlankLine : MarkdownBlock()
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val imageRegex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
    var inCodeBlock = false
    val codeLines = mutableListOf<String>()

    fun flushCodeBlock() {
        if (codeLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            codeLines.clear()
        }
    }

    markdown.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()

        if (line.startsWith("```")) {
            if (inCodeBlock) {
                flushCodeBlock()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            return@forEach
        }

        if (inCodeBlock) {
            codeLines.add(line)
            return@forEach
        }

        if (line.isBlank()) {
            blocks.add(MarkdownBlock.BlankLine)
            return@forEach
        }

        when {
            imageRegex.matches(line.trim()) -> {
                val match = imageRegex.find(line.trim())!!
                val alt = match.groupValues[1]
                val src = match.groupValues[2]
                if (alt == "audio") {
                    blocks.add(MarkdownBlock.Audio(src = src))
                } else {
                    blocks.add(MarkdownBlock.Image(alt = alt, src = src))
                }
            }
            line.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(3, line.removePrefix("### ")))
            }
            line.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(2, line.removePrefix("## ")))
            }
            line.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(1, line.removePrefix("# ")))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                val text = line.removePrefix("- ").removePrefix("* ")
                blocks.add(MarkdownBlock.ListItem(text, ordered = false))
            }
            line.startsWith("> ") -> {
                blocks.add(MarkdownBlock.Quote(line.removePrefix("> ")))
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(line))
            }
        }
    }

    flushCodeBlock()
    return blocks
}
