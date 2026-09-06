package com.example.data.model

import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.model.OrderWithItems

enum class ScanStage(val titleFarsi: String) {
    COLLECTION("جمع‌آوری در محل"),
    WORKSHOP("ورود به انبار / تخلیه"),
    DELIVERY("تحویل به مشتری")
}

sealed class ScanVerificationResult {
    data class Success(
        val scannedCode: String,
        val orderWithItems: OrderWithItems,
        val carpetItem: CarpetItemEntity?,
        val scanStage: ScanStage,
        val message: String
    ) : ScanVerificationResult()

    data class Mismatch(
        val scannedCode: String,
        val expectedOrderId: String,
        val targetOrderWithItems: OrderWithItems?,
        val actualOrderWithItems: OrderWithItems?,
        val actualCarpetItem: CarpetItemEntity?,
        val scanStage: ScanStage,
        val warningTitle: String,
        val warningMessage: String
    ) : ScanVerificationResult()

    data class NotFound(
        val scannedCode: String,
        val message: String
    ) : ScanVerificationResult()
}
