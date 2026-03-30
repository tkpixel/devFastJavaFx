package com.example.devfastjavafx.ui.markdown

import androidx.compose.ui.text.AnnotatedString

sealed class MarkdownBlock {
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class Text(val content: AnnotatedString) : MarkdownBlock()
    data class Code(val filename: String, val content: String, val language: String) : MarkdownBlock()
    data class PlantUML(val filename: String, val content: String) : MarkdownBlock()
    data class Image(val url: String, val altText: String? = null) : MarkdownBlock()
    data class Error(val message: String) : MarkdownBlock()
}
