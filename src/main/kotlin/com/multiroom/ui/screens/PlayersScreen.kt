package com.multiroom.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiroom.audio.AudioDevice
import com.multiroom.data.AppSettings
import com.multiroom.diagnostics.Health
import com.multiroom.diagnostics.PlayerHealth
import com.multiroom.model.Player
import com.multiroom.model.PlayerState
import com.multiroom.model.ServerClient
import com.multiroom.model.ServerGroup
import com.multiroom.model.ServerStream
import com.multiroom.model.TrackInfo
import com.multiroom.ui.components.CardGroupHeader
import com.multiroom.ui.components.FluentIconButton
import com.multiroom.ui.components.FluentSlider
import com.multiroom.ui.components.InfoBar
import com.multiroom.ui.theme.CardCornerRadius
import com.multiroom.ui.theme.CardGap
import com.multiroom.ui.theme.CardGroupGap
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.NumericValueTextStyle
import com.multiroom.ui.theme.SettingsCardPadding
import com.multiroom.ui.util.Artwork
import kotlinx.coroutines.delay
import java.net.URI
import kotlin.math.roundToInt

private val ArtSize = 56.dp

/** Fixed, as in the Windows volume mixer. */
private val SliderWidth = 110.dp

/** Thin enough to read as part of the card edge. */
private val ProgressHeight = 2.dp

/** How often the local position advances between the reports. */
private const val TickMs = 1_000L

