package com.multiroom.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.multiroom.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players ORDER BY position, name")
    fun observeAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY position, name")
    suspend fun getAll(): List<PlayerEntity>

    @Upsert
    suspend fun save(player: PlayerEntity)

    @Upsert
    suspend fun saveAll(players: List<PlayerEntity>)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Removes rows the caller no longer holds, so a delete actually sticks. */
    @Query("DELETE FROM players WHERE id NOT IN (:keep)")
    suspend fun deleteNotIn(keep: List<String>)

    @Query("DELETE FROM players")
    suspend fun deleteAll()
}
