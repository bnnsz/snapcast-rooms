package com.multiroom.diagnostics

/** Overall verdict for a player, worst-first. */
enum class Health { UNKNOWN, OK, DEGRADED, BROKEN }

/**
 * Rolling health derived from a player's log stream.
 *
 * @property bufferMs last reported buffer fill
 * @property syncMs last reported offset to the server
 * @property clockDriftMsPerSec estimated clock rate error, or null until two
 *   lag samples have been seen
 * @property diagnosis a sentence naming the fault, or null when healthy
 */
data class PlayerHealth(
    val health: Health = Health.UNKNOWN,
    val bufferMs: Int = 0,
    val syncMs: Int = 0,
    val starvedCount: Int = 0,
    val underrunCount: Int = 0,
    val resyncCount: Int = 0,
    val clockDriftMsPerSec: Double? = null,
    val diagnosis: String? = null,
)

/**
 * Accumulates [LogEvent]s into a [PlayerHealth].
 *
 * Not thread safe; own one per player and feed it from that player's reader.
 */
class PlayerHealthTracker(private val startedAtMs: Long = System.currentTimeMillis()) {

    private var current = PlayerHealth()
    private var firstLag: Pair<Long, Int>? = null

    val state: PlayerHealth get() = current

    fun onEvent(event: LogEvent, nowMs: Long = System.currentTimeMillis()) {
        current = when (event) {
            is LogEvent.Stats -> current.copy(
                bufferMs = event.bufferMs,
                syncMs = event.syncMs,
                health = if (event.bufferMs > 0) Health.OK else current.health,
                diagnosis = if (event.bufferMs > 0) null else current.diagnosis,
            )

            LogEvent.Starved -> current.copy(starvedCount = current.starvedCount + 1)

            is LogEvent.Underrun -> current.copy(underrunCount = current.underrunCount + 1)

            LogEvent.SilencePadded -> current.copy(
                health = Health.DEGRADED,
                diagnosis = "Output is being padded with silence.",
            )

            is LogEvent.LaggingBehind -> trackDrift(event.ageMs, nowMs)

            LogEvent.Resync -> current.copy(resyncCount = current.resyncCount + 1)

            is LogEvent.DeviceBindFailed -> current.copy(
                health = Health.BROKEN,
                diagnosis = "Audio device '${event.requested}' did not resolve. " +
                    "--soundcard needs the endpoint id, not its description.",
            )

            is LogEvent.Format ->
                if (event.playerFormat.endsWith(":0")) current.copy(
                    health = Health.BROKEN,
                    diagnosis = "Channel count resolved to 0 (${event.playerFormat}). " +
                        "Remove --sampleformat: the server already streams " +
                        "${event.streamFormat}.",
                ) else current

            else -> current
        }
    }

    /** Estimates the clock rate error from two lag samples. */
    private fun trackDrift(ageMs: Int, nowMs: Long): PlayerHealth {
        val first = firstLag
        if (first == null) {
            firstLag = nowMs to ageMs
            return current.copy(health = Health.DEGRADED, diagnosis = "Falling behind the server.")
        }
        val elapsedSec = (nowMs - first.first) / 1000.0
        if (elapsedSec < 5) return current
        val rate = (ageMs - first.second) / elapsedSec
        return if (rate > 1.0) current.copy(
            health = Health.BROKEN,
            clockDriftMsPerSec = rate,
            diagnosis = "Clock rate error: falling behind %.1f ms/s. Audio will stop after about %d s. Check the server host's clocksource."
                .format(rate, (current.bufferMs.coerceAtLeast(1) / rate).toInt()),
        ) else current.copy(clockDriftMsPerSec = rate)
    }
}
