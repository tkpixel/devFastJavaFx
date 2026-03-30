package com.example.devfastjavafx.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.nio.file.Files
import java.nio.file.Path

class MarkdownParser(private val componentDirectory: Path) {

    private val parser: Parser = Parser.builder(MutableDataSet()).build()

    fun parse(markdown: String): List<MarkdownBlock> {
        val document = parser.parse(markdown)
        val blocks = mutableListOf<MarkdownBlock>()

        var node = document.firstChild
        while (node != null) {
            visitNode(node, blocks)
            node = node.next
        }

        return blocks
    }

    private fun visitNode(node: Node, blocks: MutableList<MarkdownBlock>) {
        when (node) {
            is Heading -> {
                blocks.add(MarkdownBlock.Heading(node.text.toString(), node.level))
            }
            is Paragraph -> {
                val text = node.chars.toString().trim()
                when {
                    text.startsWith("@[code](") && text.endsWith(")") -> {
                        val filename = text.substringAfter("@[code](").substringBeforeLast(")")
                        blocks.add(resolveCodeBlock(filename))
                    }
                    text.startsWith("@[plantuml](") && text.endsWith(")") -> {
                        val filename = text.substringAfter("@[plantuml](").substringBeforeLast(")")
                        blocks.add(resolvePlantUmlBlock(filename))
                    }
                    else -> {
                        blocks.add(MarkdownBlock.Text(renderInlines(node)))
                    }
                }
            }
            is FencedCodeBlock -> {
                blocks.add(MarkdownBlock.Code("In-line code", node.contentChars.toString(), node.info.toString()))
            }
            is Image -> {
                blocks.add(MarkdownBlock.Image(node.url.toString(), node.title.toString()))
            }
            is BulletList, is OrderedList -> {
                blocks.add(MarkdownBlock.Text(renderInlines(node)))
            }
            else -> {
                val text = node.chars.toString()
                if (text.isNotBlank()) {
                    blocks.add(MarkdownBlock.Text(AnnotatedString(text)))
                }
            }
        }
    }

    private fun renderInlines(node: Node): AnnotatedString = buildAnnotatedString {
        var child = node.firstChild
        while (child != null) {
            when (child) {
                is Text -> append(child.chars.toString())
                is Emphasis -> withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(renderInlines(child))
                }
                is StrongEmphasis -> withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(renderInlines(child))
                }
                is Code -> withStyle(style = SpanStyle(background = Color.LightGray)) {
                    append(child.chars.toString())
                }
                is SoftLineBreak, is HardLineBreak -> append("\n")
                else -> append(child.chars.toString())
            }
            child = child.next
        }
    }

    private fun resolveCodeBlock(filename: String): MarkdownBlock {
        val safePath = validatePath(filename) ?: return MarkdownBlock.Error("Invalid file path: $filename")
        return if (Files.exists(safePath)) {
            try {
                val content = Files.readString(safePath)
                val extension = filename.substringAfterLast(".", "txt")
                MarkdownBlock.Code(filename, content, extension)
            } catch (e: Exception) {
                MarkdownBlock.Error("Error reading file $filename: ${e.message}")
            }
        } else {
            MarkdownBlock.Error("File not found: $filename")
        }
    }

    private fun resolvePlantUmlBlock(filename: String): MarkdownBlock {
        val safePath = validatePath(filename) ?: return MarkdownBlock.Error("Invalid file path: $filename")
        return if (Files.exists(safePath)) {
            try {
                val content = Files.readString(safePath)
                MarkdownBlock.PlantUML(filename, content)
            } catch (e: Exception) {
                MarkdownBlock.Error("Error reading file $filename: ${e.message}")
            }
        } else {
            MarkdownBlock.Error("File not found: $filename")
        }
    }

    private fun validatePath(filename: String): Path? {
        val resolvedPath = componentDirectory.resolve(filename).normalize()
        return if (resolvedPath.startsWith(componentDirectory.normalize())) {
            resolvedPath
        } else {
            null
        }
    }
}
