package com.example.devfastjavafx.ui.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.io.ByteArrayOutputStream
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image as SkiaImage
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager

@Composable
fun MarkdownRenderer(blocks: List<MarkdownBlock>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(blocks) { block ->
            when (block) {
                is MarkdownBlock.Heading -> HeadingBlock(block)
                is MarkdownBlock.Text -> TextBlock(block)
                is MarkdownBlock.Code -> CodeBlock(block)
                is MarkdownBlock.PlantUML -> PlantUMLBlock(block)
                is MarkdownBlock.Image -> ImageBlock(block)
                is MarkdownBlock.Error -> ErrorBlock(block)
            }
        }
    }
}

@Composable
fun HeadingBlock(block: MarkdownBlock.Heading) {
    val fontSize = when (block.level) {
        1 -> 24.sp
        2 -> 20.sp
        3 -> 18.sp
        else -> 16.sp
    }
    Text(
        text = block.text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun TextBlock(block: MarkdownBlock.Text) {
    Text(text = block.content)
}

@Composable
fun CodeBlock(block: MarkdownBlock.Code) {
    val annotatedContent = remember(block.content, block.language) {
        highlightCode(block.content, block.language)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2B2B), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = block.filename, color = Color.Gray, fontSize = 12.sp)
            Row {
                ActionButton(onClick = {
                    CopyPasteManager.getInstance().setContents(StringSelection(block.content))
                }) {
                    Text("Copy")
                }
                Spacer(modifier = Modifier.width(4.dp))
                ActionButton(onClick = {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return@ActionButton
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@ActionButton
                    val document = editor.document
                    val caretModel = editor.caretModel
                    val offset = caretModel.offset

                    WriteCommandAction.runWriteCommandAction(project) {
                        document.insertString(offset, block.content)
                    }
                }) {
                    Text("Insert")
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = annotatedContent,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}

fun highlightCode(content: String, language: String): AnnotatedString = buildAnnotatedString {
    val keywords = setOf("class", "public", "private", "protected", "fun", "val", "var", "import", "package", "if", "else", "for", "while", "return", "void", "static")
    val stringColor = Color(0xFF6A8759)
    val keywordColor = Color(0xFFCC7832)
    val commentColor = Color(0xFF808080)
    val defaultColor = Color(0xFFA9B7C6)

    val regex = Regex("""("[^"]*"|//.*|/\*.*?\*/|\b\w+\b|\s+|.)""", RegexOption.DOT_MATCHES_ALL)

    regex.findAll(content).forEach { match ->
        val text = match.value
        when {
            text.startsWith("\"") -> withStyle(style = SpanStyle(color = stringColor)) { append(text) }
            text.startsWith("//") || text.startsWith("/*") -> withStyle(style = SpanStyle(color = commentColor)) { append(text) }
            keywords.contains(text) -> withStyle(style = SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) { append(text) }
            else -> withStyle(style = SpanStyle(color = defaultColor)) { append(text) }
        }
    }
}

@Composable
fun PlantUMLBlock(block: MarkdownBlock.PlantUML) {
    val imageBitmap = remember(block.content) {
        try {
            val reader = SourceStringReader(block.content)
            val os = ByteArrayOutputStream()
            reader.outputImage(os, FileFormatOption(FileFormat.PNG))
            val bytes = os.toByteArray()
            val skiaImage = SkiaImage.makeFromEncoded(bytes)
            val bitmap = Bitmap.makeFromImage(skiaImage)
            bitmap.asComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Diagram: ${block.filename}", fontWeight = FontWeight.SemiBold)
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = block.filename,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ErrorBlock(MarkdownBlock.Error("Failed to render PlantUML: ${block.filename}"))
        }
    }
}

@Composable
fun ImageBlock(block: MarkdownBlock.Image) {
    Text(text = "[Image: ${block.altText ?: block.url}]", color = Color.Blue)
}

@Composable
fun ErrorBlock(block: MarkdownBlock.Error) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(text = block.message, color = Color.Red)
    }
}
