package com.example.devfastjavafx.settings

import com.example.devfastjavafx.api.GitLabClient
import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import io.ktor.client.plugins.*
import kotlinx.coroutines.*
import javax.swing.JPanel

class GitLabSettingsComponent(private val scope: CoroutineScope) {
    private val gitlabUrlField = JBTextField()
    private val projectIdField = JBTextField()
    private val tokenField = JBPasswordField()
    private val feedbackLabel = JBLabel()

    val panel: JPanel = panel {
        group("GitLab Connection") {
            row("GitLab URL:") {
                cell(gitlabUrlField).align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Project ID:") {
                cell(projectIdField).align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Personal Access Token:") {
                cell(tokenField).align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row {
                button("Test Connection") {
                    testConnection()
                }
                cell(feedbackLabel)
            }
        }
    }

    private fun testConnection() {
        val url = gitlabUrl
        val id = projectId
        val token = token

        feedbackLabel.text = "Testing..."
        feedbackLabel.icon = AnimatedIcon.Default.INSTANCE
        feedbackLabel.isVisible = true

        scope.launch {
            val result = try {
                val client = withContext(Dispatchers.IO) { GitLabClient(url, token) }
                try {
                    withContext(Dispatchers.IO) { client.getProject(id) }
                    Result.success("Connection successful")
                } finally {
                    withContext(Dispatchers.IO) { client.close() }
                }
            } catch (e: ClientRequestException) {
                if (e.response.status.value == 401) {
                    Result.failure(Exception("Authentication failed: Invalid Token (HTTP 401)"))
                } else if (e.response.status.value == 404) {
                    Result.failure(Exception("Connection failed: Repository not found (HTTP 404)"))
                } else {
                    Result.failure(Exception("Connection failed: ${e.message ?: "Status ${e.response.status}"}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Connection failed: ${e.message ?: "Unknown error"}"))
            }

            result.onSuccess { message ->
                feedbackLabel.text = message
                feedbackLabel.icon = AllIcons.General.InspectionsOK
            }.onFailure { error ->
                feedbackLabel.text = error.message ?: "Unknown error"
                feedbackLabel.icon = AllIcons.General.Error
            }
        }
    }

    var gitlabUrl: String
        get() = gitlabUrlField.text
        set(value) {
            gitlabUrlField.text = value
        }

    var projectId: String
        get() = projectIdField.text
        set(value) {
            projectIdField.text = value
        }

    var token: String
        get() = String(tokenField.password)
        set(value) {
            tokenField.text = value
        }
}
