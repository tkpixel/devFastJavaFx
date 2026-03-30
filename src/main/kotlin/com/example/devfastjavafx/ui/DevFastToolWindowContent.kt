package com.example.devfastjavafx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.devfastjavafx.cache.TemplateCache
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState

@Composable
fun DevFastToolWindowContent() {
    val templates = remember { TemplateCache.listCachedTemplates() }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    val splitLayoutState = rememberSplitLayoutState(0.3f)

    HorizontalSplitLayout(
        state = splitLayoutState,
        modifier = Modifier.fillMaxSize(),
        first = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Components",
                    modifier = Modifier.padding(8.dp)
                )
                Divider(orientation = Orientation.Horizontal)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(templates) { template ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = template }
                                .padding(8.dp)
                        ) {
                            Text(text = template)
                        }
                    }
                }
            }
        },
        second = {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTemplate != null) {
                    Text(text = "Details for: $selectedTemplate")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Template content preview will be here.")
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(text = "Select a component to see details")
                    }
                }
            }
        }
    )
}
