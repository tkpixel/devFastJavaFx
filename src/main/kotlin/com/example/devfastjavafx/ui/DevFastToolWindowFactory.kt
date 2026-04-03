package com.example.devfastjavafx.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab

class DevFastToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Templates") {
            DevFastToolWindowContent(project)
        }
        toolWindow.addComposeTab("Favorites") {
            DevFastToolWindowContent(project, showOnlyFavorites = true)
        }
    }
}
