package com.multiroom.process

import com.multiroom.model.Player
import com.multiroom.diagnostics.ClientLog
import com.multiroom.diagnostics.Health
import com.multiroom.diagnostics.PlayerHealth
import com.multiroom.diagnostics.PlayerHealthTracker
import com.multiroom.model.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.Writer
import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import java.util.concurrent.ConcurrentHashMap

/** Runs one snapclient process per player and restarts it if it exits. */
class SnapclientSupervisor(
    private val snapclientExe: () -> Path,
    private val scope: CoroutineScope,
) {
    private val jobs = ConcurrentHashMap<String, Job>()
    private val launched = ConcurrentHashMap<String, Player>()
    private val processes = ConcurrentHashMap<String, Process>()
    private val states = MutableStateFlow<Map<String, PlayerState>>(emptyMap())
    private val health = MutableStateFlow<Map<String, PlayerHealth>>(emptyMap())

    /** Current state per player id. */
    val playerStates: StateFlow<Map<String, PlayerState>> = states

    /** Rolling health per player id, derived from each client's own log. */
    val playerHealth: StateFlow<Map<String, PlayerHealth>> = health

    /**
     * Starts [player] and keeps it running. Does nothing if already started.
     *
     * @param host Snapserver hostname or IP
     * @param port Snapserver stream port, normally 1704
     */
    fun start(player: Player, host: String, port: Int) {
        if (jobs.containsKey(player.id)) return
        launched[player.id] = player
        jobs[player.id] = scope.launch(Dispatchers.IO) {
            var backoffMs = 2_000L
            while (isActive) {
                setState(player.id, PlayerState.STARTING)
                val exit = runOnce(player, host, port)
                if (!isActive) break
                setState(player.id, PlayerState.CRASHED)
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                if (exit == 0) backoffMs = 2_000L
            }
            setState(player.id, PlayerState.STOPPED)
        }
    }

    /** Stops [playerId] and does not restart it. */
    fun stop(playerId: String) {
        launched.remove(playerId)
        jobs.remove(playerId)?.cancel()
        processes.remove(playerId)?.destroy()
        setState(playerId, PlayerState.STOPPED)
    }

    /** Stops every player. Call before the app exits. */
    fun stopAll() = jobs.keys.toList().forEach(::stop)

    /** PIDs of the clients this supervisor owns. */
    fun ownedPids(): Set<Long> = processes.values.mapNotNull { it.pid() }.toSet()

    /** Stops any snapclient already serving [player], then starts it. */
    fun adopt(player: Player, host: String, port: Int) {
        scope.launch(Dispatchers.IO) {
            ProcessScanner.stopStrays(player.name, player.instance, ownedPids())
            start(player, host, port)
        }
    }

    /** Restarts running players whose launch settings changed. */
    fun reconcile(players: List<Player>, host: String, port: Int) {
        players.forEach { player ->
            val running = launched[player.id] ?: return@forEach
            if (!running.launchesAs(player)) {
                stop(player.id)
                adopt(player, host, port)
            }
        }
    }

    private fun Player.launchesAs(other: Player) =
        name == other.name &&
            deviceId == other.deviceId &&
            instance == other.instance &&
            sampleFormat == other.sampleFormat

    private companion object {
        /** Exit code used when the process never started. */
        const val MISSING_BINARY = -1
    }

    private suspend fun runOnce(player: Player, host: String, port: Int): Int =
        withContext(Dispatchers.IO) {
            if (player.name.isBlank()) {
                setHealth(
                    player.id,
                    PlayerHealth(
                        health = Health.BROKEN,
                        diagnosis = "This player has no name. snapclient rejects an " +
                            "empty client id. Set a name in Settings.",
                    ),
                )
                return@withContext MISSING_BINARY
            }

            val exe = snapclientExe()
            if (!exe.exists()) {
                setHealth(
                    player.id,
                    PlayerHealth(
                        health = Health.BROKEN,
                        diagnosis = "snapclient.exe not found at $exe. " +
                            "Set the path in Settings.",
                    ),
                )
                return@withContext MISSING_BINARY
            }

            val process = try {
                ProcessBuilder(buildCommand(player, host, port))
                    .directory(exe.parent?.toFile())
                    .redirectErrorStream(true)
                    .start()
            } catch (e: IOException) {
                setHealth(
                    player.id,
                    PlayerHealth(
                        health = Health.BROKEN,
                        diagnosis = "Could not start snapclient: ${e.message}",
                    ),
                )
                return@withContext MISSING_BINARY
            }

            processes[player.id] = process
            setState(player.id, PlayerState.RUNNING)

            val log = logFor(player)
            log?.write(buildCommand(player, host, port).joinToString(" "))

            val tracker = PlayerHealthTracker()
            runCatching {
                process.inputStream.bufferedReader().forEachLine { line ->
                    log?.write(line)
                    tracker.onEvent(ClientLog.parse(line))
                    health.value = health.value + (player.id to tracker.state)
                }
            }

            val code = runCatching { process.waitFor() }.getOrDefault(MISSING_BINARY)
            log?.write("snapclient exited with code $code")
            log?.close()
            processes.remove(player.id)
            code
        }

    /**
     * Opens this player's log, replacing the previous run's.
     *
     * @returns null when the log cannot be opened
     */
    private fun logFor(player: Player): PlayerLog? = runCatching {
        val directory = Path.of(
            System.getenv("APPDATA") ?: System.getProperty("user.home"),
            "SnapcastRooms",
            "logs",
        )
        directory.createDirectories()
        val safeName = player.name.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
        PlayerLog(directory.resolve("$safeName.log").bufferedWriter())
    }.getOrNull()

    private fun buildCommand(player: Player, host: String, port: Int): List<String> =
        buildList {
            add(snapclientExe().toString())
            add("tcp://$host:$port")
            add("--instance"); add(player.instance.toString())
            add("--soundcard"); add(player.deviceId)
            add("--hostID"); add(player.name)
            add("--player"); add("wasapi")
            add("--sharingmode"); add("shared")
            player.sampleFormat?.let { add("--sampleformat"); add(it) }
            add("--logfilter"); add("*:info")
        }

    private fun setHealth(playerId: String, value: PlayerHealth) {
        health.value = health.value + (playerId to value)
    }

    private fun setState(playerId: String, state: PlayerState) {
        states.value = states.value + (playerId to state)
    }
}

/** One player's snapclient output on disk. */
private class PlayerLog(private val writer: Writer) {

    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun write(line: String) = runCatching {
        writer.write("${LocalTime.now().format(clock)}  $line")
        writer.write(System.lineSeparator())
        writer.flush()
    }.let { }

    fun close() = runCatching { writer.close() }.let { }
}
