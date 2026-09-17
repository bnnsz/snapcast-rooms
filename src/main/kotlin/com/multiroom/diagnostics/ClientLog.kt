package com.multiroom.diagnostics

/** A meaningful line from snapclient's output. */
sealed interface LogEvent {
    /** Periodic statistics. @property bufferMs filled buffer, @property syncMs offset to server */
    data class Stats(val syncMs: Int, val bufferMs: Int) : LogEvent

    /** The player had no chunk to play. */
    data object Starved : LogEvent

    /** The device asked for more frames than were available. */
    data class Underrun(val requested: Int, val available: Int) : LogEvent

    /** Output was padded with silence. */
    data object SilencePadded : LogEvent

    /** Chunks arrived too old to play. @property ageMs how far behind */
    data class LaggingBehind(val ageMs: Int) : LogEvent

    /** The client corrected for drift. */
    data object Resync : LogEvent

    /** --soundcard did not resolve; the client will exit. */
    data class DeviceBindFailed(val requested: String) : LogEvent

    /** Negotiated formats, as "rate:bits:channels". */
    data class Format(val playerFormat: String, val streamFormat: String) : LogEvent

    data class Connected(val host: String) : LogEvent
    data object Reconnecting : LogEvent
    data class Other(val line: String) : LogEvent
}

/** Turns snapclient stdout into [LogEvent]s. */
object ClientLog {

    private val stats = Regex("""Stats\) Chunk:\s*(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(\d+)""")
    private val underrun = Regex("""requested frames: (\d+), available: (\d+)""")
    private val lagging = Regex("""age > 0: (\d+)ms""")
    private val player = Regex("""Player\) Sampleformat: (\S+), stream: (\S+)""")
    private val bindFail = Regex("""device: (.+?), description: <none>, idx: -1""")
    private val connected = Regex("""Connected to (\S+)""")

    fun parse(line: String): LogEvent = when {
        "Stats) Chunk:" in line ->
            stats.find(line)?.let { LogEvent.Stats(it.groupValues[1].toInt(), it.groupValues[5].toInt()) }
                ?: LogEvent.Other(line)

        "Failed to get chunk" in line || "Waiting for chunk" in line -> LogEvent.Starved

        "Not enough frames available" in line ->
            underrun.find(line)?.let { LogEvent.Underrun(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
                ?: LogEvent.Other(line)

        "Silent frames" in line -> LogEvent.SilencePadded

        "dropping old chunks" in line ->
            lagging.find(line)?.let { LogEvent.LaggingBehind(it.groupValues[1].toInt()) }
                ?: LogEvent.Other(line)

        "pBuffer->full()" in line || "pMiniBuffer->full()" in line ||
            "pShortBuffer->full()" in line -> LogEvent.Resync

        "idx: -1" in line ->
            LogEvent.DeviceBindFailed(bindFail.find(line)?.groupValues?.get(1).orEmpty())

        "Player) Sampleformat:" in line ->
            player.find(line)?.let { LogEvent.Format(it.groupValues[1], it.groupValues[2]) }
                ?: LogEvent.Other(line)

        "Connected to" in line ->
            LogEvent.Connected(connected.find(line)?.groupValues?.get(1).orEmpty())

        "Reconnecting" in line -> LogEvent.Reconnecting

        else -> LogEvent.Other(line)
    }
}
