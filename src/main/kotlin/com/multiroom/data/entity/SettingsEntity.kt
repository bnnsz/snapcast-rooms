package com.multiroom.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Which colour scheme to render. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Application settings. Single row, always id 1, so reads never have to decide
 * between competing records.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val serverHost: String = "127.0.0.1",
    val streamPort: Int = 1704,
    val controlPort: Int = 1780,
    /** Server buffer in ms; full scale for the buffer meters. */
    val bufferMs: Int = 1000,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val launchAtLogon: Boolean = false,
)
