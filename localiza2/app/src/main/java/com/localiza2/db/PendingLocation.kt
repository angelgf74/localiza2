package com.localiza2.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_locations")
data class PendingLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val batteryLevel: Int?,
    val timestamp: Long = System.currentTimeMillis()
)
