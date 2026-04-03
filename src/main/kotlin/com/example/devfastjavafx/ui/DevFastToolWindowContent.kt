package com.example.devfastjavafx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.devfastjavafx.api.ComponentMetadata
import com.example.devfastjavafx.cache.TemplateCache
import com.example.devfastjavafx.service.FavoritesService
import com.example.devfastjavafx.ui.markdown.MarkdownParser
import com.example.devfastjavafx.ui.markdown.MarkdownRenderer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import androidx.compose.ui.Alignment
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import java.nio.file.Paths

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun DevFastToolWindowContent(project: Project, showOnlyFavorites: Boolean = false) {
    val allFiles = remember { TemplateCache.listCachedTemplates() }
    val componentsMetadata = remember(allFiles) {
        allFiles.filter { it.endsWith("manifest.json") }
            .mapNotNull { path ->
                TemplateCache.loadTemplate(path)?.let { content ->
                    try {
                        val metadata = json.decodeFromString<ComponentMetadata>(content)
                        val id = path.substringBeforeLast("/manifest.json").substringAfterLast("/")
                        metadata.copy(id = id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
    var selectedComponent by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val favoritesService = remember { FavoritesService.getInstance() }
    var favoritesUpdated by remember { mutableLongStateOf(0L) }

    val filteredComponents = remember(componentsMetadata, searchQuery, favoritesUpdated, showOnlyFavorites) {
        val baseList = if (showOnlyFavorites) {
            componentsMetadata.filter { favoritesService.isFavorite(it.id) }.sortedBy { it.name }
        } else {
            componentsMetadata.sortedWith(
                compareByDescending<ComponentMetadata> { favoritesService.isFavorite(it.id) }
                    .thenBy { it.name }
            )
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { component ->
                component.name.contains(searchQuery, ignoreCase = true) ||
                        component.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

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
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search components...") }
                )
                Divider(orientation = Orientation.Horizontal)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredComponents, key = { it.id }) { component ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedComponent = component.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = component.name, modifier = Modifier.weight(1f))

                            val isFavorite = favoritesService.isFavorite(component.id)
                            ActionButton(
                                onClick = {
                                    favoritesService.toggleFavorite(component.id)
                                    favoritesUpdated++
                                }
                            ) {
                                Text(if (isFavorite) "★" else "☆")
                            }
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
                        MarkdownRenderer(blocks, project)
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
