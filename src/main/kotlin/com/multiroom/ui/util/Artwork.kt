package com.multiroom.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches and caches album art published by the audio source.
 *
 * A fetch that fails returns null rather than throwing; the source host is on
 * the local network and may be unreachable.
 */
object Artwork {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * @param url absolute http(s) URL of the image
     * @returns the decoded image, or null when it cannot be fetched or decoded
     */
    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build()
                val bytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                    .takeIf { it.statusCode() == 200 }
                    ?.body()
                    ?: return@runCatching null
                Image.makeFromEncoded(bytes).toComposeImageBitmap().also { cache[url] = it }
            }.getOrNull()
        }
    }
}
