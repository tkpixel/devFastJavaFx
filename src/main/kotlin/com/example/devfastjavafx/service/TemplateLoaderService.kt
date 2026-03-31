package com.example.devfastjavafx.service

import com.example.devfastjavafx.api.GitLabClient
import com.example.devfastjavafx.cache.TemplateCache
import com.example.devfastjavafx.credentials.GitLabCredentialsManager
import com.example.devfastjavafx.settings.GitLabSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Service(Service.Level.APP)
class TemplateLoaderService(private val scope: CoroutineScope) {
    private val LOG = logger<TemplateLoaderService>()
    private val TEMPLATE_PATH = "templates"

    fun loadTemplates(onComplete: (() -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val settings = GitLabSettingsState.getInstance().state
                val baseUrl = settings.gitlabUrl
                val projectId = settings.projectId
                val token = GitLabCredentialsManager.getToken()

                if (baseUrl.isEmpty() || projectId.isEmpty() || token.isNullOrEmpty()) {
                    LOG.warn("GitLab settings or token not configured. Skipping template loading.")
                    return@launch
                }

                val client = GitLabClient(baseUrl, token)
                try {
                    val tree = client.getRepositoryTree(projectId, TEMPLATE_PATH)
                    for (item in tree) {
                        if (item.type == "blob") {
                            val file = client.getFileContent(projectId, item.path)
                            val content = if (file.encoding == "base64") {
                                String(Base64.getMimeDecoder().decode(file.content))
                            } else {
                                file.content
                            }
                            TemplateCache.saveTemplate(item.path, content)
                        }
                    }
                    LOG.info("Successfully loaded templates from GitLab.")
                } finally {
                    client.close()
                }
            } catch (e: Exception) {
                LOG.error("Failed to load templates from GitLab", e)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    companion object {
        fun getInstance(): TemplateLoaderService = service()
    }
}
