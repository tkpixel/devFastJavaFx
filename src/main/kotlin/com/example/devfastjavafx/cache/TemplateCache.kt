package com.example.devfastjavafx.cache

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TemplateCache {
    private const val CACHE_DIR_NAME = "devFastJavaFx/templates"

    private fun getCacheDir(): Path {
        val systemPath = PathManager.getSystemPath()
        val cachePath = Paths.get(systemPath, CACHE_DIR_NAME)
        if (!Files.exists(cachePath)) {
            Files.createDirectories(cachePath)
        }
        return cachePath
    }

    fun saveTemplate(path: String, content: String) {
        val cacheDir = getCacheDir()
        val templateFile = cacheDir.resolve(path)
        Files.createDirectories(templateFile.parent)
        Files.write(templateFile, content.toByteArray(Charsets.UTF_8))
    }

    fun loadTemplate(path: String): String? {
        val cacheDir = getCacheDir()
        val templateFile = cacheDir.resolve(path)
        return if (Files.exists(templateFile)) {
            Files.readString(templateFile, Charsets.UTF_8)
        } else {
            null
        }
    }

    fun listCachedTemplates(): List<String> {
        val cacheDir = getCacheDir()
        if (!Files.exists(cacheDir)) return emptyList()

        return Files.walk(cacheDir).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .map { cacheDir.relativize(it).toString() }
                .toList()
        }
    }

    fun clearCache() {
        val cacheDir = getCacheDir()
        if (Files.exists(cacheDir)) {
            Files.walk(cacheDir).use { stream ->
                stream.sorted(Comparator.reverseOrder())
                    .map { it.toFile() }
                    .forEach { it.delete() }
            }
        }
    }
}
