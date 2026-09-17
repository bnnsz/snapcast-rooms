package com.multiroom.process

import java.util.concurrent.TimeUnit

/**
 * A snapclient running on this machine that the app did not start.
 *
 * @property instance the --instance it was launched with; 1 when not given
 */
data class StrayClient(
    val pid: Long,
    val hostId: String?,
    val instance: Int,
    val command: String,
) {
    /** The id Snapserver knows this client by. */
    val clientId: String?
        get() = hostId?.let { if (instance > 1) "$it#$instance" else it }
}

/**
 * Finds snapclient processes the app does not own.
 *
 * Command lines are read through WMI: on Windows ProcessHandle reports only the
 * executable path, never the arguments, for a process it did not start.
 */
object ProcessScanner {

    private const val EXE = "snapclient.exe"
    private const val QUERY_TIMEOUT_SECONDS = 15L

    // Quote-free: ProcessBuilder re-quotes arguments and mangles a double-quoted
    // -Command string.
    private val COMMAND = listOf(
        "powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
        "Get-CimInstance Win32_Process | " +
            "Where-Object { \$_.Name -eq '$EXE' } | " +
            "ForEach-Object { [string]\$_.ProcessId + '|' + [string]\$_.CommandLine }",
    )

    /**
     * @param ownedPids processes the app launched, which are not strays
     * @return every other snapclient on this machine, empty when the query fails
     */
    fun findSnapclients(ownedPids: Set<Long> = emptySet()): List<StrayClient> =
        queryCommandLines()
            .filterKeys { it !in ownedPids }
            .map { (pid, commandLine) ->
                val args = tokenize(commandLine)
                StrayClient(
                    pid = pid,
                    hostId = args.valueOf("--hostID"),
                    instance = args.valueOf("--instance")?.toIntOrNull() ?: 1,
                    command = commandLine,
                )
            }

    /**
     * Stops any stray serving [hostId] at [instance].
     *
     * @return how many were stopped
     */
    fun stopStrays(hostId: String, instance: Int = 1, ownedPids: Set<Long> = emptySet()): Int =
        findSnapclients(ownedPids)
            .filter { it.hostId == hostId && it.instance == instance }
            .count { stray ->
                ProcessHandle.of(stray.pid)
                    .map { handle -> handle.destroy() }
                    .orElse(false)
            }

    private fun queryCommandLines(): Map<Long, String> = runCatching {
        val process = ProcessBuilder(COMMAND).redirectErrorStream(false).start()
        val lines = process.inputStream.bufferedReader().readLines()
        if (!process.waitFor(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
        lines.mapNotNull { line ->
            val separator = line.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            val pid = line.take(separator).trim().toLongOrNull() ?: return@mapNotNull null
            pid to line.substring(separator + 1).trim()
        }.toMap()
    }.getOrDefault(emptyMap())

    /** Splits a Windows command line, treating a double-quoted run as one token. */
    private fun tokenize(commandLine: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        for (character in commandLine) {
            when {
                character == '"' -> quoted = !quoted
                character.isWhitespace() && !quoted -> {
                    if (current.isNotEmpty()) {
                        tokens += current.toString()
                        current.clear()
                    }
                }
                else -> current.append(character)
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        return tokens
    }

    private fun List<String>.valueOf(flag: String): String? {
        val index = indexOf(flag)
        return if (index >= 0 && index + 1 < size) this[index + 1] else null
    }
}
