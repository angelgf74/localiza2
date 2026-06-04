package com.localiza2.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingLocationDao {

    @Insert
    suspend fun insert(location: PendingLocation)

    @Query("SELECT * FROM pending_locations ORDER BY timestamp ASC LIMIT 50")
    suspend fun getOldest(): List<PendingLocation>

    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun count(): Int
}
