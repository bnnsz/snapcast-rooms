package com.multiroom.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.multiroom.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    /** Emits null until settings have been written for the first time. */
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun save(settings: SettingsEntity)
}
