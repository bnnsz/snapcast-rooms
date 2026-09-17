package com.multiroom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.multiroom.data.AppSettings
import com.multiroom.diagnostics.Health
import com.multiroom.diagnostics.PlayerHealth
import com.multiroom.ui.components.CardGroupHeader
import com.multiroom.ui.components.InfoBar
import com.multiroom.ui.components.SettingsCard
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.MetricValueTextStyle
import com.multiroom.ui.theme.UnitTextStyle

/**
 * Per-player signal quality.
 *
 * Drift is the figure that matters: a lag growing steadily means the server and
 * client clocks run at different rates, and the player falls silent once the
 * accumulated error exceeds the buffer. Everything else is context for it.
 */
@Composable
fun DiagnosticsScreen(config: AppSettings, health: Map<String, PlayerHealth>) {
    val reporting = config.players.filter { health[it.id] != null }
    if (reporting.isEmpty()) {
        InfoBar(
            title = "Nothing to report",
            message = "Start a player to collect signal data.",
        )
        return
    }

    val fluent = LocalFluent.current

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        reporting.forEach { player ->
            val h = health.getValue(player.id)
            val statusColor = when (h.health) {
                Health.BROKEN -> fluent.critical
                Health.DEGRADED -> fluent.caution
                Health.OK -> fluent.success
                Health.UNKNOWN -> fluent.textSecondary
            }
            val drifting = (h.clockDriftMsPerSec ?: 0.0) > 1.0

            Column {
                CardGroupHeader(player.name)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsCard(
                        header = "Buffer",
                        description = "Fill against the ${config.bufferMs} ms server buffer",
                        icon = Icons.Outlined.GraphicEq,
                        iconTint = statusColor,
                    ) {
                        Meter(
                            fraction = if (config.bufferMs <= 0) 0f
                            else h.bufferMs.toFloat() / config.bufferMs,
                            color = statusColor,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(h.bufferMs.toString(), style = MetricValueTextStyle, color = statusColor)
                        Text("ms", style = UnitTextStyle, color = fluent.textSecondary)
                    }

                    SettingsCard(
                        header = "Timing",
                        description = "Offset to the server, and any clock rate error",
                    ) {
                        Figure("sync", "${h.syncMs} ms", fluent.textSecondary, MaterialTheme.colorScheme.onSurface)
                        Figure(
                            label = "drift",
                            value = h.clockDriftMsPerSec?.let { "%.1f ms/s".format(it) } ?: "none",
                            labelColor = fluent.textSecondary,
                            valueColor = if (drifting) fluent.critical else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    SettingsCard(
                        header = "Playback faults",
                        description = h.diagnosis ?: "No faults recorded",
                    ) {
                        Figure("starved", "${h.starvedCount}", fluent.textSecondary, tintIfNonZero(h.starvedCount, fluent.caution, MaterialTheme.colorScheme.onSurface))
                        Figure("underruns", "${h.underrunCount}", fluent.textSecondary, tintIfNonZero(h.underrunCount, fluent.caution, MaterialTheme.colorScheme.onSurface))
                        Figure("resyncs", "${h.resyncCount}", fluent.textSecondary, MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, labelColor: Color, valueColor: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = label, style = UnitTextStyle, color = labelColor)
        Text(text = value, style = MetricValueTextStyle, color = valueColor)
    }
}

@Composable
private fun Meter(fraction: Float, color: Color) {
    val fluent = LocalFluent.current
    Box(
        Modifier
            .width(120.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(fluent.controlStroke),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

private fun tintIfNonZero(count: Int, warn: Color, normal: Color): Color =
    if (count > 0) warn else normal
