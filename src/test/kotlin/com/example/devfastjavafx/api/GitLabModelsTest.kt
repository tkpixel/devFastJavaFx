package com.example.devfastjavafx.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GitLabModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDeserializeTreeItem() {
        val jsonString = """
            {
                "id": "123",
                "name": "template.java",
                "type": "blob",
                "path": "templates/template.java",
                "mode": "100644"
            }
        """.trimIndent()

        val item = json.decodeFromString<GitLabTreeItem>(jsonString)
        assertEquals("123", item.id)
        assertEquals("template.java", item.name)
        assertEquals("blob", item.type)
    }

    @Test
    fun testDeserializeFile() {
        val jsonString = """
            {
                "file_name": "template.java",
                "file_path": "templates/template.java",
                "size": 100,
                "encoding": "base64",
                "content": "SGVsbG8gV29ybGQ=",
                "content_sha256": "sha256",
                "ref": "main",
                "blob_id": "blob123",
                "commit_id": "commit123",
                "last_commit_id": "last123"
            }
        """.trimIndent()

        val file = json.decodeFromString<GitLabFile>(jsonString)
        assertEquals("template.java", file.file_name)
        assertEquals("base64", file.encoding)
        assertEquals("SGVsbG8gV29ybGQ=", file.content)
    }
}
