package com.multiroom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.multiroom.audio.AudioDevice
import com.multiroom.data.AppSettings
import com.multiroom.model.Player
import com.multiroom.process.SnapclientSupervisor
import com.multiroom.rpc.SnapcastControl
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.multiroom.model.PlayerState
import com.multiroom.ui.components.NavMode
import com.multiroom.ui.components.NavigationPane
import com.multiroom.ui.components.Screen
import com.multiroom.ui.screens.DiagnosticsScreen
import com.multiroom.ui.screens.GroupsScreen
import com.multiroom.ui.screens.PlayersScreen
import com.multiroom.ui.screens.SettingsScreen
import com.multiroom.ui.theme.CardGroupGap
import com.multiroom.ui.theme.ContentMaxWidth
import com.multiroom.ui.theme.ContentStartPadding
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.FluentMotion
import com.multiroom.ui.theme.NavCompactThreshold
import com.multiroom.ui.theme.NavExpandedThreshold
import com.multiroom.ui.theme.LocalFluent

/**
 * NavigationView layout.
 *
 * Content sits directly on the page background, as Windows 11 Settings draws
 * it, with no card or layer wrapping the scroll region.
 */
@Composable
fun App(
    config: AppSettings,
    devices: List<AudioDevice>,
    autoStartEnabled: Boolean,
    supervisor: SnapclientSupervisor,
    control: SnapcastControl,
    navMode: NavMode,
    paneOpen: Boolean,
    onPaneOpenChange: (Boolean) -> Unit,
    onConfigChange: (AppSettings) -> Unit,
    onRemovePlayer: (Player) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.Players) }
    val states by supervisor.playerStates.collectAsState()
    val health by supervisor.playerHealth.collectAsState()
    val serverClients by control.clients.collectAsState()
    val serverGroups by control.groups.collectAsState()
    val serverStreams by control.streams.collectAsState()

    Box(Modifier.fillMaxSize()) {
        val mode = navMode

        Row(Modifier.fillMaxSize()) {
            if (mode != NavMode.Minimal) {
                NavigationPane(
                    current = screen,
                    onNavigate = { screen = it },
                    serverLabel = "${config.serverHost}:${config.streamPort}",
                    mode = mode,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = ContentStartPadding, end = ContentStartPadding),
            ) {
                Spacer(Modifier.height(20.dp))

                PageTitle(screen)

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = ContentMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(CardGroupGap),
                ) {
                    when (screen) {
                        Screen.Players -> PlayersScreen(
                            config = config,
                            devices = devices,
                            states = states,
                            health = health,
                            serverClients = serverClients,
                            serverGroups = serverGroups,
                            streams = serverStreams,
                            onToggle = { player: Player ->
                                if (states[player.id] == PlayerState.RUNNING) {
                                    supervisor.stop(player.id)
                                } else {
                                    supervisor.adopt(player, config.serverHost, config.streamPort)
                                }
                            },
                            onVolumeChange = control::setVolume,
                        )

                        Screen.Groups -> GroupsScreen(
                            groups = serverGroups,
                            onMoveClientToGroup = control::moveClientToGroup,
                            onSetGroupMute = control::setGroupMute,
                        )

                        Screen.Diagnostics -> DiagnosticsScreen(config, health)

                        Screen.Settings -> SettingsScreen(
                            config = config,
                            devices = devices,
                            autoStartEnabled = autoStartEnabled,
                            onConfigChange = onConfigChange,
                            onRemovePlayer = onRemovePlayer,
                            onAutoStartChange = onAutoStartChange,
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // The pane is drawn over the content when it is not docked. The scrim
        // fades while the pane slides, so the two do not arrive together.
        AnimatedVisibility(
            visible = mode != NavMode.Expanded && paneOpen,
            enter = fadeIn(FluentMotion.enter(FluentMotion.FAST)),
            exit = fadeOut(FluentMotion.exit()),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPaneOpenChange(false) },
            )
        }

        AnimatedVisibility(
            visible = mode != NavMode.Expanded && paneOpen,
            enter = slideInHorizontally(FluentMotion.enter()) { -it } +
                fadeIn(FluentMotion.enter(FluentMotion.FAST)),
            exit = slideOutHorizontally(FluentMotion.exit()) { -it } +
                fadeOut(FluentMotion.exit()),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .shadow(16.dp)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                NavigationPane(
                    current = screen,
                    onNavigate = {
                        screen = it
                        onPaneOpenChange(false)
                    },
                    serverLabel = "${config.serverHost}:${config.streamPort}",
                    mode = NavMode.Expanded,
                )
            }
        }
    }
}

/** Breadcrumb page heading, e.g. "Snapcast Rooms  >  Diagnostics". */
@Composable
private fun PageTitle(screen: Screen) {
    val fluent = LocalFluent.current
    val title = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal)) {
            append("Snapcast Rooms")
        }
        withStyle(SpanStyle(color = fluent.textSecondary, fontWeight = FontWeight.Normal)) {
            append("  ›  ")
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold)) {
            append(screen.label)
        }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
