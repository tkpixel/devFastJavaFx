package com.example.devfastjavafx.settings

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

class GitLabSettingsComponent {
    private val gitlabUrlField = JBTextField()
    private val projectIdField = JBTextField()
    private val tokenField = JBPasswordField()

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