@Composable
fun PlayersScreen(
    config: AppSettings,
    devices: List<AudioDevice>,
    states: Map<String, PlayerState>,
    health: Map<String, PlayerHealth>,
    serverClients: List<ServerClient>,
    serverGroups: List<ServerGroup>,
    streams: List<ServerStream>,
    onToggle: (Player) -> Unit,
    onVolumeChange: (String, Int) -> Unit,
) {
    if (config.players.isEmpty()) {
        InfoBar(
            title = "No players yet",
            message = "Players already on the server appear here automatically. " +
                "Add one in Settings to start a new speaker on this PC.",
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(CardGroupGap)) {
        Column {
            CardGroupHeader("This PC")
            Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
                config.players.forEach { player ->
                    val client = serverClients.firstOrNull { it.id == player.clientId }
                    val group = serverGroups.firstOrNull { it.id == client?.groupId }
                    PlayerCard(
                        player = player,
                        deviceName = deviceName(player.deviceId, devices),
                        state = states[player.id] ?: PlayerState.STOPPED,
                        health = health[player.id] ?: PlayerHealth(),
                        client = client,
                        stream = streams.firstOrNull { it.id == group?.streamId },
                        artHost = config.serverHost,
                        onToggle = { onToggle(player) },
                        onVolumeChange = onVolumeChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    deviceName: String,
    state: PlayerState,
    health: PlayerHealth,
    client: ServerClient?,
    stream: ServerStream?,
    artHost: String,
    onToggle: () -> Unit,
    onVolumeChange: (String, Int) -> Unit,
) {
    val fluent = LocalFluent.current
    val running = state == PlayerState.RUNNING
    val track = if (running) stream?.track else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardCornerRadius)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, fluent.cardStroke, CardCornerRadius),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SettingsCardPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(track, artHost, running)

            Spacer(Modifier.width(SettingsCardPadding))

            Column(Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (track != null) nowPlaying(track) else status(state, health),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (track != null) fluent.textSecondary
                    else statusColor(health, fluent.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = fluent.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (client != null) {
                Spacer(Modifier.width(SettingsCardPadding))

                Icon(
                    imageVector = if (client.muted) Icons.Outlined.VolumeMute
                    else Icons.Outlined.VolumeUp,
                    contentDescription = "Volume",
                    tint = fluent.iconTint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = client.volumePercent.toString(),
                    style = NumericValueTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                FluentSlider(
                    value = client.volumePercent / 100f,
                    onValueChange = { onVolumeChange(client.id, (it * 100).roundToInt()) },
                    modifier = Modifier.width(SliderWidth),
                )
            }

            Spacer(Modifier.width(8.dp))

            FluentIconButton(
                icon = if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                description = if (running) "Stop this player" else "Start this player",
                accent = !running,
                onClick = onToggle,
            )
        }

        AnimatedVisibility(
            visible = track != null,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
        ) {
            TrackProgress(track, playing = stream?.playing == true)
        }
    }
}

@Composable
private fun AlbumArt(track: TrackInfo?, artHost: String, running: Boolean) {
    val fluent = LocalFluent.current
    val artUrl = remember(track?.artUrl, artHost) { rehost(track?.artUrl, artHost) }
    var art by remember(artUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(artUrl) {
        art = artUrl?.let { Artwork.load(it) }
    }

    Box(
        modifier = Modifier
            .size(ArtSize)
            .clip(RoundedCornerShape(4.dp))
            .background(fluent.controlFill),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = art
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = track?.album,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = if (running) Icons.Outlined.Speaker else Icons.Outlined.Headphones,
                contentDescription = null,
                tint = fluent.iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Draws how far the track has run, along the bottom edge of the card.
 *
 * The position advances locally between the reports the server sends.
 *
 * @param playing whether to advance; a stopped track holds its position
 */
@Composable
private fun TrackProgress(track: TrackInfo?, playing: Boolean) {
    if (track == null || track.durationSeconds <= 0.0) return

    var elapsed by remember(track.reportedAtMs) { mutableStateOf(track.positionSeconds) }

    LaunchedEffect(track.reportedAtMs, playing) {
        elapsed = track.positionSeconds
        while (playing) {
            delay(TickMs)
            val advanced = (System.currentTimeMillis() - track.reportedAtMs) / 1000.0
            elapsed = (track.positionSeconds + advanced).coerceAtMost(track.durationSeconds)
        }
    }

    val progress = (elapsed / track.durationSeconds).coerceIn(0.0, 1.0).toFloat()
    val width by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(TickMs.toInt()),
        label = "trackProgress",
    )

    // Inset by the border width so the card outline stays unbroken.
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 0.dp)
            .padding(bottom = 1.dp)
            .height(ProgressHeight)
    ) {
        Box(
            Modifier
                .fillMaxWidth(width)
                .height(ProgressHeight)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Points an art URL at [host], replacing the address the source advertised.
 *
 * @returns the rewritten URL, or null when there was none
 */
private fun rehost(url: String?, host: String): String? {
    if (url == null || host.isBlank()) return url
    return runCatching {
        val original = URI(url)
        URI(
            original.scheme,
            original.userInfo,
            host,
            original.port,
            original.path,
            original.query,
            original.fragment,
        ).toString()
    }.getOrDefault(url)
}

/**
 * @param devices endpoints reported by snapclient
 * @returns the friendly name, or the raw endpoint id when it is not listed
 */
private fun deviceName(deviceId: String, devices: List<AudioDevice>): String = when (deviceId) {
    "default" -> "Windows default device"
    else -> devices.firstOrNull { it.id == deviceId }?.description ?: deviceId
}

private fun status(state: PlayerState, health: PlayerHealth): String = when {
    health.diagnosis != null -> health.diagnosis
    state == PlayerState.RUNNING && health.bufferMs > 0 -> "Playing"
    state == PlayerState.RUNNING -> "Connected, waiting for audio"
    state == PlayerState.STARTING -> "Starting"
    state == PlayerState.CRASHED -> "Restarting after a failure"
    else -> "Stopped"
}

@Composable
private fun statusColor(health: PlayerHealth, default: Color): Color = when (health.health) {
    Health.BROKEN -> LocalFluent.current.critical
    Health.DEGRADED -> LocalFluent.current.caution
    else -> default
}

/** @returns "Title  ·  Artist", or just the title when the artist is unknown */
private fun nowPlaying(track: TrackInfo): String {
    val artist = track.artist.ifBlank { track.album }
    return if (artist.isBlank()) track.title else "${track.title}  ·  $artist"
}

