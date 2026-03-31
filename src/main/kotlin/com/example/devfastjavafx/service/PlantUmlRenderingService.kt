package com.example.devfastjavafx.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap

@Service(Service.Level.APP)
class PlantUmlRenderingService {

    private val LOG = logger<PlantUmlRenderingService>()
    private val renderMutex = Mutex()

    private val cacheLimit = 20
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, ImageBitmap>(cacheLimit, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > cacheLimit
        }
    })

    suspend fun render(content: String): ImageBitmap? = withContext(Dispatchers.Default) {
        val hash = hashString(content)
        cache[hash]?.let { return@withContext it }

        renderMutex.withLock {
            // Double-check after acquiring lock (another coroutine may have rendered it already)
            cache[hash]?.let { return@withLock it }

            try {
                System.setProperty("PLANTUML_LIMIT_SIZE", "8192")

                val reader = SourceStringReader(fixComponentNotes(content))
                val os = ByteArrayOutputStream()
                val result = reader.outputImage(os, FileFormatOption(FileFormat.PNG))

                LOG.debug("PlantUML result description: ${result?.description}")

                val bytes = os.toByteArray()
                // Check bytes first – PlantUML may return "(Error)" in description even when the
                // image renders successfully (e.g. component names containing dots like "LoginView.java").
                if (bytes.isEmpty()) {
                    LOG.warn("PlantUML render produced empty output. Description: ${result?.description}")
                    return@withLock null
                }

                val skiaImage = Image.makeFromEncoded(bytes)
                val bitmap = Bitmap.makeFromImage(skiaImage)
                val imageBitmap = bitmap.asComposeImageBitmap()

                cache[hash] = imageBitmap
                imageBitmap
            } catch (e: Exception) {
                LOG.error("PlantUML rendering exception", e)
                null
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Rewrites "note right/left/top/bottom of <alias>" blocks to the standalone
     * "note as N\n...\nend note\nN .. <alias>" syntax, which avoids the PlantUML
     * "element already defined" bug in Component Diagrams.
     */
    private fun fixComponentNotes(content: String): String {
        val notePattern = Regex(
            """note\s+(right|left|top|bottom)\s+of\s+(\w+)\s*\n(.*?)\nend note""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        var counter = 0
        return notePattern.replace(content) { match ->
            val alias = match.groupValues[2]
            val body = match.groupValues[3]
            val noteName = "N_${alias}_${counter++}"
            "note as $noteName\n$body\nend note\n$noteName .. $alias"
        }
    }

    companion object {
        fun getInstance(): PlantUmlRenderingService = service()
    }
}
