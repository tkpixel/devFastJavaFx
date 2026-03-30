package com.example.devfastjavafx.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GitLabClient(
    private val baseUrl: String,
    private val privateToken: String?
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getRepositoryTree(projectId: String, path: String = "", recursive: Boolean = true): List<GitLabTreeItem> {
        return client.get("$baseUrl/api/v4/projects/$projectId/repository/tree") {
            parameter("path", path)
            parameter("recursive", recursive)
            privateToken?.let { header("PRIVATE-TOKEN", it) }
        }.body()
    }

    suspend fun getFileContent(projectId: String, filePath: String, ref: String = "main"): GitLabFile {
        val encodedFilePath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
        return client.get("$baseUrl/api/v4/projects/$projectId/repository/files/$encodedFilePath") {
            parameter("ref", ref)
            privateToken?.let { header("PRIVATE-TOKEN", it) }
        }.body()
    }

    fun close() {
        client.close()
    }
}
