package com.example.data.remote

import android.util.Log
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.remote.supabase.ZomorrodSupabaseConfig
import com.example.data.remote.supabase.ZomorrodSupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * سرویس همگام‌سازی و ارتباط با Supabase در سامانه قالیشویی صبا
 * (اتصال واقعی از طریق Edge Functionهای driver-api و otp)
 */
class SupabaseSyncService(
    private var baseUrl: String = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
    private var apiKey: String = ZomorrodSupabaseConfig.DRIVER_API_KEY
) {
    val supabaseManager = ZomorrodSupabaseManager(baseUrl, apiKey)

    fun updateConfig(url: String, key: String = apiKey) {
        this.baseUrl = url.trim().removeSuffix("/")
        this.apiKey = key.trim()
        supabaseManager.updateCredentials(this.baseUrl, this.apiKey)
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun testConnection(targetUrl: String = baseUrl, targetKey: String = apiKey): Pair<Boolean, String> {
        supabaseManager.updateCredentials(targetUrl, targetKey)
        return supabaseManager.checkHealth()
    }

    suspend fun requestOtp(phone: String): Pair<Boolean, String> {
        return supabaseManager.requestOtp(phone)
    }

    suspend fun verifyOtp(phone: String, code: String): Triple<Boolean, String, String> {
        return supabaseManager.verifyOtp(phone, code)
    }

    suspend fun syncTelemetry(driver: DriverEntity): Boolean {
        return supabaseManager.syncDriverStatus(driver)
    }

    suspend fun pushOrderUpdate(order: OrderEntity): Boolean {
        return supabaseManager.upsertOrder(order)
    }

    suspend fun pushCarpetItems(items: List<com.example.data.local.entities.CarpetItemEntity>): Boolean {
        return supabaseManager.upsertCarpetItems(items)
    }

    suspend fun pushDriverSettlement(settlement: DriverSettlementEntity): Boolean {

        return supabaseManager.upsertDriverSettlement(settlement)
    }

    suspend fun sendChatMessage(msg: ChatMessageEntity, driverId: String): Boolean {
        return supabaseManager.sendChatMessage(msg, driverId)
    }

    suspend fun uploadCustomerSignature(orderId: String, signatureBase64: String): String? {
        return supabaseManager.uploadSignature(orderId, signatureBase64)
    }

    suspend fun fetchAssignedOrders(driverId: String = "DRV-101"): List<com.example.data.remote.supabase.SupabaseOrderDto> {
        return supabaseManager.fetchDriverOrders(driverId)
    }

    suspend fun fetchChatMessages(driverId: String = "DRV-101"): List<com.example.data.remote.supabase.SupabaseChatMessageDto> {
        return supabaseManager.fetchChatMessages(driverId)
    }

    suspend fun fetchTariffs(): com.example.data.model.TariffSyncResult {
        return supabaseManager.fetchTariffs()
    }

    suspend fun fetchWorkshopInfo(): com.example.data.remote.supabase.WorkshopInfo? {
        return supabaseManager.fetchWorkshopInfo()
    }
}
