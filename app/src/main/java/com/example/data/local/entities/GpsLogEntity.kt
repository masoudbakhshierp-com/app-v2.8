package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_logs")
data class GpsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
