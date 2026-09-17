package com.multiroom.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A snapclient instance this machine runs.
 *
 * @property deviceId Windows audio endpoint id, or "default" to follow the
 *   Windows default device
 * @property instance snapclient --instance number; unique on this machine
 * @property sampleFormat resample target such as "48000:16:*", or null to take
 *   the rate the server streams at
 */
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceId: String = "default",
    val instance: Int = 1,
    val sampleFormat: String? = null,
    val autoStart: Boolean = true,
    /** Display order in the UI. */
    val position: Int = 0,
)
