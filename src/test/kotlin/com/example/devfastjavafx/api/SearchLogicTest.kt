package com.example.devfastjavafx.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchLogicTest {

    private val components = listOf(
        ComponentMetadata(name = "Button", tags = listOf("ui", "basic"), id = "btn"),
        ComponentMetadata(name = "Table", tags = listOf("ui", "data"), id = "tbl"),
        ComponentMetadata(name = "Network Client", tags = listOf("network", "api"), id = "net")
    )

    @Test
    fun `test search by name`() {
        val query = "Button"
        val filtered = components.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("btn", filtered[0].id)
    }

    @Test
    fun `test search by tag`() {
        val query = "network"
        val filtered = components.filter { component ->
            component.name.contains(query, ignoreCase = true) ||
                    component.tags.any { it.contains(query, ignoreCase = true) }
        }
        assertEquals(1, filtered.size)
        assertEquals("net", filtered[0].id)
    }

    @Test
    fun `test fuzzy search case insensitive`() {
        val query = "UI"
        val filtered = components.filter { component ->
            component.name.contains(query, ignoreCase = true) ||
                    component.tags.any { it.contains(query, ignoreCase = true) }
        }
        assertEquals(2, filtered.size)
    }
}
