package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val actionType: String, // e.g., "CARPET_REGISTRATION", "ORDER_STATUS_UPDATE", "SETTLEMENT_FINALIZED", "RACK_ASSIGNMENT", "GPS_LOG"
    val orderId: String,
    val title: String, // Descriptive title in Farsi for UI list display
    val payloadJson: String, // Detailed metadata/json
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "SYNCING", "SYNCED", "FAILED"
    val retryCount: Int = 0,
    val lastError: String? = null
)
