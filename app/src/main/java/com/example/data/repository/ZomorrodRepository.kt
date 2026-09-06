package com.example.data.repository

import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.DriverDao
import com.example.data.local.dao.DriverSettlementDao
import com.example.data.local.dao.GpsLogDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.model.OrderWithItems
import com.example.data.remote.SupabaseSyncService
import com.example.data.remote.supabase.toEntity
import com.example.data.model.TariffSyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class ZomorrodRepository(
    private val orderDao: OrderDao,
    private val chatMessageDao: ChatMessageDao,
    private val gpsLogDao: GpsLogDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val driverDao: DriverDao? = null,
    private val driverSettlementDao: DriverSettlementDao? = null
) {
    constructor(database: com.example.data.local.ZomorrodDatabase) : this(
        orderDao = database.orderDao(),
        chatMessageDao = database.chatMessageDao(),
        gpsLogDao = database.gpsLogDao(),
        syncQueueDao = database.syncQueueDao(),
        driverDao = database.driverDao(),
        driverSettlementDao = database.driverSettlementDao()
    )

    val supabaseService = SupabaseSyncService()

    private val _tariffsState = MutableStateFlow(TariffSyncResult.createDefault())
    val tariffsState: StateFlow<TariffSyncResult> = _tariffsState.asStateFlow()

    val allOrders: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()
    val allChatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    val unsyncedOrdersCount: Flow<Int> = orderDao.getUnsyncedOrdersCount()
    val recentGpsLogs: Flow<List<GpsLogEntity>> = gpsLogDao.getRecentGpsLogs()
    val pendingQueue: Flow<List<SyncQueueEntity>> = syncQueueDao?.getPendingQueue() ?: flowOf(emptyList())
    val pendingQueueCount: Flow<Int> = syncQueueDao?.getPendingCount() ?: flowOf(0)
    val allSettlements: Flow<List<DriverSettlementEntity>> = driverSettlementDao?.getAllSettlementsFlow() ?: flowOf(emptyList())

    suspend fun enqueueSyncAction(
        actionType: String,
        orderId: String,
        title: String,
        payloadJson: String
    ) {
        syncQueueDao?.insertSyncQueueItem(
            SyncQueueEntity(
                actionType = actionType,
                orderId = orderId,
                title = title,
                payloadJson = payloadJson,
                timestamp = System.currentTimeMillis(),
                status = "PENDING"
            )
        )
    }

    suspend fun insertOrder(order: OrderEntity) {
        withContext(Dispatchers.IO) {
            orderDao.insertOrder(order)
            enqueueSyncAction(
                actionType = "ORDER_INSERTED",
                orderId = order.id,
                title = "سفارش جدید ${order.id} برای ${order.customerName}",
                payloadJson = "{\"id\":\"${order.id}\",\"customerName\":\"${order.customerName}\",\"stage\":\"${order.stage}\"}"
            )
        }
    }

    suspend fun getOrderWithItems(orderId: String): OrderWithItems? {
        return withContext(Dispatchers.IO) {
            orderDao.getOrderWithItemsById(orderId)
        }
    }

    fun observeOrderWithItems(orderId: String): Flow<OrderWithItems?> {
        return orderDao.observeOrderWithItemsById(orderId)
    }

    suspend fun addCarpetItemToOrder(orderId: String, item: CarpetItemEntity) {
        withContext(Dispatchers.IO) {
            orderDao.insertCarpetItem(item.copy(orderId = orderId))
            recalculateOrderTotals(orderId)
            enqueueSyncAction(
                actionType = "CARPET_REGISTRATION",
                orderId = orderId,
                title = "ثبت فرش ${item.carpetType} (${item.areaSqMeter} م²) برای سفارش $orderId",
                payloadJson = "{\"carpetType\":\"${item.carpetType}\",\"area\":${item.areaSqMeter},\"price\":${item.totalPrice},\"barcode\":\"${item.barcodeTag}\"}"
            )
        }
    }

    suspend fun removeCarpetItem(itemId: Long, orderId: String) {
        withContext(Dispatchers.IO) {
            orderDao.deleteCarpetItemById(itemId)
            recalculateOrderTotals(orderId)
            enqueueSyncAction(
                actionType = "ITEM_DELETED",
                orderId = orderId,
                title = "حذف آیتم از فاکتور $orderId",
                payloadJson = "{\"itemId\":$itemId}"
            )
        }
    }

    private suspend fun recalculateOrderTotals(orderId: String) {
        val orderWithItems = orderDao.getOrderWithItemsById(orderId) ?: return
        val totalAmount = orderWithItems.items.sumOf { it.totalPrice }
        val totalArea = orderWithItems.items.sumOf { it.areaSqMeter }
        val newStatus = if (orderWithItems.items.isNotEmpty()) {
            "COLLECTED_IN_INSPECTION"
        } else {
            "ASSIGNED"
        }
        val newStage = if (orderWithItems.items.isNotEmpty()) "collected" else "pickup_assigned"
        val updatedOrder = orderWithItems.order.copy(
            totalAmount = totalAmount,
            totalArea = totalArea,
            finalPayable = maxOf(0L, totalAmount - orderWithItems.order.discountAmount),
            status = newStatus,
            stage = newStage,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.updateOrder(updatedOrder)
    }

    suspend fun updateRackAssignment(orderId: String, rackCode: String) {
        withContext(Dispatchers.IO) {
            orderDao.updateRackCode(orderId, rackCode)
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    rackCode = rackCode,
                    status = "DELIVERED_TO_WORKSHOP",
                    stage = "factory_received",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
            }
            enqueueSyncAction(
                actionType = "RACK_ASSIGNMENT",
                orderId = orderId,
                title = "تخصیص قفسه کارگاه $rackCode به سفارش $orderId",
                payloadJson = "{\"rackCode\":\"$rackCode\",\"stage\":\"factory_received\"}"
            )
        }
    }

    suspend fun updateCleanWarehouseReturn(orderId: String, cleanRackCode: String, returnReason: String) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    cleanRackCode = cleanRackCode,
                    returnReason = returnReason,
                    status = "RETURNED_TO_CLEAN_WAREHOUSE",
                    stage = "returned_to_clean_warehouse",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
                enqueueSyncAction(
                    actionType = "RETURNED_TO_CLEAN_WAREHOUSE",
                    orderId = orderId,
                    title = "برگشت به انبار تمیز قفسه $cleanRackCode سفارش $orderId ($returnReason)",
                    payloadJson = "{\"cleanRackCode\":\"$cleanRackCode\",\"returnReason\":\"$returnReason\",\"stage\":\"returned_to_clean_warehouse\"}"
                )
            }
        }
    }

    // وقتی سفارشی که قبلاً به‌خاطر عدم حضور مشتری به انبار تمیز برگشته
    // (RETURNED_TO_CLEAN_WAREHOUSE) دوباره سوار ون می‌شود تا برای تحویل
    // دوباره تلاش شود. با ست‌کردن status/stage روی READY_FOR_DELIVERY و
    // isSynced=false، حلقه‌ی sync معمولی (upsertOrder -> PUT
    // orders/:id/status) آن را دوباره روی سرور هم به ready_for_delivery
    // برمی‌گرداند و دوباره در GET routes/delivery ظاهر می‌شود.
    suspend fun reloadOrderForRedelivery(orderId: String) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    status = "READY_FOR_DELIVERY",
                    stage = "ready_for_delivery",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
                enqueueSyncAction(
                    actionType = "ORDER_STATUS_UPDATE",
                    orderId = orderId,
                    title = "بارگیری مجدد سفارش $orderId جهت تلاش دوباره تحویل",
                    payloadJson = "{\"status\":\"READY_FOR_DELIVERY\",\"stage\":\"ready_for_delivery\"}"
                )
            }
        }
    }

    suspend fun updateCustomerSignature(orderId: String, signatureUrl: String) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    customerSignatureUrl = signatureUrl,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
                enqueueSyncAction(
                    actionType = "SIGNATURE_CAPTURED",
                    orderId = orderId,
                    title = "ثبت امضای دیجیتال سفارش $orderId",
                    payloadJson = "{\"signatureUrl\":\"$signatureUrl\"}"
                )
            }
        }
    }

    suspend fun finalizeSettlement(
        orderId: String,
        paidAmount: Long,
        discountAmount: Long,
        paymentMethod: String
    ) {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                val updated = order.copy(
                    paidAmount = paidAmount,
                    discountAmount = discountAmount,
                    finalPayable = maxOf(0L, order.totalAmount - discountAmount),
                    paymentMethod = paymentMethod,
                    paymentStatus = if (paidAmount >= (order.totalAmount - discountAmount)) "paid" else "deposit",
                    status = "DELIVERED_SETTLED",
                    stage = "delivered",
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updated)
            } else {
                orderDao.updateSettlement(orderId, paidAmount, discountAmount, paymentMethod)
            }

            enqueueSyncAction(
                actionType = "SETTLEMENT_FINALIZED",
                orderId = orderId,
                title = "تسویه حساب سفارش $orderId به مبلغ $paidAmount تومان ($paymentMethod)",
                payloadJson = "{\"paidAmount\":$paidAmount,\"discount\":$discountAmount,\"method\":\"$paymentMethod\",\"stage\":\"delivered\"}"
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        withContext(Dispatchers.IO) {
            val stage = when (status) {
                "ASSIGNED" -> "pickup_assigned"
                "COLLECTED_IN_INSPECTION" -> "collected"
                "DELIVERED_TO_WORKSHOP" -> "factory_received"
                // قبلاً اینجا "factory_received" برمی‌گشت — یعنی این نگاشت محلی با
                // نگاشت رسمی سرور (_shared/orderMapping.ts:
                // mapDriverStatusToStage('WASHING') === 'washing') و با نگاشت
                // دیگرِ خودِ همین اپ (ZomorrodSupabaseManager.driverApiStatusToLocalStage)
                // ناهماهنگ بود. وقتی این مسیر صدا زده می‌شد، ستون stage محلی روی
                // "factory_received" ست می‌شد در حالی که سرور برای همان status
                // "washing" محاسبه می‌کرد.
                "WASHING" -> "washing"
                "READY_FOR_DELIVERY" -> "ready_for_delivery"
                "DELIVERED_SETTLED" -> "delivered"
                "RETURNED_TO_CLEAN_WAREHOUSE" -> "returned_to_clean_warehouse"
                "OFFICE_SETTLED" -> "office_settled"
                else -> "pickup_assigned"
            }
            orderDao.updateOrderStatus(orderId, status)
            val order = orderDao.getOrderWithItemsById(orderId)?.order
            if (order != null) {
                orderDao.updateOrder(order.copy(stage = stage, status = status, isSynced = false, updatedAt = System.currentTimeMillis()))
            }
            enqueueSyncAction(
                actionType = "ORDER_STATUS_UPDATE",
                orderId = orderId,
                title = "تغییر وضعیت سفارش $orderId به $status ($stage)",
                payloadJson = "{\"status\":\"$status\",\"stage\":\"$stage\"}"
            )
        }
    }

    suspend fun saveDriverSettlement(settlement: DriverSettlementEntity) {
        withContext(Dispatchers.IO) {
            driverSettlementDao?.insertSettlement(settlement)
            enqueueSyncAction(
                actionType = "DRIVER_SETTLEMENT_SUBMITTED",
                orderId = settlement.id,
                title = "ثبت بیلان و تسویه حساب روزانه ${settlement.date} به مبلغ ${settlement.totalAmount} تومان",
                payloadJson = "{\"id\":\"${settlement.id}\",\"totalAmount\":${settlement.totalAmount},\"ordersCount\":${settlement.ordersCount}}"
            )
        }
    }

    suspend fun sendChatMessage(orderId: String, messageText: String, driverId: String, sender: String = "DRIVER", voiceUrl: String = "") {
        withContext(Dispatchers.IO) {
            val msg = ChatMessageEntity(
                orderId = orderId,
                sender = sender,
                senderName = if (sender == "DRIVER") "سفیر راننده" else "دیسپچر مرکزی (${com.example.data.WorkshopNameHolder.current})",
                messageText = messageText,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insertMessage(msg)
            supabaseService.sendChatMessage(msg, driverId)
        }
    }

    suspend fun insertChatMessage(msg: ChatMessageEntity) {
        withContext(Dispatchers.IO) {
            chatMessageDao.insertMessage(msg)
        }
    }

    suspend fun logGpsLocation(driverId: String, lat: Double, lng: Double, speedKmh: Float, batteryLevel: Int = 85) {
        withContext(Dispatchers.IO) {
            val log = GpsLogEntity(
                latitude = lat,
                longitude = lng,
                speedKmh = speedKmh,
                timestamp = System.currentTimeMillis()
            )
            gpsLogDao.insertGpsLog(log)
            driverDao?.updateTelemetry(driverId, lat, lng, speedKmh, batteryLevel)
            val driver = driverDao?.getDriverDirect(driverId) ?: DriverEntity(
                id = driverId,
                currentLat = lat,
                currentLng = lng,
                speed = speedKmh,
                batteryLevel = batteryLevel
            )
            supabaseService.syncTelemetry(driver)
        }
    }

    suspend fun archiveSettledOrders() {
        withContext(Dispatchers.IO) {
            orderDao.archiveSettledOrders()
        }
    }

    // این تابع قبلاً به‌صورت مستقل و کپی‌شده هم در syncWithWebPanel هم در
    // performBackgroundSync وجود داشت — با فیلدهای متفاوت/ناقص (مثلاً
    // cleanRackCode اصلاً merge نمی‌شد، یعنی بعد از هر sync، کد قفسه‌ی
    // انبار تمیز محلی که راننده ثبت کرده بود گم می‌شد). حالا یک نسخه‌ی
    // واحد است که هر دو مسیر از آن استفاده می‌کنند.
    private fun mergeRemoteOrderIntoLocal(
        remoteOrder: com.example.data.remote.supabase.SupabaseOrderDto,
        local: OrderEntity
    ): OrderEntity = local.copy(
        customerName = if (remoteOrder.customer_name.isNotBlank()) remoteOrder.customer_name else local.customerName,
        customerPhone = if (remoteOrder.customer_phone.isNotBlank()) remoteOrder.customer_phone else local.customerPhone,
        address = if (remoteOrder.customer_address.isNotBlank()) remoteOrder.customer_address else local.address,
        orderType = if (remoteOrder.order_type.isNotBlank()) remoteOrder.toEntity().orderType else local.orderType,
        status = if (remoteOrder.status.isNotBlank()) remoteOrder.status else local.status,
        stage = if (remoteOrder.stage.isNotBlank()) remoteOrder.stage else local.stage,
        totalAmount = if (remoteOrder.total_amount > 0) remoteOrder.total_amount else local.totalAmount,
        finalPayable = if (remoteOrder.final_payable > 0) remoteOrder.final_payable else local.finalPayable,
        paidAmount = if (remoteOrder.paid_amount > 0) remoteOrder.paid_amount else local.paidAmount,
        paymentStatus = if (remoteOrder.payment_status.isNotBlank()) remoteOrder.payment_status else local.paymentStatus,
        rackCode = if (remoteOrder.rack_code.isNotBlank()) remoteOrder.rack_code else local.rackCode,
        cleanRackCode = if (remoteOrder.cleanRackCode.isNotBlank()) remoteOrder.cleanRackCode else local.cleanRackCode
    )

    suspend fun syncWithWebPanel(
        serverBaseUrl: String = "https://panel.yaselectrical.ir",
        driverId: String = "DRV-101",
        onNewOrder: ((OrderEntity) -> Unit)? = null,
        onNewMessage: ((ChatMessageEntity) -> Unit)? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                supabaseService.updateConfig(serverBaseUrl)

                // 1. Process pending queue from Room database and send to Supabase / panel
                val pendingList = syncQueueDao?.getPendingItemsList() ?: emptyList()
                for (item in pendingList) {
                    syncQueueDao?.markAsSynced(item.id)
                }
                syncQueueDao?.clearSyncedQueue()

                // 2. Sync unsynced orders and their carpet items to Supabase
                val unsynced = orderDao.getUnsyncedOrders()
                for (order in unsynced) {
                    supabaseService.pushOrderUpdate(order)
                    val orderWithItems = orderDao.getOrderWithItemsById(order.id)
                    if (orderWithItems != null && orderWithItems.items.isNotEmpty()) {
                        supabaseService.pushCarpetItems(orderWithItems.items)
                    }
                }
                if (unsynced.isNotEmpty()) {
                    orderDao.markOrdersAsSynced(unsynced.map { it.id })
                }

                // 3. Sync unsynced settlements to Supabase
                val unsyncedSettlements = driverSettlementDao?.getUnsyncedSettlements() ?: emptyList()
                for (stl in unsyncedSettlements) {
                    supabaseService.pushDriverSettlement(stl)
                    driverSettlementDao?.markAsApprovedAndSynced(stl.id)
                }

                // 4. Sync telemetry
                val driver = driverDao?.getDriverDirect(driverId)
                if (driver != null) {
                    supabaseService.syncTelemetry(driver)
                }

                // 5. Fetch updated/new orders assigned to this driver from Supabase panel
                try {
                    val remoteOrders = supabaseService.fetchAssignedOrders(driverId)
                    if (remoteOrders.isNotEmpty()) {
                        for (remoteOrder in remoteOrders) {
                            val localExisting = orderDao.getOrderWithItemsById(remoteOrder.id)
                            if (localExisting == null) {
                                val newEntity = remoteOrder.toEntity()
                                orderDao.insertOrder(newEntity)
                                onNewOrder?.invoke(newEntity)
                            } else {
                                val updated = mergeRemoteOrderIntoLocal(remoteOrder, localExisting.order)
                                orderDao.updateOrder(updated)
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 6. Fetch incoming chat/dispatcher messages
                try {
                    val remoteMessages = supabaseService.fetchChatMessages(driverId)
                    syncRemoteChatMessages(remoteMessages, onNewMessage)
                } catch (_: Exception) {}

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun syncRemoteChatMessages(
        remoteMessages: List<com.example.data.remote.supabase.SupabaseChatMessageDto>,
        onNewMessage: ((ChatMessageEntity) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (remoteMessages.isEmpty()) return@withContext
        val localMessages = chatMessageDao.getAllChatMessagesDirect()

        val knownServerIds = localMessages.mapNotNull { it.serverId.ifBlank { null } }.toSet()

        fun makeSignature(sender: String, text: String, ts: Long): String {
            val roundedSec = ts / 1000L
            return "${sender.uppercase()}_${text.trim()}_$roundedSec"
        }

        val knownSignatures = localMessages.map {
            makeSignature(it.sender, it.messageText, it.timestamp)
        }.toMutableSet()

        val isInitialDbEmpty = localMessages.isEmpty()
        val currentTime = System.currentTimeMillis()

        for (dto in remoteMessages) {
            if (dto.text.isBlank()) continue
            val entity = dto.toEntity()

            val isDuplicateById = entity.serverId.isNotBlank() && knownServerIds.contains(entity.serverId)
            val isDuplicateBySig = knownSignatures.contains(makeSignature(entity.sender, entity.messageText, entity.timestamp))
            val isDuplicateByFuzzy = localMessages.any { local ->
                local.sender.equals(entity.sender, ignoreCase = true) &&
                local.messageText.trim() == entity.messageText.trim() &&
                Math.abs(local.timestamp - entity.timestamp) < 180_000L
            }

            if (!isDuplicateById && !isDuplicateBySig && !isDuplicateByFuzzy) {
                chatMessageDao.insertMessage(entity)
                knownSignatures.add(makeSignature(entity.sender, entity.messageText, entity.timestamp))

                // Only notify if:
                // 1) sender is not DRIVER (message is from dispatcher)
                // 2) local DB was not empty on app launch (prevents historical replay of old messages)
                // 3) message was created recently (within last 10 minutes)
                val isRecent = Math.abs(currentTime - entity.timestamp) < 600_000L
                if (entity.sender != "DRIVER" && !isInitialDbEmpty && isRecent) {
                    onNewMessage?.invoke(entity)
                }
            }
        }
    }

    suspend fun performBackgroundSync(
        driverId: String = "DRV-101",
        onNewOrder: ((OrderEntity) -> Unit)? = null,
        onNewMessage: ((ChatMessageEntity) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Sync telemetry
            val driver = driverDao?.getDriverDirect(driverId)
            if (driver != null) {
                supabaseService.syncTelemetry(driver)
            }

            // 2. Fetch assigned orders from Supabase web panel
            try {
                val remoteOrders = supabaseService.fetchAssignedOrders(driverId)
                if (remoteOrders.isNotEmpty()) {
                    for (remoteOrder in remoteOrders) {
                        val localExisting = orderDao.getOrderWithItemsById(remoteOrder.id)
                        if (localExisting == null) {
                            val newEntity = remoteOrder.toEntity()
                            orderDao.insertOrder(newEntity)
                            onNewOrder?.invoke(newEntity)
                        } else {
                            val updated = mergeRemoteOrderIntoLocal(remoteOrder, localExisting.order)
                            orderDao.updateOrder(updated)
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. Fetch latest tariffs & rates from Supabase web panel
            try {
                fetchAndSyncTariffs()
            } catch (_: Exception) {}

            // 4. Fetch incoming chat / dispatcher messages from Supabase web panel with deduplication
            try {
                val remoteMessages = supabaseService.fetchChatMessages(driverId)
                syncRemoteChatMessages(remoteMessages, onNewMessage)
            } catch (_: Exception) {}

            // 5. Push local unsynced orders
            val unsynced = orderDao.getUnsyncedOrders()
            for (order in unsynced) {
                supabaseService.pushOrderUpdate(order)
                val orderWithItems = orderDao.getOrderWithItemsById(order.id)
                if (orderWithItems != null && orderWithItems.items.isNotEmpty()) {
                    supabaseService.pushCarpetItems(orderWithItems.items)
                }
            }
            if (unsynced.isNotEmpty()) {
                orderDao.markOrdersAsSynced(unsynced.map { it.id })
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchAndSyncTariffs(): TariffSyncResult = withContext(Dispatchers.IO) {
        try {
            val result = supabaseService.fetchTariffs()
            _tariffsState.value = result
            result
        } catch (e: Exception) {
            _tariffsState.value
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            // یک ردیف راننده خام (بدون نام/خودرو واقعی) فقط برای این‌که کلید
            // خارجی محلی (DRV-101) در دیتابیس آفلاین موجود باشد — این ردیف با
            // اولین ورود واقعی (OTP) و همگام‌سازی با پنل بازنویسی می‌شود.
            // هیچ سفارش، فرش، مشتری یا پیام چت نمونه‌ای دیگر ساخته نمی‌شود؛
            // همه‌چیز باید واقعاً از پنل وب ارسال شده باشد.
            if (driverDao?.getDriverDirect("DRV-101") == null) {
                driverDao?.insertOrUpdateDriver(
                    DriverEntity(
                        id = "DRV-101",
                        name = "",
                        phone = "",
                        vehicleType = "",
                        vehiclePlate = "",
                        status = "active",
                        currentLat = 35.7219,
                        currentLng = 51.3347,
                        batteryLevel = 100,
                        speed = 0.0f
                    )
                )
            }
        }
    }
}
