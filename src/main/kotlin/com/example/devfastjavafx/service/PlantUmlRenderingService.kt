package com.example.devfastjavafx.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
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

    private val cacheLimit = 20
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, ImageBitmap>(cacheLimit, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > cacheLimit
        }
    })

    suspend fun render(content: String): ImageBitmap? = withContext(Dispatchers.Default) {
        val hash = hashString(content)
        cache[hash]?.let { return@withContext it }

        try {
            // Configure PlantUML to use Smetana and set size limits
            System.setProperty("PLANTUML_LIMIT_SIZE", "8192")

            // To ensure Smetana is used, we explicitly prepend !pragma useSmetana
            val plantUmlContent = if (content.contains("@startuml")) {
                if (content.contains("!pragma useSmetana")) {
                    content
                } else {
                    content.replaceFirst("@startuml", "@startuml\n!pragma useSmetana")
                }
            } else {
                "@startuml\n!pragma useSmetana\n$content\n@enduml"
            }

            val reader = SourceStringReader(plantUmlContent)
            val os = ByteArrayOutputStream()

            val result = reader.outputImage(os, FileFormatOption(FileFormat.PNG))
            if (result == null || result.description == "(Error)") {
                return@withContext null
            }

            val bytes = os.toByteArray()
            if (bytes.isEmpty()) return@withContext null

            val skiaImage = Image.makeFromEncoded(bytes)
            val bitmap = Bitmap.makeFromImage(skiaImage)
            val imageBitmap = bitmap.asComposeImageBitmap()

            cache[hash] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun getInstance(): PlantUmlRenderingService = service()
    }
}
