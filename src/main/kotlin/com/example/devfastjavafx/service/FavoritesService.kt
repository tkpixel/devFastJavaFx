package com.example.devfastjavafx.service

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "DevFastFavorites",
    storages = [Storage("devFastFavorites.xml")]
)
class FavoritesService : PersistentStateComponent<FavoritesService.State> {

    class State {
        var favoriteIds: MutableSet<String> = mutableSetOf()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun isFavorite(id: String): Boolean = myState.favoriteIds.contains(id)

    fun toggleFavorite(id: String) {
        if (myState.favoriteIds.contains(id)) {
            myState.favoriteIds.remove(id)
        } else {
            myState.favoriteIds.add(id)
        }
    }

    companion object {
        fun getInstance(): FavoritesService = service()
    }
}
