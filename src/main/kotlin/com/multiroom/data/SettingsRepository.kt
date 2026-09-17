package com.multiroom.data

import com.multiroom.data.entity.PlayerEntity
import com.multiroom.data.entity.SettingsEntity
import com.multiroom.data.entity.ThemeMode
import com.multiroom.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Everything the app persists: server address, theme, and the players this
 * machine runs.
 *
 * Settings and players are separate tables, read as one [AppSettings].
 */
class SettingsRepository(private val db: AppDatabase) {

    val settings: Flow<AppSettings> =
        combine(db.settings().observe(), db.players().observeAll()) { stored, players ->
            val s = stored ?: SettingsEntity()
            AppSettings(
                serverHost = s.serverHost,
                streamPort = s.streamPort,
                controlPort = s.controlPort,
                bufferMs = s.bufferMs,
                themeMode = s.themeMode,
                launchAtLogon = s.launchAtLogon,
                players = players.map(PlayerEntity::toDomain),
            )
        }

    /** Emits the players alone, for callers that do not need server settings. */
    val players: Flow<List<Player>> =
        db.players().observeAll().map { list -> list.map(PlayerEntity::toDomain) }

    suspend fun save(settings: AppSettings) {
        db.settings().save(
            SettingsEntity(
                serverHost = settings.serverHost,
                streamPort = settings.streamPort,
                controlPort = settings.controlPort,
                bufferMs = settings.bufferMs,
                themeMode = settings.themeMode,
                launchAtLogon = settings.launchAtLogon,
            )
        )
        // Upserting alone would leave a removed player in the table.
        if (settings.players.isEmpty()) {
            db.players().deleteAll()
        } else {
            db.players().deleteNotIn(settings.players.map { it.id })
            db.players().saveAll(
                settings.players.mapIndexed { index, player -> player.toEntity(index) }
            )
        }
    }

    suspend fun savePlayer(player: Player, position: Int = 0) =
        db.players().save(player.toEntity(position))

    suspend fun deletePlayer(id: String) = db.players().deleteById(id)
}

/** Persisted state as the UI consumes it. */
data class AppSettings(
    val serverHost: String = "127.0.0.1",
    val streamPort: Int = 1704,
    val controlPort: Int = 1780,
    val bufferMs: Int = 1000,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val launchAtLogon: Boolean = false,
    val players: List<Player> = emptyList(),
)

private fun PlayerEntity.toDomain() = Player(
    id = id,
    name = name,
    deviceId = deviceId,
    instance = instance,
    sampleFormat = sampleFormat,
    autoStart = autoStart,
)

private fun Player.toEntity(position: Int) = PlayerEntity(
    id = id,
    name = name,
    deviceId = deviceId,
    instance = instance,
    sampleFormat = sampleFormat,
    autoStart = autoStart,
    position = position,
)
