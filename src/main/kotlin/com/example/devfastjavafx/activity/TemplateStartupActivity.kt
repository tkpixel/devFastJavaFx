package com.example.devfastjavafx.activity

import com.example.devfastjavafx.service.TemplateLoaderService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class TemplateStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val service = ApplicationManager.getApplication().getService(TemplateLoaderService::class.java)
        service.loadTemplates()
    }
}
