package com.example.devfastjavafx.settings

import com.example.devfastjavafx.credentials.GitLabCredentialsManager
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class GitLabSettingsConfigurable : Configurable {
    private var mySettingsComponent: GitLabSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "DevFast GitLab Settings"
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = GitLabSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val state = GitLabSettingsState.getInstance().state
        return mySettingsComponent!!.gitlabUrl != state.gitlabUrl ||
                mySettingsComponent!!.projectId != state.projectId ||
                mySettingsComponent!!.token != (GitLabCredentialsManager.getToken() ?: "")
    }

    override fun apply() {
        val state = GitLabSettingsState.getInstance().state
        state.gitlabUrl = mySettingsComponent!!.gitlabUrl
        state.projectId = mySettingsComponent!!.projectId
        GitLabCredentialsManager.saveToken(mySettingsComponent!!.token)
    }

    override fun reset() {
        val state = GitLabSettingsState.getInstance().state
        mySettingsComponent!!.gitlabUrl = state.gitlabUrl
        mySettingsComponent!!.projectId = state.projectId
        mySettingsComponent!!.token = GitLabCredentialsManager.getToken() ?: ""
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
