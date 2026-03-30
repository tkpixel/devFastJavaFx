package com.example.devfastjavafx.api

import kotlinx.serialization.Serializable

@Serializable
data class GitLabTreeItem(
    val id: String,
    val name: String,
    val type: String,
    val path: String,
    val mode: String
)

@Serializable
data class GitLabFile(
    val file_name: String,
    val file_path: String,
    val size: Int,
    val encoding: String,
    val content: String,
    val content_sha256: String,
    val ref: String,
    val blob_id: String,
    val commit_id: String,
    val last_commit_id: String
)

@Serializable
data class ComponentMetadata(
    val name: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val id: String = ""
)
