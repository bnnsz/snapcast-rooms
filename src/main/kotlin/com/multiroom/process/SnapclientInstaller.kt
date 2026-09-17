package com.multiroom.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

/** A downloadable snapclient build. */
data class SnapclientRelease(val version: String, val downloadUrl: String, val sizeBytes: Long)

/** Progress of an install. */
sealed interface InstallState {
    data object Idle : InstallState
    data object Checking : InstallState
    data class Available(val release: SnapclientRelease) : InstallState
    data class Downloading(val fraction: Float) : InstallState
    data object Extracting : InstallState
    data class Installed(val path: Path, val version: String) : InstallState
    data class Failed(val message: String) : InstallState
}

/** Fetches snapclient from the official Snapcast releases. */
object SnapclientInstaller {

    private const val RELEASES = "https://api.github.com/repos/snapcast/snapcast/releases/latest"
    private const val ASSET = "snapclient_win64.zip"

    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private const val BUFFER = 1 shl 16
    private const val PROGRESS_STEP = 0.01f
    private const val VC_REDIST = "vc_redist.x64.exe"

    /**
     * Looks up the newest published Windows client.
     *
     * @throws IllegalStateException when the release carries no Windows asset
     */
    suspend fun latest(): SnapclientRelease {
        return withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder(URI.create(RELEASES))
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build()

            val body = http.send(request, HttpResponse.BodyHandlers.ofString()).body()
            val release = json.parseToJsonElement(body).jsonObject
            val version = release["tag_name"]?.jsonPrimitive?.content.orEmpty()

            val assets = release["assets"]?.jsonArray.orEmpty()
            val asset = assets.map { it.jsonObject }
                .firstOrNull { it["name"]?.jsonPrimitive?.content == ASSET }
            checkNotNull(asset) { "No $ASSET published in release $version" }

            SnapclientRelease(
                version = version,
                downloadUrl = asset["browser_download_url"]!!.jsonPrimitive.content,
                sizeBytes = asset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            )
        }
    }

    /**
     * Downloads [release] and extracts the whole archive into [targetDir].
     *
     * @param onProgress receives 0f..1f while downloading
     * @return the extracted snapclient.exe
     */
    suspend fun install(
        release: SnapclientRelease,
        targetDir: Path,
        onProgress: (Float) -> Unit = {},
    ): Path = withContext(Dispatchers.IO) {
        targetDir.createDirectories()
        val archive = targetDir.resolve("snapclient-download.zip")

        try {
            download(release, archive, onProgress)
            extract(archive, targetDir)
            targetDir.resolve("snapclient.exe")
        } finally {
            // Runs on cancellation too.
            archive.deleteIfExists()
        }
    }

    private suspend fun download(
        release: SnapclientRelease,
        into: Path,
        onProgress: (Float) -> Unit,
    ) {
        val request = HttpRequest.newBuilder(URI.create(release.downloadUrl)).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        check(response.statusCode() == 200) { "Download failed: HTTP ${response.statusCode()}" }

        val total = response.headers().firstValueAsLong("content-length")
            .orElse(release.sizeBytes)

        response.body().use { source ->
            Files.newOutputStream(into).buffered(BUFFER).use { sink ->
                val buffer = ByteArray(BUFFER)
                var written = 0L
                var lastReported = 0f
                while (true) {
                    // A blocking read never reaches a cancellation point.
                    currentCoroutineContext().ensureActive()

                    val read = source.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    written += read

                    if (total > 0) {
                        val fraction = (written.toFloat() / total).coerceIn(0f, 1f)
                        if (fraction - lastReported >= PROGRESS_STEP || fraction == 1f) {
                            lastReported = fraction
                            onProgress(fraction)
                        }
                    }
                }
            }
        }
    }

    private fun extract(archive: Path, targetDir: Path) {
        Files.newInputStream(archive).use { raw ->
            ZipInputStream(raw).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue

                    // An entry must not escape the target directory.
                    val name = Path.of(entry.name).fileName?.toString() ?: continue
                    if (name.isBlank()) continue

                    // A prerequisite, not part of the client.
                    if (name.equals(VC_REDIST, ignoreCase = true)) continue

                    val destination = targetDir.resolve(name).normalize()
                    check(destination.startsWith(targetDir)) { "Unsafe entry: ${entry.name}" }

                    Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
