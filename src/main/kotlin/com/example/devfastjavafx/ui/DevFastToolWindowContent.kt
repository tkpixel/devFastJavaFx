package com.example.devfastjavafx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.devfastjavafx.cache.TemplateCache
import com.example.devfastjavafx.ui.markdown.MarkdownParser
import com.example.devfastjavafx.ui.markdown.MarkdownRenderer
import com.intellij.openapi.application.PathManager
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import java.nio.file.Paths

@Composable
fun DevFastToolWindowContent() {
    val allFiles = remember { TemplateCache.listCachedTemplates() }
    val components = remember(allFiles) {
        // Group by directory and identify components by presence of manifest.json
        allFiles.filter { it.endsWith("manifest.json") }
            .map { it.substringBeforeLast("/manifest.json").substringAfterLast("/") }
            .distinct()
    }
    var selectedComponent by remember { mutableStateOf<String?>(null) }
    val splitLayoutState = rememberSplitLayoutState(0.3f)

    HorizontalSplitLayout(
        state = splitLayoutState,
        modifier = Modifier.fillMaxSize(),
        first = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "JavaFX Components",
                    modifier = Modifier.padding(8.dp)
                )
                Divider(orientation = Orientation.Horizontal)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(components) { component ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedComponent = component }
                                .padding(8.dp)
                        ) {
                            Text(text = component)
                        }
                    }
                }
            }
        },
        second = {
            if (selectedComponent != null) {
                val componentDirPath = Paths.get(PathManager.getSystemPath(), "devFastJavaFx/templates", selectedComponent!!)
                val markdownParser = remember(selectedComponent) { MarkdownParser(componentDirPath) }

                val readmePath = allFiles.find { it.contains(selectedComponent!!) && it.endsWith("README.md") }
                if (readmePath != null) {
                    val readmeContent = TemplateCache.loadTemplate(readmePath)
                    if (readmeContent != null) {
                        val blocks = remember(readmeContent) { markdownParser.parse(readmeContent) }
                        MarkdownRenderer(blocks)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = "No README.md found for component: $selectedComponent")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(text = "Select a component to see details")
                }
            }
        }
    )
}
