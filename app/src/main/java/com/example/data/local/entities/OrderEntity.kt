package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String, // e.g. "ORD-1403-1042" or Supabase ID
    val orderSequence: Int = 1,
    val trackingCode: String = "",
    val subscriptionCode: String = "",
    val customerName: String,
    val customerPhone: String,
    val address: String, // Maps to customer_address
    val notes: String = "",
    val latitude: Double = 35.7796,
    val longitude: Double = 51.4058,
    val orderType: String = "PICKUP", // "PICKUP" or "DELIVERY"
    val status: String = "ASSIGNED", // UI status: "ASSIGNED", "COLLECTED_IN_INSPECTION", "DELIVERED_TO_WORKSHOP", "WASHING", "READY_FOR_DELIVERY", "DELIVERED_SETTLED", "RETURNED_TO_CLEAN_WAREHOUSE", "OFFICE_SETTLED"
    val stage: String = "pickup_assigned", // Supabase stage: registered, pickup_assigned, collected, factory_received, ready_for_delivery, out_for_delivery, delivered, returned_to_clean_warehouse, office_settled
    val driverId: String = "DRV-101",
    val driverName: String = "سفیر مسعود بختیاری",
    val carpetsJson: String = "[]",
    val totalArea: Double = 0.0,
    val totalAmount: Long = 0L,
    val depositAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val finalPayable: Long = 0L,
    val remainingAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val paymentMethod: String = "unpaid", // "cash", "pos", "card_to_card", "online", "unpaid"
    val paymentStatus: String = "unpaid", // "paid", "deposit", "unpaid", "office_settled"
    val rackCode: String = "", // Workshop rack code e.g. "A-01"
    val cleanRackCode: String = "", // Clean warehouse rack code e.g. "C-03"
    val returnReason: String = "",
    val customerSignatureUrl: String = "",
    val pickupDate: String = "",
    val deliveryDate: String = "",
    val officeSettled: Boolean = false,
    val routeOrder: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
