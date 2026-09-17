package com.multiroom.audio

import java.nio.file.Path

/**
 * A Windows audio endpoint.
 *
 * @property id endpoint id to pass to snapclient --soundcard
 * @property description friendly name shown to the user
 */
data class AudioDevice(val id: String, val description: String)

/**
 * Enumerates output devices by running snapclient --list, which prints pairs of
 * lines:
 *   "<index>: <endpoint id>"
 *   "<description>"
 *
 * Only the id is stable; indices shift when audio hardware is plugged in.
 */
object AudioDevices {

    private val HEADER = Regex("""^(\d+):\s*(.+)$""")

    fun list(snapclientExe: Path): List<AudioDevice> {
        val output = ProcessBuilder(snapclientExe.toString(), "--list")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readLines()

        val devices = mutableListOf<AudioDevice>()
        var pendingId: String? = null
        for (raw in output) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val header = HEADER.find(line)
            if (header != null) {
                pendingId = header.groupValues[2].trim()
            } else if (pendingId != null) {
                devices += AudioDevice(pendingId, line)
                pendingId = null
            }
        }
        return devices
    }
}
