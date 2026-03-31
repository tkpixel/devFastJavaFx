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
import com.example.devfastjavafx.service.TemplateLoaderService
import com.example.devfastjavafx.ui.markdown.MarkdownParser
import com.example.devfastjavafx.ui.markdown.MarkdownRenderer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import androidx.compose.ui.Alignment
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import java.nio.file.Paths

private val json = Json { ignoreUnknownKeys = true }

sealed class TemplateTreeItem {
    data class Folder(
        val name: String,
        val relativePath: String,
        val children: MutableMap<String, TemplateTreeItem> = mutableMapOf(),
        var isExpanded: Boolean = false
    ) : TemplateTreeItem()

    data class Component(
        val metadata: ComponentMetadata,
        val relativePath: String
    ) : TemplateTreeItem()
}

@Composable
fun DevFastToolWindowContent(project: Project) {
    var refreshTick by remember { mutableLongStateOf(0L) }
    var isRefreshing by remember { mutableStateOf(false) }

    val allFiles = remember(refreshTick) { TemplateCache.listCachedTemplates() }
    val componentsMetadata = remember(allFiles) {
        allFiles.filter { it.endsWith("manifest.json") || it.endsWith("manifest.json".replace("/", "\\")) }
            .mapNotNull { path ->
                TemplateCache.loadTemplate(path)?.let { content ->
                    try {
                        val metadata = json.decodeFromString<ComponentMetadata>(content)
                        val normalizedPath = path.replace("\\", "/")
                        val relativePath = normalizedPath.substringBeforeLast("/manifest.json")
                        metadata.copy(id = relativePath) // Use the full relative path as the unique ID
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    val treeRoot = remember(componentsMetadata) {
        val root = TemplateTreeItem.Folder("", "")
        for (component in componentsMetadata) {
            val normalizedId = component.id.replace("\\", "/")
            val parts = normalizedId.split("/")
            var currentFolder = root
            for (i in 0 until parts.size - 1) {
                val folderName = parts[i]
                val folderPath = parts.take(i + 1).joinToString("/")
                currentFolder = currentFolder.children.getOrPut(folderName) {
                    TemplateTreeItem.Folder(folderName, folderPath)
                } as TemplateTreeItem.Folder
            }
            val componentName = parts.last()
            currentFolder.children[componentName] = TemplateTreeItem.Component(component, normalizedId)
        }
        root
    }

    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var selectedComponent by remember { mutableStateOf<String?>(null) }
    val searchState = rememberTextFieldState("")

    val favoritesService = remember { FavoritesService.getInstance() }
    var favoritesUpdated by remember { mutableLongStateOf(0L) }

    val displayItems = remember(treeRoot, searchState.text, favoritesUpdated, expandedFolders) {
        val items = mutableListOf<Pair<Int, TemplateTreeItem>>()
        val searchQuery = searchState.text.toString()

        fun addItems(folder: TemplateTreeItem.Folder, depth: Int) {
            val sortedChildren = folder.children.values.sortedWith { a, b ->
                when {
                    a is TemplateTreeItem.Folder && b is TemplateTreeItem.Component -> -1
                    a is TemplateTreeItem.Component && b is TemplateTreeItem.Folder -> 1
                    a is TemplateTreeItem.Folder && b is TemplateTreeItem.Folder -> a.name.compareTo(b.name)
                    a is TemplateTreeItem.Component && b is TemplateTreeItem.Component -> {
                        val favA = favoritesService.isFavorite(a.metadata.id)
                        val favB = favoritesService.isFavorite(b.metadata.id)
                        if (favA != favB) if (favA) -1 else 1
                        else a.metadata.name.compareTo(b.metadata.name)
                    }
                    else -> 0
                }
            }

            for (item in sortedChildren) {
                if (searchQuery.isNotBlank()) {
                    // In search mode, we flatten everything that matches
                    if (item is TemplateTreeItem.Component) {
                        if (item.metadata.name.contains(searchQuery, ignoreCase = true) ||
                            item.metadata.tags.any { it.contains(searchQuery, ignoreCase = true) }
                        ) {
                            items.add(depth to item)
                        }
                    } else if (item is TemplateTreeItem.Folder) {
                        addItems(item, depth)
                    }
                } else {
                    items.add(depth to item)
                    if (item is TemplateTreeItem.Folder && expandedFolders.contains(item.relativePath)) {
                        addItems(item, depth + 1)
                    }
                }
            }
        }

        addItems(treeRoot, 0)
        items
    }

    val splitLayoutState = rememberSplitLayoutState(0.3f)

    HorizontalSplitLayout(
        state = splitLayoutState,
        modifier = Modifier.fillMaxSize(),
        first = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "JavaFX Components")
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        ActionButton(onClick = {
                            isRefreshing = true
                            TemplateCache.clearCache()
                            TemplateLoaderService.getInstance().loadTemplates {
                                refreshTick++
                                isRefreshing = false
                            }
                        }) {
                            Text("↻")
                        }
                    }
                }
                TextField(
                    state = searchState,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search components...") }
                )
                Divider(orientation = Orientation.Horizontal)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayItems, key = { (it.second as? TemplateTreeItem.Component)?.metadata?.id ?: (it.second as TemplateTreeItem.Folder).relativePath }) { (depth, item) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item is TemplateTreeItem.Folder) {
                                        expandedFolders = if (expandedFolders.contains(item.relativePath)) {
                                            expandedFolders - item.relativePath
                                        } else {
                                            expandedFolders + item.relativePath
                                        }
                                    } else if (item is TemplateTreeItem.Component) {
                                        selectedComponent = item.metadata.id
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .padding(start = (depth * 16).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (item is TemplateTreeItem.Folder) {
                                Text(
                                    text = if (expandedFolders.contains(item.relativePath)) "▼" else "▶",
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(text = item.name, modifier = Modifier.weight(1f))
                            } else if (item is TemplateTreeItem.Component) {
                                Spacer(modifier = Modifier.width(20.dp))
                                Text(text = item.metadata.name, modifier = Modifier.weight(1f))

                                val isFavorite = favoritesService.isFavorite(item.metadata.id)
                                ActionButton(
                                    onClick = {
                                        favoritesService.toggleFavorite(item.metadata.id)
                                        favoritesUpdated++
                                    }
                                ) {
                                    Text(if (isFavorite) "★" else "☆")
                                }
                            }
                        }
                    }
                }
            }
        },
        second = {
            if (selectedComponent != null) {
                val normalizedSelectedId = selectedComponent!!.replace("\\", "/")
                val componentDirPath = Paths.get(PathManager.getSystemPath(), "devFastJavaFx/templates", normalizedSelectedId)
                val markdownParser = remember(normalizedSelectedId) { MarkdownParser(componentDirPath) }

                val readmePath = allFiles.find {
                    val normalizedPath = it.replace("\\", "/")
                    val parentDir = if (normalizedPath.contains("/")) normalizedPath.substringBeforeLast("/") else ""
                    parentDir == normalizedSelectedId && normalizedPath.endsWith("README.md")
                }
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
