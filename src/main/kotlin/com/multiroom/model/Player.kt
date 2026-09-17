package com.multiroom.model

import kotlinx.serialization.Serializable

/**
 * A snapclient instance this machine runs.
 *
 * @property id stable key for this player's configuration
 * @property name the player name; used as snapclient's --hostID and as the
 *   Snapserver client id
 * @property deviceId Windows audio endpoint id, e.g. "{0.0.0.00000000}.{guid}",
 *   or "default" to follow the Windows default device
 * @property instance snapclient --instance number; unique on this machine
 * @property sampleFormat resample target such as "48000:16:*", or null to take
 *   the rate the server streams at
 * @property autoStart whether to launch this player when the app starts
 */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val deviceId: String = "default",
    val instance: Int = 1,
    val sampleFormat: String? = null,
    val autoStart: Boolean = true,
) {
    /** The id Snapserver knows this player by: the name, then "#<instance>" past the first. */
    val clientId: String get() = if (instance > 1) "$name#$instance" else name
}

/** Runtime state of a player's snapclient process. */
enum class PlayerState { STOPPED, STARTING, RUNNING, CRASHED }

/** A Snapserver client as reported over the control API. */
data class ServerClient(
    val id: String,
    val displayName: String,
    /** Name set on the server, empty when it has never been given one. */
    val configuredName: String,
    /** Machine the client runs on. */
    val hostName: String,
    val connected: Boolean,
    val volumePercent: Int,
    val muted: Boolean,
    val groupId: String,
)

/** A Snapserver group; its clients play one stream in sync. */
data class ServerGroup(
    val id: String,
    val name: String,
    val streamId: String,
    val muted: Boolean,
    val clients: List<ServerClient>,
)

/**
 * What a stream is currently playing, as the source reports it.
 *
 * @property artist joined for display; the server sends a list
 * @property artUrl absolute URL on the source, or null when it sends no art
 * @property positionSeconds playback position when the server last reported it
 * @property reportedAtMs when [positionSeconds] was reported, epoch milliseconds
 */
data class TrackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val artUrl: String?,
    val durationSeconds: Double,
    val positionSeconds: Double,
    val reportedAtMs: Long = System.currentTimeMillis(),
)

/**
 * A Snapserver stream. A group plays exactly one.
 *
 * @property playing whether the server reports audio flowing
 */
data class ServerStream(
    val id: String,
    val status: String,
    val track: TrackInfo?,
) {
    val playing: Boolean get() = status.equals("playing", ignoreCase = true)

    /** Source name without the plugin prefix, e.g. "maOfficePCx". */
    val shortName: String get() = id.substringAfter(" - ", id)
}
