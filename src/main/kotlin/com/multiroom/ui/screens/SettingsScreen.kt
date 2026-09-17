package com.multiroom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.multiroom.audio.AudioDevice
import com.multiroom.data.AppSettings
import com.multiroom.data.entity.ThemeMode
import com.multiroom.model.Player
import com.multiroom.process.InstallState
import com.multiroom.process.SnapclientRelease
import com.multiroom.process.SnapclientStore
import com.multiroom.process.SnapclientInstaller
import com.multiroom.ui.util.FilePicker
import com.multiroom.ui.components.CardGroupHeader
import com.multiroom.ui.components.ComboOption
import com.multiroom.ui.components.FieldKind
import com.multiroom.ui.components.FluentButton
import com.multiroom.ui.components.FluentComboBox
import com.multiroom.ui.components.FluentDialog
import com.multiroom.ui.components.FluentMenuButton
import com.multiroom.ui.components.MenuAction
import com.multiroom.ui.components.FluentTextBox
import com.multiroom.ui.components.FluentToggle
import com.multiroom.ui.components.ExpanderRow
import com.multiroom.ui.components.SettingsCard
import com.multiroom.ui.components.SettingsExpander
import com.multiroom.ui.theme.CardGap
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.CardGroupGap
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Server address, appearance, startup and players. */
@Composable
fun SettingsScreen(
    config: AppSettings,
    devices: List<AudioDevice>,
    autoStartEnabled: Boolean,
    onConfigChange: (AppSettings) -> Unit,
    onRemovePlayer: (Player) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(CardGroupGap)) {

        Column {
            CardGroupHeader("Server")
            Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
                SettingsCard(
                    header = "Snapserver address",
                    description = "Where this PC connects to receive audio",
                    icon = Icons.Outlined.Dns,
                ) {
                    FluentTextBox(
                        value = config.serverHost,
                        onValueChange = { onConfigChange(config.copy(serverHost = it)) },
                        kind = FieldKind.IP_ADDRESS,
                        modifier = Modifier.width(140.dp),
                    )
                }

                SettingsCard(
                    header = "Stream port",
                    description = "Port snapclients connect to, normally 1704",
                    icon = Icons.Outlined.SettingsEthernet,
                ) {
                    FluentTextBox(
                        value = config.streamPort.toString(),
                        onValueChange = { t ->
                            t.toIntOrNull()?.let { onConfigChange(config.copy(streamPort = it)) }
                        },
                        kind = FieldKind.NUMBER,
                        modifier = Modifier.width(92.dp),
                    )
                }

                SettingsCard(
                    header = "Control port",
                    description = "JSON-RPC port used to read status, normally 1780",
                    icon = Icons.Outlined.Tune,
                ) {
                    FluentTextBox(
                        value = config.controlPort.toString(),
                        onValueChange = { t ->
                            t.toIntOrNull()?.let { onConfigChange(config.copy(controlPort = it)) }
                        },
                        kind = FieldKind.NUMBER,
                        modifier = Modifier.width(92.dp),
                    )
                }

                SettingsCard(
                    header = "Buffer",
                    description = "End-to-end latency budget shared by every player",
                    icon = Icons.Outlined.Timer,
                ) {
                    FluentTextBox(
                        value = config.bufferMs.toString(),
                        onValueChange = { t ->
                            t.toIntOrNull()?.let { onConfigChange(config.copy(bufferMs = it)) }
                        },
                        kind = FieldKind.NUMBER,
                        suffix = "ms",
                        modifier = Modifier.width(110.dp),
                    )
                }
            }
        }

        Column {
            CardGroupHeader("Snapclient")
            SnapclientCard()
        }

        Column {
            CardGroupHeader("Appearance and startup")
            Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
                SettingsCard(
                    header = "App theme",
                    description = "Choose the colour scheme",
                    icon = Icons.Outlined.Palette,
                ) {
                    FluentComboBox(
                        selected = config.themeMode,
                        options = listOf(
                            ComboOption(ThemeMode.SYSTEM, "Use system setting"),
                            ComboOption(ThemeMode.LIGHT, "Light"),
                            ComboOption(ThemeMode.DARK, "Dark"),
                        ),
                        onSelect = { onConfigChange(config.copy(themeMode = it)) },
                        modifier = Modifier.width(180.dp),
                    )
                }

                SettingsCard(
                    header = "Start with Windows",
                    description = "Runs in your session so audio devices are reachable",
                    icon = Icons.Outlined.PowerSettingsNew,
                ) {
                    FluentToggle(checked = autoStartEnabled, onCheckedChange = onAutoStartChange)
                }
            }
        }

        Column {
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGroupHeader("Players", Modifier.weight(1f).padding(bottom = 0.dp))
                FluentButton(
                    text = "Add player",
                    icon = Icons.Outlined.Add,
                    onClick = { adding = true },
                )
            }

            if (adding) {
                AddPlayerDialog(
                    devices = devices,
                    taken = config.players.map { it.name }.toSet(),
                    onDismiss = { adding = false },
                    onCreate = { name, deviceId ->
                        adding = false
                        val next = (config.players.maxOfOrNull { it.instance } ?: 0) + 1
                        onConfigChange(
                            config.copy(
                                players = config.players + Player(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    deviceId = deviceId,
                                    instance = next,
                                )
                            )
                        )
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
                if (config.players.isEmpty()) {
                    SettingsCard(
                        header = "No players configured",
                        description = "Add a player to send audio to an output on this PC",
                        icon = Icons.Outlined.Speaker,
                    )
                }

                config.players.forEach { player ->
                    PlayerSettingsCard(
                        player = player,
                        devices = devices,
                        onChange = { updated ->
                            onConfigChange(
                                config.copy(
                                    players = config.players.map {
                                        if (it.id == player.id) updated else it
                                    }
                                )
                            )
                        },
                        onDelete = { onRemovePlayer(player) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSettingsCard(
    player: Player,
    devices: List<AudioDevice>,
    onChange: (Player) -> Unit,
    onDelete: () -> Unit,
) {
    val deviceOptions = buildList {
        add(ComboOption("default", "Windows default device", Icons.Outlined.Speaker))
        devices.filterNot { it.id == "default" }.forEach {
            add(ComboOption(it.id, it.description, Icons.Outlined.Headphones))
        }
    }
    val deviceLabel = deviceOptions.firstOrNull { it.value == player.deviceId }?.label
        ?: player.deviceId

    SettingsExpander(
        header = player.name,
        description = deviceLabel,
        icon = Icons.Outlined.Speaker,
    ) {
        ExpanderRow(
            label = "Player name",
            description = "Shown on the server and in Snapweb",
            showDivider = false,
        ) {
            FluentTextBox(
                value = player.name,
                // snapclient rejects an empty --hostID.
                onValueChange = { edited ->
                    val trimmed = edited.trim()
                    if (trimmed.isNotEmpty()) onChange(player.copy(name = trimmed))
                },
                modifier = Modifier.width(220.dp),
            )
        }

        ExpanderRow(
            label = "Output device",
            description = "Where this player plays audio on this PC",
        ) {
            FluentComboBox(
                selected = player.deviceId,
                options = deviceOptions,
                onSelect = { onChange(player.copy(deviceId = it)) },
                modifier = Modifier.width(260.dp),
            )
        }

        ExpanderRow(
            label = "Resample",
            description = "Leave empty unless the server rate differs from the device rate",
        ) {
            FluentTextBox(
                value = player.sampleFormat.orEmpty(),
                onValueChange = {
                    onChange(player.copy(sampleFormat = it.ifBlank { null }))
                },
                modifier = Modifier.width(150.dp),
            )
        }

        ExpanderRow(
            label = "Start automatically",
            description = "Launch this player when the app starts",
        ) {
            FluentToggle(
                checked = player.autoStart,
                onCheckedChange = { onChange(player.copy(autoStart = it)) },
            )
        }

        ExpanderRow(
            label = "Remove this player",
            description = "Instance ${player.instance} will stop and be forgotten",
        ) {
            FluentButton(
                text = "Remove",
                icon = Icons.Outlined.Delete,
                destructive = true,
                onClick = onDelete,
            )
        }
    }
}

/** The app's snapclient, installed by download or by browsing to a build. */
@Composable
private fun SnapclientCard() {
    val scope = rememberCoroutineScope()
    val fluent = LocalFluent.current

    var installed by remember { mutableStateOf(SnapclientStore.isInstalled()) }
    var version by remember { mutableStateOf<String?>(null) }
    var latest by remember { mutableStateOf<SnapclientRelease?>(null) }
    var progress by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }

    suspend fun refresh() {
        installed = SnapclientStore.isInstalled()
        version = SnapclientStore.version()
    }

    LaunchedEffect(Unit) {
        refresh()
        latest = runCatching { SnapclientInstaller.latest() }.getOrNull()
    }

    val description = when (val state = progress) {
        is InstallState.Downloading -> "Downloading ${(state.fraction * 100).toInt()}%"
        InstallState.Extracting -> "Extracting"
        is InstallState.Failed -> state.message
        else -> when {
            installed && version != null -> "Version $version"
            installed -> "Installed"
            else -> "Not installed - download it, or select an existing build"
        }
    }

    val busy = progress is InstallState.Downloading || progress == InstallState.Extracting

    SettingsCard(
        header = "Snapclient",
        description = description,
        icon = Icons.Outlined.Terminal,
        iconTint = when {
            progress is InstallState.Failed -> fluent.critical
            !installed -> fluent.caution
            else -> null
        },
    ) {
        if (busy) {
            FluentButton(text = "Cancel") {
                job?.cancel()
                job = null
                progress = InstallState.Idle
            }
            return@SettingsCard
        }

        FluentMenuButton(
            text = if (installed) "Change" else "Install",
            icon = Icons.Outlined.Download,
            enabled = !busy,
            actions = listOf(
                MenuAction(
                    label = latest?.let { "Download ${it.version}" } ?: "Download latest",
                    icon = Icons.Outlined.Download,
                    enabled = latest != null,
                ) {
                    val release = latest ?: return@MenuAction
                    job = scope.launch {
                        progress = InstallState.Downloading(0f)
                        progress = try {
                            SnapclientStore.installFrom(release) { fraction ->
                                progress = InstallState.Downloading(fraction)
                            }
                            refresh()
                            InstallState.Idle
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            InstallState.Failed(e.message ?: "Download failed")
                        }
                    }
                },
                MenuAction(
                    label = "Browse for snapclient.exe",
                    icon = Icons.Outlined.FolderOpen,
                ) {
                    val chosen = FilePicker.chooseFile(
                        title = "Select snapclient.exe",
                        extension = "exe",
                    ) ?: return@MenuAction
                    scope.launch {
                        progress = InstallState.Extracting
                        progress = runCatching {
                            SnapclientStore.installFrom(chosen)
                            refresh()
                            InstallState.Idle
                        }.getOrElse { InstallState.Failed(it.message ?: "Copy failed") }
                    }
                },
            ),
        )
    }
}

/**
 * Collects a new player's name and output device before anything is created.
 *
 * @param taken names already in use, which a new player may not reuse
 */
@Composable
private fun AddPlayerDialog(
    devices: List<AudioDevice>,
    taken: Set<String>,
    onDismiss: () -> Unit,
    onCreate: (name: String, deviceId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("default") }

    val trimmed = name.trim()
    val duplicate = trimmed in taken
    val valid = trimmed.isNotEmpty() && !duplicate

    val deviceOptions = buildList {
        add(ComboOption("default", "Windows default device", Icons.Outlined.Speaker))
        devices.filterNot { it.id == "default" }.forEach {
            add(ComboOption(it.id, it.description, Icons.Outlined.Headphones))
        }
    }

    FluentDialog(
        title = "Add a player",
        confirmText = "Create",
        confirmEnabled = valid,
        onConfirm = { onCreate(trimmed, deviceId) },
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Player name",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalFluent.current.textSecondary,
        )
        FluentTextBox(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            commitWhileTyping = true,
        )
        Text(
            text = when {
                duplicate -> "A player called \"$trimmed\" already exists"
                else -> "Shown on the server and in Snapweb"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (duplicate) LocalFluent.current.critical
            else LocalFluent.current.textSecondary,
        )

        Text(
            text = "Output device",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalFluent.current.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
        FluentComboBox(
            selected = deviceId,
            options = deviceOptions,
            onSelect = { deviceId = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
