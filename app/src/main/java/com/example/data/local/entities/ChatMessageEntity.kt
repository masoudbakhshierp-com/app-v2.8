package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val serverId: String = "",
    val orderId: String = "GENERAL",
    val sender: String, // "DRIVER" or "DISPATCHER"
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

