package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey
    val id: String = "DRV-101",
    val name: String = "سفیر شماره ۱۰۱ (مسعود بختیاری)",
    val phone: String = "09123456789",
    val vehicleType: String = "وانت نیسان مسقف",
    val vehiclePlate: String = "ایران ۱۱ - ۲۵۸ ج ۹۴",
    val status: String = "active", // "active", "on_delivery", "off_duty", "suspended"
    val currentLat: Double = 35.7796,
    val currentLng: Double = 51.4058,
    val batteryLevel: Int = 85,
    val speed: Float = 0.0f,
    val appStatus: String = "active",
    val lastAppLogin: Long = System.currentTimeMillis(),
    val totalCollectedCash: Long = 0L,
    val totalCollectedPos: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
