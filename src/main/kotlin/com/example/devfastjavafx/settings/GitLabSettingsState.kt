package com.example.devfastjavafx.settings

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "com.example.devfastjavafx.settings.GitLabSettingsState",
    storages = [Storage("devFastGitLabSettings.xml")]
)
class GitLabSettingsState : PersistentStateComponent<GitLabSettingsState.State> {

    class State {
        var gitlabUrl: String = "https://gitlab.com"
        var projectId: String = ""
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): GitLabSettingsState = service()
    }
}
