package com.multiroom

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.multiroom.audio.AudioDevice
import com.multiroom.audio.AudioDevices
import com.multiroom.data.AppSettings
import com.multiroom.data.PlayerSync
import com.multiroom.data.SettingsRepository
import com.multiroom.model.Player
import com.multiroom.data.entity.ThemeMode
import com.multiroom.data.openDatabase
import com.multiroom.process.SnapclientStore
import com.multiroom.process.SnapclientSupervisor
import com.multiroom.rpc.SnapcastControl
import com.multiroom.startup.AutoStart
import com.multiroom.ui.App
import com.multiroom.ui.components.NavMode
import com.multiroom.ui.components.TitleBar
import com.multiroom.ui.theme.NavCompactThreshold
import com.multiroom.ui.theme.NavExpandedThreshold
import com.multiroom.ui.theme.RoomsTheme
import com.multiroom.ui.util.loadSvg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension

/**
 * Entry point.
 *
 * Closing the window hides it; exit is explicit from the tray menu.
 */
private const val MinWindowWidth = 480
private const val MinWindowHeight = 520
private const val MaxWindowWidth = 1240
private const val MaxWindowHeight = 860

/** How often to check the control connection and re-open it if it dropped. */
private const val ReconnectIntervalMs = 5_000L

fun main() = application {
    val db = remember { openDatabase() }
    val repository = remember { SettingsRepository(db) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val uiScope = rememberCoroutineScope()

    val settings by repository.settings.collectAsState(initial = AppSettings())

    val supervisor = remember { SnapclientSupervisor({ SnapclientStore.executable }, scope) }
    val control = remember(settings.serverHost, settings.controlPort) {
        SnapcastControl(settings.serverHost, settings.controlPort)
    }

    var devices by remember { mutableStateOf(emptyList<AudioDevice>()) }
    var windowVisible by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    val appIcon = remember { loadSvg("snapcast-logo.svg", Density(1f)) }

    LaunchedEffect(Unit) {
        devices = runCatching { AudioDevices.list(SnapclientStore.executable) }
            .getOrDefault(emptyList())
    }

    val serverClients by control.clients.collectAsState()

    // Discovery must not hand a removed player straight back while the server
    // still reports its client.
    val dismissedClients = remember { mutableStateListOf<String>() }

    LaunchedEffect(serverClients, settings.players) {
        val discovered =
            PlayerSync.discover(settings.players, serverClients, dismissedClients.toSet())
        if (discovered.isNotEmpty()) {
            repository.save(settings.copy(players = settings.players + discovered))
        }
    }

    // A client displays as its host name until it is given a name of its own.
    LaunchedEffect(serverClients, settings.players) {
        settings.players.forEach { player ->
            val client = serverClients.firstOrNull { it.id == player.clientId }
            if (client != null && client.configuredName != player.name) {
                control.setName(player.clientId, player.name)
            }
        }
    }

    // Renaming a player changes its client id, leaving the old one on the server.
    val saveSettings: (AppSettings) -> Unit = { updated ->
        val before = settings.players.associateBy { it.id }
        updated.players.forEach { player ->
            val previous = before[player.id] ?: return@forEach
            if (previous.clientId != player.clientId) {
                dismissedClients += previous.clientId
                control.removeClient(previous.clientId)
            }
        }
        uiScope.launch { repository.save(updated) }
    }

    val removePlayer: (Player) -> Unit = { player ->
        supervisor.stop(player.id)
        dismissedClients += player.clientId
        control.removeClient(player.clientId)
        uiScope.launch {
            repository.save(
                settings.copy(players = settings.players.filterNot { it.id == player.id })
            )
        }
    }

    // Connects independently of whether any player exists, and reconnects after
    // a server restart.
    LaunchedEffect(control) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                if (!control.connected.value) runCatching { control.connect() }
                delay(ReconnectIntervalMs)
            }
        }
    }

    DisposableEffect(control) {
        onDispose { control.close() }
    }

    LaunchedEffect(settings.players, settings.serverHost, settings.streamPort) {
        supervisor.reconcile(settings.players, settings.serverHost, settings.streamPort)
    }

    // Runs once, after settings have loaded from the database.
    LaunchedEffect(settings.players) {
        if (!started && settings.players.isNotEmpty()) {
            started = true
            settings.players.filter { it.autoStart }
                .forEach { supervisor.start(it, settings.serverHost, settings.streamPort) }
        }
    }

    val trayState = rememberTrayState()
    Tray(
        state = trayState,
        icon = appIcon,
        tooltip = "Snapcast Rooms",
        onAction = { windowVisible = true },
        menu = {
            Item("Show") { windowVisible = true }
            Item("Stop all players") { supervisor.stopAll() }
            Item("Exit") {
                supervisor.stopAll()
                control.close()
                exitApplication()
            }
        },
    )

    if (windowVisible) {
        val windowState = rememberWindowState(width = 1040.dp, height = 700.dp)
        Window(
            onCloseRequest = { windowVisible = false },
            state = windowState,
            title = "Snapcast Rooms",
            undecorated = true,
            icon = appIcon,
        ) {
            // Set on the window itself; clamping the state from a resize listener
            // oscillates against the drag.
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(MinWindowWidth, MinWindowHeight)
                window.maximumSize = Dimension(MaxWindowWidth, MaxWindowHeight)
            }

            val navMode = when {
                windowState.size.width >= NavExpandedThreshold -> NavMode.Expanded
                windowState.size.width >= NavCompactThreshold -> NavMode.Compact
                else -> NavMode.Minimal
            }
            var paneOpen by remember { mutableStateOf(false) }
            if (navMode == NavMode.Expanded && paneOpen) paneOpen = false

            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            RoomsTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        TitleBar(
                            title = "Snapcast Rooms",
                            showMenuButton = navMode != NavMode.Expanded,
                            onMenuClick = { paneOpen = !paneOpen },
                            onClose = { windowVisible = false },
                        )
                        App(
                            config = settings,
                            devices = devices,
                            autoStartEnabled = settings.launchAtLogon,
                            supervisor = supervisor,
                            control = control,
                            navMode = navMode,
                            paneOpen = paneOpen,
                            onPaneOpenChange = { paneOpen = it },
                            onConfigChange = saveSettings,
                            onRemovePlayer = removePlayer,
                            onAutoStartChange = { enabled ->
                                val exe = ProcessHandle.current().info().command().orElse("")
                                if (enabled) AutoStart.enable(exe) else AutoStart.disable()
                                uiScope.launch {
                                    repository.save(settings.copy(launchAtLogon = enabled))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
