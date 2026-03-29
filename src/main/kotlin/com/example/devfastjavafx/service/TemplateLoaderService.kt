package com.example.devfastjavafx.service

import com.example.devfastjavafx.api.GitLabClient
import com.example.devfastjavafx.cache.TemplateCache
import com.example.devfastjavafx.credentials.GitLabCredentialsManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@Service(Service.Level.APP)
class TemplateLoaderService(private val scope: CoroutineScope) {
    private val LOG = logger<TemplateLoaderService>()

    // These should ideally come from configuration
    private val GITLAB_BASE_URL = "https://gitlab.example.com"
    private val PROJECT_ID = "12345"
    private val TEMPLATE_PATH = "templates"

    fun loadTemplates() {
        scope.launch(Dispatchers.IO) {
            try {
                val token = GitLabCredentialsManager.getToken()
                if (token == null) {
                    LOG.warn("GitLab token not found. Skipping template loading.")
                    return@launch
                }

                val client = GitLabClient(GITLAB_BASE_URL, token)
                try {
                    val tree = client.getRepositoryTree(PROJECT_ID, TEMPLATE_PATH)
                    for (item in tree) {
                        if (item.type == "blob") {
                            val file = client.getFileContent(PROJECT_ID, item.path)
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
            }
        }
    }
}
