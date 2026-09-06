package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "carpet_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class CarpetItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val orderId: String,
    val carpetType: String, // "ماشینی", "دستبافت", "ابریشم", "گلیم"
    val lengthMeter: Double,
    val widthMeter: Double,
    val areaSqMeter: Double,
    val unitPricePerMeter: Long,
    val requestedServicesJson: String, // e.g. "اعلاشویی, رفوگری, لکه‌بری"
    val defectsJson: String, // e.g. "سوختگی, بیدزدگی"
    val totalPrice: Long,
    val notes: String = "",
    val barcodeTag: String = ""
)
