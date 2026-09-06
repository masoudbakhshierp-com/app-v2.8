package com.example.data.remote.supabase

import com.example.data.WorkshopNameHolder
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity

/**
 * مدل داده‌ای اطلاعات کارگاه و دفتر مرکزی
 */
data class WorkshopInfo(
    val name: String,
    val address: String = "",
    val phone: String = "",
    val lat: Double = 35.7219,
    val lng: Double = 51.3347
)

/**
 * مدل داده‌ای سفیران / رانندگان (جدول drivers در Supabase)
 */
data class SupabaseDriverDto(
    val id: String,
    val name: String,
    val phone: String,
    val vehicle_type: String = "وانت پیکان",
    val vehicle_plate: String = "ایران ۱۱ - ۱۲۳ ج ۴۵",
    val status: String = "ACTIVE",
    val current_lat: Double = 35.779,
    val current_lng: Double = 51.405,
    val battery_level: Int = 100,
    val speed: Double = 0.0,
    val app_status: String = "active",
    val total_collected_cash: Long = 0L,
    val total_collected_pos: Long = 0L,
    val last_online_at: String? = null
)

/**
 * مدل داده‌ای سفارشات (جدول orders در Supabase)
 */
data class SupabaseOrderDto(
    val id: String,
    val tracking_code: String,
    val customer_name: String,
    val customer_phone: String,
    val customer_address: String,
    val lat: Double,
    val lng: Double,
    val stage: String,
    val status: String,
    val order_type: String,
    val driver_id: String,
    val driver_name: String,
    val total_amount: Long,
    val discount_amount: Long,
    val final_payable: Long,
    val paid_amount: Long,
    val payment_method: String,
    val payment_status: String,
    val rack_code: String,
    val clean_rack_code: String,
    val return_reason: String,
    val customer_signature_url: String,
    val notes: String,
    val updated_at: String? = null
)

/**
 * مدل داده‌ای تسویه حساب روزانه راننده با دفتر (جدول driver_settlements در Supabase)
 */
data class SupabaseDriverSettlementDto(
    val id: String,
    val driver_id: String,
    val driver_name: String,
    val date: String,
    val total_cash: Long,
    val total_pos: Long,
    val total_card_to_card: Long,
    val total_online: Long,
    val total_amount: Long,
    val orders_count: Int,
    val returned_orders_count: Int,
    val status: String,
    val notes: String
)

/**
 * مدل داده‌ای پیام‌های چت پشتیبانی و دیسپچر (جدول chat_messages در Supabase)
 */
data class SupabaseChatMessageDto(
    val id: String,
    val driver_id: String,
    val sender: String,
    val sender_name: String,
    val text: String,
    val timestamp: String
)

/**
 * توابع تبدیل و مپینگ بین موجودیت‌های محلی Room و مدل‌های سرور Supabase
 */
fun DriverEntity.toSupabaseDto(): SupabaseDriverDto {
    return SupabaseDriverDto(
        id = this.id,
        name = this.name,
        phone = this.phone,
        vehicle_type = this.vehicleType,
        vehicle_plate = this.vehiclePlate,
        status = this.status,
        current_lat = this.currentLat,
        current_lng = this.currentLng,
        battery_level = this.batteryLevel,
        speed = this.speed.toDouble(),
        app_status = "active",
        total_collected_cash = this.totalCollectedCash,
        total_collected_pos = this.totalCollectedPos
    )
}

fun OrderEntity.toSupabaseDto(): SupabaseOrderDto {
    val payable = (this.totalAmount - this.discountAmount).coerceAtLeast(0L)
    val payStatus = if (this.paidAmount >= payable && payable > 0) "paid"
    else if (this.paidAmount > 0) "deposit"
    else "unpaid"

    return SupabaseOrderDto(
        id = this.id,
        tracking_code = if (this.trackingCode.isNotBlank()) this.trackingCode else this.id,
        customer_name = this.customerName,
        customer_phone = this.customerPhone,
        customer_address = this.address,
        lat = this.latitude,
        lng = this.longitude,
        stage = this.stage,
        status = this.status,
        order_type = this.orderType,
        driver_id = this.driverId,
        driver_name = this.driverName,
        total_amount = this.totalAmount,
        discount_amount = this.discountAmount,
        final_payable = payable,
        paid_amount = this.paidAmount,
        payment_method = this.paymentMethod,
        payment_status = payStatus,
        rack_code = this.rackCode,
        clean_rack_code = this.cleanRackCode,
        return_reason = this.returnReason,
        customer_signature_url = this.customerSignatureUrl,
        notes = this.notes
    )
}

fun DriverSettlementEntity.toSupabaseDto(): SupabaseDriverSettlementDto {
    return SupabaseDriverSettlementDto(
        id = this.id,
        driver_id = this.driverId,
        driver_name = this.driverName,
        date = this.date,
        total_cash = this.totalCash,
        total_pos = this.totalPos,
        total_card_to_card = this.totalCardToCard,
        total_online = this.totalOnline,
        total_amount = this.totalAmount,
        orders_count = this.ordersCount,
        returned_orders_count = this.returnedOrdersCount,
        status = this.status,
        notes = this.notes
    )
}

