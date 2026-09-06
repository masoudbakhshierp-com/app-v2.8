package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_settlements")
data class DriverSettlementEntity(
    @PrimaryKey
    val id: String, // e.g. "STL-1403-0805-01"
    val driverId: String = "DRV-101",
    val driverName: String = "مسعود بختیاری",
    val date: String, // e.g. "۱۴۰۳/۰۸/۰۵"
    val totalCash: Long = 0L,
    val totalPos: Long = 0L,
    val totalCardToCard: Long = 0L,
    val totalOnline: Long = 0L,
    val totalAmount: Long = 0L,
    val ordersCount: Int = 0,
    val returnedOrdersCount: Int = 0,
    val orderIdsJson: String = "[]", // JSON array of order ids
    val status: String = "pending_approval", // "pending_approval", "approved", "rejected"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
