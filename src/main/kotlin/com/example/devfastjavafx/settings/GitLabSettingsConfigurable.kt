package com.example.devfastjavafx.settings

import com.example.devfastjavafx.credentials.GitLabCredentialsManager
import com.intellij.openapi.options.Configurable
import com.intellij.util.SlowOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class GitLabSettingsConfigurable : Configurable {
    private var mySettingsComponent: GitLabSettingsComponent? = null
    private var scope: CoroutineScope? = null
    private var originalToken: String = ""

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "DevFast GitLab Settings"
    }

    override fun createComponent(): JComponent? {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        mySettingsComponent = GitLabSettingsComponent(scope!!)
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val state = GitLabSettingsState.getInstance().state
        return mySettingsComponent!!.gitlabUrl != state.gitlabUrl ||
                mySettingsComponent!!.projectId != state.projectId ||
                mySettingsComponent!!.token != originalToken
    }

    override fun apply() {
        val state = GitLabSettingsState.getInstance().state
        state.gitlabUrl = mySettingsComponent!!.gitlabUrl
        state.projectId = mySettingsComponent!!.projectId
        val newToken = mySettingsComponent!!.token
        SlowOperations.allowSlowOperations("devFastJavaFx.saveToken").use {
            GitLabCredentialsManager.saveToken(newToken)
        }
        originalToken = newToken
    }

    override fun reset() {
        val state = GitLabSettingsState.getInstance().state
        mySettingsComponent!!.gitlabUrl = state.gitlabUrl
        mySettingsComponent!!.projectId = state.projectId
        val token = SlowOperations.allowSlowOperations("devFastJavaFx.getToken").use {
            GitLabCredentialsManager.getToken()
        } ?: ""
        originalToken = token
        mySettingsComponent!!.token = token
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
        scope?.cancel()
        scope = null
    }
}