fun ChatMessageEntity.toSupabaseDto(): SupabaseChatMessageDto {
    return SupabaseChatMessageDto(
        id = "MSG-${this.id}-${this.timestamp}",
        driver_id = this.orderId,
        sender = if (this.sender == "DRIVER") "driver" else "operator",
        sender_name = this.senderName,
        text = this.messageText,
        timestamp = this.timestamp.toString()
    )
}

/**
 * مدل داده‌ای اقلام فرش (جدول carpet_items در Supabase)
 */
data class SupabaseCarpetItemDto(
    val id: Long,
    val order_id: String,
    val carpet_type: String,
    val length_meter: Double,
    val width_meter: Double,
    val area_sq_meter: Double,
    val unit_price: Long,
    val requested_services: String,
    val defects: String,
    val total_price: Long,
    val notes: String = "",
    val barcode_tag: String = ""
)

fun com.example.data.local.entities.CarpetItemEntity.toSupabaseDto(): SupabaseCarpetItemDto {
    return SupabaseCarpetItemDto(
        id = this.id,
        order_id = this.orderId,
        carpet_type = this.carpetType,
        length_meter = this.lengthMeter,
        width_meter = this.widthMeter,
        area_sq_meter = this.areaSqMeter,
        unit_price = this.unitPricePerMeter,
        requested_services = this.requestedServicesJson,
        defects = this.defectsJson,
        total_price = this.totalPrice,
        notes = this.notes,
        barcode_tag = this.barcodeTag
    )
}

fun SupabaseOrderDto.toEntity(): OrderEntity {
    val normType = when (this.order_type.uppercase()) {
        "DELIVERY", "تحویل" -> "DELIVERY"
        "COLLECTION", "PICKUP", "جمع‌آوری" -> "PICKUP"
        else -> if (this.order_type.isNotBlank()) this.order_type.uppercase() else "PICKUP"
    }
    return OrderEntity(
        id = this.id,
        orderSequence = 1,
        trackingCode = if (this.tracking_code.isNotBlank()) this.tracking_code else this.id,
        subscriptionCode = "SUB-${this.id.takeLast(4)}",
        customerName = this.customer_name.ifBlank { "مشتری ${WorkshopNameHolder.current}" },
        customerPhone = this.customer_phone,
        address = this.customer_address.ifBlank { "تهران" },
        notes = this.notes,
        latitude = if (this.lat != 0.0) this.lat else 35.7796,
        longitude = if (this.lng != 0.0) this.lng else 51.4058,
        orderType = normType,
        status = this.status.ifBlank { "ASSIGNED" },
        stage = this.stage.ifBlank { "pickup_assigned" },
        totalAmount = this.total_amount,
        discountAmount = this.discount_amount,
        paidAmount = this.paid_amount,
        finalPayable = if (this.final_payable > 0) this.final_payable else (this.total_amount - this.discount_amount).coerceAtLeast(0L),
        paymentMethod = this.payment_method.ifBlank { "unpaid" },
        paymentStatus = this.payment_status.ifBlank { "unpaid" },
        driverId = this.driver_id.ifBlank { "DRV-101" },
        driverName = this.driver_name.ifBlank { "سفیر مسعود بختیاری" },
        rackCode = this.rack_code,
        cleanRackCode = this.clean_rack_code,
        returnReason = this.return_reason,
        customerSignatureUrl = this.customer_signature_url,
        isSynced = true,
        updatedAt = System.currentTimeMillis()
    )
}

fun parseTimestampToEpochMillis(rawTimestamp: String?): Long {
    if (rawTimestamp.isNullOrBlank()) return System.currentTimeMillis()
    val clean = rawTimestamp.trim()

    // 1. Check if numeric epoch timestamp (seconds or millis)
    clean.toLongOrNull()?.let { num ->
        return if (num < 10_000_000_000L) num * 1000L else num
    }

    // 2. Try parsing ISO-8601 with java.time if available (Android 8.0+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        try {
            return java.time.Instant.parse(clean).toEpochMilli()
        } catch (_: Exception) {}
        try {
            return java.time.OffsetDateTime.parse(clean).toInstant().toEpochMilli()
        } catch (_: Exception) {}
        try {
            return java.time.LocalDateTime.parse(clean).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {}
    }

    // 3. Fallback SimpleDateFormat patterns
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss"
    )
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(clean)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }

    return System.currentTimeMillis()
}

fun SupabaseChatMessageDto.toEntity(): ChatMessageEntity {
    val ts = parseTimestampToEpochMillis(this.timestamp)
    return ChatMessageEntity(
        id = 0L,
        serverId = this.id.trim(),
        orderId = if (this.driver_id.isNotBlank()) this.driver_id else "GENERAL",
        sender = if (this.sender.equals("driver", ignoreCase = true) || this.sender.equals("DRIVER", ignoreCase = true)) "DRIVER" else "DISPATCHER",
        senderName = if (this.sender_name.isNotBlank()) this.sender_name else "دیسپچینگ ${WorkshopNameHolder.current}",
        messageText = this.text.trim(),
        timestamp = ts,
        isSynced = true
    )
}


