package com.multiroom.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/** What the app knows about its snapclient. */
data class SnapclientInfo(val path: Path, val version: String?)

/**
 * Owns the app's copy of snapclient.
 *
 * Downloading and browsing both copy into [directory]; no path outside it is
 * ever referenced.
 */
object SnapclientStore {

    private const val EXE = "snapclient.exe"

    /** Files snapclient links against. */
    private val RUNTIME_FILES = setOf(
        "FLAC.dll", "ogg.dll", "opus.dll", "vorbis.dll", "soxr.dll",
        "libcrypto-3-x64.dll", "libssl-3-x64.dll",
    )

    /** Working directory holding the executable and its runtime files. */
    val directory: Path = Path.of(
        System.getenv("APPDATA") ?: System.getProperty("user.home"),
        "SnapcastRooms",
        "snapclient",
    )

    val executable: Path get() = directory.resolve(EXE)

    fun isInstalled(): Boolean = executable.isRegularFile()

    /**
     * Reads the version from the program itself.
     *
     * @return e.g. "0.35.0", or null when it cannot be run
     */
    suspend fun version(): String? = withContext(Dispatchers.IO) {
        if (!isInstalled()) return@withContext null
        runCatching {
            val process = ProcessBuilder(executable.toString(), "--version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            Regex("""v?(\d+\.\d+\.\d+)""").find(output)?.groupValues?.get(1)
        }.getOrNull()
    }

    /**
     * Copies a chosen build into the working directory, with the runtime files
     * that sit beside it.
     *
     * @param source the selected snapclient.exe
     * @return the copy inside the working directory
     */
    suspend fun installFrom(source: Path): Path = withContext(Dispatchers.IO) {
        require(source.isRegularFile()) { "$source is not a file" }
        directory.createDirectories()

        Files.copy(source, executable, StandardCopyOption.REPLACE_EXISTING)

        source.parent?.let { sourceDir ->
            Files.list(sourceDir).use { entries ->
                entries.filter { it.isRegularFile() && it.name in RUNTIME_FILES }
                    .forEach {
                        Files.copy(
                            it,
                            directory.resolve(it.name),
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
            }
        }

        executable
    }

    /** Downloads [release] straight into the working directory. */
    suspend fun installFrom(
        release: SnapclientRelease,
        onProgress: (Float) -> Unit,
    ): Path = SnapclientInstaller.install(release, directory, onProgress)
}
