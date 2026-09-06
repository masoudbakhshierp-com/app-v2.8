package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ZomorrodDatabase
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.model.OrderWithItems
import com.example.data.repository.ZomorrodRepository
import com.example.utils.BackupInfo
import com.example.utils.BluetoothPrinterDevice
import com.example.utils.DatabaseBackupManager
import com.example.utils.FarsiUtils
import com.example.utils.NetworkMonitor
import com.example.utils.PrinterManager
import com.example.utils.RealGpsManager
import com.example.utils.ZomorrodNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZomorrodDatabase.getDatabase(application)
    private val repository = ZomorrodRepository(
        orderDao = db.orderDao(),
        chatMessageDao = db.chatMessageDao(),
        gpsLogDao = db.gpsLogDao(),
        syncQueueDao = db.syncQueueDao(),
        driverDao = db.driverDao(),
        driverSettlementDao = db.driverSettlementDao()
    )
    private val networkMonitor = NetworkMonitor(application)
    private val prefs = application.getSharedPreferences("zomorrod_driver_prefs", Context.MODE_PRIVATE)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _savedDriverPhone = MutableStateFlow(prefs.getString("driver_phone", "") ?: "")
    val savedDriverPhone: StateFlow<String> = _savedDriverPhone

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _generatedOtp = MutableStateFlow("")
    val generatedOtp: StateFlow<String> = _generatedOtp

    private val _serverUrl = run {
        val saved = prefs.getString("server_url", null)
        val savedIsKnownInvalid = saved != null &&
            com.example.data.remote.supabase.ZomorrodSupabaseConfig.KNOWN_INVALID_PROJECT_REFS.any { saved.contains(it) }
        val target = if (saved.isNullOrBlank() || savedIsKnownInvalid) {
            com.example.data.remote.supabase.ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL
        } else {
            saved
        }
        if (saved != target) {
            prefs.edit().putString("server_url", target).apply()
        }
        MutableStateFlow(target)
    }
    val serverUrl: StateFlow<String> = _serverUrl

    private val _driverApiKey = run {
        val saved = prefs.getString("driver_api_key", null)
        val target = if (saved.isNullOrBlank() || saved == com.example.data.remote.supabase.ZomorrodSupabaseConfig.KNOWN_INVALID_DRIVER_API_KEY) {
            com.example.data.remote.supabase.ZomorrodSupabaseConfig.DRIVER_API_KEY
        } else {
            saved
        }
        if (saved != target) {
            prefs.edit().putString("driver_api_key", target).apply()
        }
        MutableStateFlow(target)
    }
    val driverApiKey: StateFlow<String> = _driverApiKey

    // نام کارگاه — از همون تنظیماتی که توی پنل وب ثبت می‌شود، تا هرجای اپ
    // که قبلاً یک اسم ثابت نشون داده می‌شد با همون یک منبع هماهنگ بمونه.
    private val _workshopName = MutableStateFlow(prefs.getString("workshop_name", null) ?: "کارگاه").also {
        com.example.data.WorkshopNameHolder.current = it.value
    }
    val workshopName: StateFlow<String> = _workshopName

    // suspend عمداً — تا فراخوان‌ها (مثل چاپ فاکتور/گزارش) بتونن قبل از
    // چاپ منتظر یک تازه‌سازی واقعی از پنل بمونن، نه فقط یک launch جدا که
    // ممکنه دیرتر از خودِ چاپ تموم بشه.
    private suspend fun refreshWorkshopInfo() {
        try {
            val info = repository.supabaseService.fetchWorkshopInfo()
            if (info != null && info.name.isNotBlank()) {
                _workshopName.value = info.name
                com.example.data.WorkshopNameHolder.current = info.name
                prefs.edit().putString("workshop_name", info.name).apply()
            }
        } catch (e: Exception) {
            Log.d("DriverViewModel", "Refresh workshop info failed: ${e.message}")
        }
    }

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult

    fun updateServerUrl(url: String) {
        updateServerConfig(url, _driverApiKey.value)
    }

    fun updateServerConfig(url: String, apiKey: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = apiKey.trim()
        _serverUrl.value = cleanUrl
        _driverApiKey.value = cleanKey
        prefs.edit()
            .putString("server_url", cleanUrl)
            .putString("driver_api_key", cleanKey)
            .apply()
        repository.supabaseService.updateConfig(cleanUrl, cleanKey)
        _syncToastMessage.value = "تنظیمات سرور و کلید راننده ذخیره شد."
    }

    fun testServerConnection(url: String, apiKey: String = _driverApiKey.value) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            val cleanUrl = url.trim().removeSuffix("/")
            val cleanKey = apiKey.trim()
            val result = repository.supabaseService.testConnection(cleanUrl, cleanKey)
            _connectionTestResult.value = result.second
            _isTestingConnection.value = false
        }
    }

    fun requestOtp(phone: String) {
        val cleanPhone = FarsiUtils.toEnglishDigits(phone.trim()).replace(" ", "").replace("-", "")
        if (cleanPhone.length < 10) {
            _authError.value = "لطفاً شماره همراه معتبر (مانند ۰۹۱۲۳۴۵۶۷۸۹) وارد کنید."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            // ارسال درخواست واقعی به سرور
            val (ok, message) = repository.supabaseService.requestOtp(cleanPhone)
            _authLoading.value = false
            if (ok) {
                _otpSent.value = true
                _syncToastMessage.value = message
            } else {
                _authError.value = message
            }
        }
    }

    fun verifyOtp(phone: String, code: String) {
        val cleanCode = FarsiUtils.toEnglishDigits(code.trim())
        if (cleanCode.length < 4) {
            _authError.value = "لطفاً کد تایید حداقل ۴ رقمی را وارد نمایید."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val cleanPhone = FarsiUtils.toEnglishDigits(phone.trim())
            val (ok, resultOrError, driverName) = repository.supabaseService.verifyOtp(cleanPhone, cleanCode)
            _authLoading.value = false
            if (ok) {
                val driverId = resultOrError.ifBlank { "DRV-101" }
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("driver_phone", cleanPhone)
                    .putString("driver_id", driverId)
                    .putString("driver_name", driverName.ifBlank { prefs.getString("driver_name", "") ?: "" })
                    .apply()
                _savedDriverPhone.value = cleanPhone
                _isLoggedIn.value = true
                _otpSent.value = false
                refreshWorkshopInfo()
                _syncToastMessage.value = "خوش آمدید! ورود موفقیت‌آمیز به سامانه ${_workshopName.value}"
            } else {
                _authError.value = resultOrError
            }
        }
    }

    // توجه: تابع quickLogin که قبلاً اینجا بود (ورود سریع بدون تایید واقعی
    // OTP) حذف شد — چون همون‌قدر که کد master سمت سرور خطرناک بود، این
    // میان‌بر هم می‌تونست بدون تایید واقعی کسی رو لاگین کنه. اگه بعداً
    // برای تست دوباره لازم شد، باید صریحاً پشت یه فلگ build نوع debug باشه.

    fun resetOtpState() {
        _otpSent.value = false
        _authError.value = null
    }

    fun logoutDriver() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
        _otpSent.value = false
        _authError.value = null
        _syncToastMessage.value = "از حساب کاربری راننده خارج شدید."
    }

    fun printTestReceipt() {
        viewModelScope.launch {
            val testContent = """
===============================
     *** ${_workshopName.value} ***
    برگه تست سلامت چاپگر حرارتی
       پنل یکپارچه رانندگان
===============================
تاریخ و ساعت: ${FarsiUtils.formatCurrentTimeFarsi()}
سرور فعال: ${serverUrl.value}
وضعیت پرینتر: متصل و آماده به کار (OK)
عرض رول: ۸۰ میلی‌متر حرارتی (POS)
سفیر فعال: راننده سامانه
-------------------------------
✓ تست فونت فارسی: ${_workshopName.value}
✓ تست اعداد و مبالغ: ۱۲,۳۴۵,۰۰۰ ریال
✓ تست جدول و خط‌کشی فاکتور
===============================
[ بارکد تست: DRIVER-PRINTER-OK-2026 ]
===============================
    پایان برگه آزمایش چاپگر
            """.trimIndent()
            val success = PrinterManager.printRawText(testContent)
            if (success) {
                _syncToastMessage.value = "برگه تست با موفقیت به پرینتر ارسال و چاپ شد"
            } else {
                _syncToastMessage.value = "عدم ارتباط با چاپگر! لطفاً اتصال بلوتوث پرینتر را بررسی نمایید."
            }
        }
    }

    val ordersList: StateFlow<List<OrderWithItems>> = repository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChatMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unsyncedCount: StateFlow<Int> = repository.unsyncedOrdersCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val pendingQueueItems: StateFlow<List<SyncQueueEntity>> = repository.pendingQueue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingQueueCount: StateFlow<Int> = repository.pendingQueueCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val gpsLogs: StateFlow<List<GpsLogEntity>> = repository.recentGpsLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSettlements: StateFlow<List<DriverSettlementEntity>> = repository.allSettlements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId

    val selectedOrder: StateFlow<OrderWithItems?> = combine(ordersList, _selectedOrderId) { list, id ->
        if (id == null) list.firstOrNull() else list.find { it.order.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeTab = MutableStateFlow(0) // 0: Missions, 1: Pickup, 2: Delivery, 3: Chat, 4: GPS
    val activeTab: StateFlow<Int> = _activeTab

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isGpsActive = MutableStateFlow(true)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncToastMessage = MutableStateFlow<String?>(null)
    val syncToastMessage: StateFlow<String?> = _syncToastMessage

    private val _showScannerDialog = MutableStateFlow(false)
    val showScannerDialog: StateFlow<Boolean> = _showScannerDialog

    private val _scanStage = MutableStateFlow(com.example.data.model.ScanStage.DELIVERY)
    val scanStage: StateFlow<com.example.data.model.ScanStage> = _scanStage

    val connectedPrinter: StateFlow<BluetoothPrinterDevice?> = PrinterManager.connectedPrinter
    val availablePrinters: StateFlow<List<BluetoothPrinterDevice>> = PrinterManager.availablePrinters
    val isPrinting: StateFlow<Boolean> = PrinterManager.isPrinting

    val tariffsState: StateFlow<com.example.data.model.TariffSyncResult> = repository.tariffsState

    private val _backupInfo = MutableStateFlow<BackupInfo?>(null)
    val backupInfo: StateFlow<BackupInfo?> = _backupInfo

    private val realGpsManager = RealGpsManager(application)

    init {
        refreshBackupInfo()
        viewModelScope.launch { refreshWorkshopInfo() }

        realGpsManager.setLocationCallback { lat, lng, speedKmh ->
            viewModelScope.launch {
                val currentDriverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"
                repository.logGpsLocation(currentDriverId, lat, lng, speedKmh)
            }
        }
        if (_isGpsActive.value) {
            realGpsManager.startTracking()
        }

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            if (networkMonitor.isOnline.value) {
                syncWithWebPanel()
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    syncWithWebPanel()
                }
            }
        }
    }

    fun openScanner(stage: com.example.data.model.ScanStage = com.example.data.model.ScanStage.DELIVERY, targetOrderId: String? = null) {
        if (targetOrderId != null) {
            _selectedOrderId.value = targetOrderId
        }
        _scanStage.value = stage
        _showScannerDialog.value = true
    }

    fun closeScanner() {
        _showScannerDialog.value = false
    }

    fun handleScanSuccess(result: com.example.data.model.ScanVerificationResult.Success) {
        val orderId = result.orderWithItems.order.id
        viewModelScope.launch {
            when (result.scanStage) {
                com.example.data.model.ScanStage.COLLECTION -> {
                    repository.updateOrderStatus(orderId, "COLLECTED_IN_INSPECTION")
                    _syncToastMessage.value = "تطابق جمع‌آوری موفق: سفارش $orderId به عنوان جمع‌آوری شده ثبت شد"
                }
                com.example.data.model.ScanStage.WORKSHOP -> {
                    repository.updateOrderStatus(orderId, "DELIVERED_TO_WORKSHOP")
                    _syncToastMessage.value = "تطابق ورودی انبار موفق: فرش‌های سفارش $orderId تحویل کارگاه گردید"
                }
                com.example.data.model.ScanStage.DELIVERY -> {
                    repository.updateOrderStatus(orderId, "DELIVERED_SETTLED")
                    _syncToastMessage.value = "تطابق تحویل مشتری موفق: فرش‌های سفارش $orderId به مشتری تحویل داده شد"
                }
            }
        }
    }

    fun reportScanMismatchToDispatch(reportText: String) {
        viewModelScope.launch {
            val currentOrder = selectedOrder.value?.order?.id ?: "GENERAL"
            val currentDriverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"
            repository.sendChatMessage(
                orderId = currentOrder,
                messageText = "🚨 " + reportText,
                driverId = currentDriverId,
                sender = "DRIVER"
            )
            _syncToastMessage.value = "هشدار عدم تطابق به مرکز پشتیبانی ارسال گردید"
        }
    }

    fun selectOrder(orderId: String) {
        _selectedOrderId.value = orderId
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun startGpsTracking() {
        if (_isGpsActive.value) {
            val started = realGpsManager.startTracking()
            if (!started) {
                // قبلاً اینجا یک مختصات ثابت و جعلی (۳۵.۷۷۹، ۵۱.۴۰۵) به‌عنوان
                // موقعیت زنده‌ی راننده به سرور فرستاده می‌شد — یعنی اگر GPS
                // به هر دلیلی (نبود مجوز، خاموش بودن لوکیشن گوشی) استارت
                // نمی‌خورد، پنل یک نقطه‌ی ثابت در تهران را به‌جای موقعیت
                // واقعی راننده نشان می‌داد، بدون هیچ هشداری که این موقعیت
                // واقعی نیست.
                _isGpsActive.value = false
                _syncToastMessage.value = "فعال‌سازی GPS ناموفق بود — مطمئن شوید مجوز موقعیت مکانی را داده‌اید و لوکیشن گوشی روشن است."
            }
        }
    }

    fun toggleGpsTracking() {
        _isGpsActive.value = !_isGpsActive.value
        if (_isGpsActive.value) {
            startGpsTracking()
        } else {
            realGpsManager.stopTracking()
        }
    }

    fun addCarpetItem(
        orderId: String,
        carpetType: String,
        lengthMeter: Double,
        widthMeter: Double,
        unitPricePerMeter: Long,
        requestedServices: List<String>,
        defects: List<String>,
        notes: String,
        barcodeTag: String = ""
    ) {
        val area = lengthMeter * widthMeter
        val itemTotalPrice = (area * unitPricePerMeter).toLong()
        val finalTag = if (barcodeTag.isNotBlank()) barcodeTag.trim().uppercase()
        else "ST-${orderId.takeLast(4)}-${(1..99).random().toString().padStart(2, '0')}"

        val item = CarpetItemEntity(
            orderId = orderId,
            carpetType = carpetType,
            lengthMeter = lengthMeter,
            widthMeter = widthMeter,
            areaSqMeter = area,
            unitPricePerMeter = unitPricePerMeter,
            requestedServicesJson = requestedServices.joinToString("، "),
            defectsJson = if (defects.isEmpty()) "بدون عیب" else defects.joinToString("، "),
            totalPrice = itemTotalPrice,
            notes = notes,
            barcodeTag = finalTag
        )
        viewModelScope.launch {
            repository.addCarpetItemToOrder(orderId, item)
        }
    }

    fun refreshTariffs() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.fetchAndSyncTariffs()
            _isSyncing.value = false
            _syncToastMessage.value = if (result.isLiveFromSupabase) {
                "نرخ‌نامه با موفقیت از پنل وب همگام‌سازی شد (${result.carpetTariffs.size} تعرفه فرش)"
            } else {
                "نرخ‌نامه مصوب ${_workshopName.value} بارگذاری شد"
            }
        }
    }

    fun deleteCarpetItem(itemId: Long, orderId: String) {
        viewModelScope.launch {
            repository.removeCarpetItem(itemId, orderId)
        }
    }

    fun finalizeInvoiceRegistration(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "COLLECTED_IN_INSPECTION")
            _syncToastMessage.value = "فاکتور سفارش $orderId ثبت شد و به مرحله تحویل انبار منتقل گردید"
            _activeTab.value = 2
        }
    }

    fun captureCustomerSignature(orderId: String, signatureData: String) {
        viewModelScope.launch {
            repository.updateCustomerSignature(orderId, signatureData)
            _syncToastMessage.value = "امضای دیجیتال مشتری برای سفارش $orderId با موفقیت ذخیره و الصاق گردید"
        }
    }

    fun assignRackCode(orderId: String, rackCode: String) {
        viewModelScope.launch {
            repository.updateRackAssignment(orderId, rackCode)
            _syncToastMessage.value = "شماره قفسه $rackCode برای سفارش $orderId با موفقیت ثبت شد"
        }
    }

    fun confirmWarehouseHandover(orderId: String, rackCode: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.updateRackAssignment(orderId, rackCode)
                repository.updateOrderStatus(orderId, "DELIVERED_TO_WORKSHOP")
                val isSynced = repository.syncWithWebPanel(serverUrl.value)
                _isSyncing.value = false
                if (isSynced) {
                    _syncToastMessage.value = "تحویل به انباردار و قفسه $rackCode در سامانه با موفقیت ثبت شد"
                } else {
                    _syncToastMessage.value = "تحویل قفسه $rackCode در دیتابیس ثبت و در صف همگام‌سازی قرار گرفت"
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در ثبت تحویل به انبار: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun confirmSettlement(orderId: String, paidAmount: Long, discountAmount: Long, paymentMethod: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // ✅ 🔧 تغییر وضعیت سفارش به DELIVERED_SETTLED
                repository.updateOrderStatus(orderId, "DELIVERED_SETTLED")
                
                // ثبت settlement برای گزارش
                val settlement = com.example.data.local.model.DriverSettlementEntity(
                    id = "settlement_${orderId}_${System.currentTimeMillis()}",
                    orderId = orderId,
                    settlementDate = System.currentTimeMillis(),
                    paidAmount = paidAmount,
                    discountAmount = discountAmount,
                    paymentMethod = paymentMethod,
                    syncedAt = null
                )
                repository.saveDriverSettlement(settlement)
                
                // همگام‌سازی با پنل وب
                val isSynced = repository.syncWithWebPanel(serverUrl.value)
                _isSyncing.value = false
                
                if (isSynced) {
                    _syncToastMessage.value = "✅ سفارش $orderId با موفقیت تسویه‌حساب شد"
                } else {
                    _syncToastMessage.value = "تسویه‌حساب در صف همگام‌سازی قرار گرفت"
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در تسویه‌حساب: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun returnToCleanWarehouse(orderId: String, rackCode: String, reason: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.updateCleanWarehouseReturn(orderId, rackCode, reason)
                val isSynced = repository.syncWithWebPanel(serverUrl.value)
                _isSyncing.value = false
                _syncToastMessage.value = "سفارش $orderId به قفسه تمیز انبار ($rackCode) بازگردانده شد و جهت برنامه‌ریزی مجدد به پنل ارسال گردید."
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در ثبت برگشت به انبار: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    // سفارشی که قبلاً به‌خاطر عدم حضور مشتری برگشت داده شده (تب «بارگیری از
    // انبار» → دکمه «بارگیری مجدد جهت تلاش دوباره تحویل») را دوباره در
    // مسیر تحویل قرار می‌دهد.
    fun loadOrderIntoVanForRedelivery(orderId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.reloadOrderForRedelivery(orderId)
                repository.syncWithWebPanel(serverUrl.value)
                _isSyncing.value = false
                _syncToastMessage.value = "سفارش $orderId مجدداً برای تحویل بارگیری شد و به مسیر تحویل بازگشت."
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در بارگیری مجدد سفارش: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun settlePayment(
        orderId: String,
        paidAmount: Long,
        discountAmount: Long,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.finalizeSettlement(orderId, paidAmount, discountAmount, paymentMethod)
                val isSynced = repository.syncWithWebPanel(serverUrl.value)
                _isSyncing.value = false
                if (isSynced) {
                    _syncToastMessage.value = "تسویه حساب سفارش $orderId نهایی و در سرور ثبت شد"
                } else {
                    _syncToastMessage.value = "تسویه حساب سفارش $orderId در دیتابیس ثبت و در صف همگام‌سازی قرار گرفت"
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در ثبت تسویه: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun settleWithOffice(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val settledOrders = ordersList.value.filter { it.order.status == "DELIVERED_SETTLED" }
                val totalCash = settledOrders.filter {
                    it.order.paymentMethod.contains("CASH", true) || it.order.paymentMethod.contains("نقدی")
                }.sumOf { it.order.paidAmount }
                val totalPos = settledOrders.filter {
                    it.order.paymentMethod.contains("POS", true) || it.order.paymentMethod.contains("کارتخوان")
                }.sumOf { it.order.paidAmount }
                val totalAmount = settledOrders.sumOf { it.order.paidAmount }

                val currentDriverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"
                val currentDriverName = prefs.getString("driver_name", "")?.ifBlank { null } ?: currentDriverId
                val settlementEntity = DriverSettlementEntity(
                    id = "STL-${System.currentTimeMillis()}",
                    driverId = currentDriverId,
                    driverName = currentDriverName,
                    date = FarsiUtils.formatCurrentTimeFarsi().split(" ").firstOrNull() ?: "امروز",
                    totalCash = totalCash,
                    totalPos = totalPos,
                    totalAmount = totalAmount,
                    ordersCount = settledOrders.size,
                    orderIdsJson = settledOrders.map { it.order.id }.toString(),
                    status = "pending_approval",
                    notes = "تسویه روزانه توسط راننده ثبت شد"
                )

                repository.saveDriverSettlement(settlementEntity)
                val success = repository.syncWithWebPanel(serverUrl.value, currentDriverId)
                repository.archiveSettledOrders()
                _isSyncing.value = false
                _syncToastMessage.value = "تسویه روزانه با موفقیت در سرور ثبت شد."
                onSuccess()
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncToastMessage.value = "خطا در تسویه با دفتر: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
            }
        }
    }

    fun printDailySettlementReport(
        driverName: String = prefs.getString("driver_name", "")?.ifBlank { null } ?: prefs.getString("driver_id", "راننده") ?: "راننده",
        date: String = "امروز",
        settledOrders: List<OrderWithItems>
    ) {
        viewModelScope.launch {
            refreshWorkshopInfo() // اسم کارگاه رو تازه از پنل بگیر تا گزارش همیشه با تنظیمات فعلی چاپ بشه
            val totalCash = settledOrders.filter {
                it.order.paymentMethod.contains("CASH", true) || it.order.paymentMethod.contains("نقدی")
            }.sumOf { it.order.paidAmount }
            val totalPos = settledOrders.filter {
                it.order.paymentMethod.contains("POS", true) || it.order.paymentMethod.contains("کارتخوان")
            }.sumOf { it.order.paidAmount }
            val totalAmount = settledOrders.sumOf { it.order.paidAmount }

            val printed = PrinterManager.printDailySettlementReport(
                driverName = driverName,
                date = date,
                settledCount = settledOrders.size,
                totalCash = totalCash,
                totalPos = totalPos,
                totalCardToCard = 0L,
                totalAmount = totalAmount,
                orderIds = settledOrders.map { it.order.id }
            )
            _syncToastMessage.value = if (printed) {
                "گزارش تسویه روزانه با موفقیت چاپ شد"
            } else {
                "چاپ گزارش تسویه ناموفق بود! ابتدا از منوی تنظیمات به یک پرینتر متصل شوید."
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val currentOrder = selectedOrder.value?.order?.id ?: "GENERAL"
        val currentDriverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"
        viewModelScope.launch {
            repository.sendChatMessage(currentOrder, text.trim(), driverId = currentDriverId, sender = "DRIVER")
        }
    }

    fun syncWithWebPanel() {
        viewModelScope.launch {
            _isSyncing.value = true
            refreshWorkshopInfo()
            val currentDriverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"
            val success = repository.syncWithWebPanel(
                serverBaseUrl = serverUrl.value,
                driverId = currentDriverId,
                onNewOrder = { order ->
                    ZomorrodNotificationManager.sendNewOrderNotification(
                        context = getApplication(),
                        orderId = order.id,
                        customerName = order.customerName,
                        address = order.address,
                        orderType = order.orderType
                    )
                },
                onNewMessage = { message ->
                    ZomorrodNotificationManager.sendNewDispatcherMessageNotification(
                        context = getApplication(),
                        senderName = message.senderName,
                        messageText = message.messageText
                    )
                }
            )
            _isSyncing.value = false
            if (success) {
                _syncToastMessage.value = "همگام‌سازی با سرور با موفقیت انجام شد"
            } else {
                _syncToastMessage.value = "داده‌ها در پایگاه‌داده Room ثبت و آماده همگام‌سازی مجدد با سرور شدند"
            }
        }
    }

    fun clearToastMessage() {
        _syncToastMessage.value = null
    }

    fun scanBluetoothPrinters(context: Context) {
        PrinterManager.scanPrinters(context)
    }

    fun connectPrinter(device: BluetoothPrinterDevice) {
        viewModelScope.launch {
            val connected = PrinterManager.connectPrinter(device)
            _syncToastMessage.value = if (connected) {
                "به پرینتر ${device.name} متصل شدید"
            } else {
                "اتصال به پرینتر ${device.name} ناموفق بود — مطمئن شوید پرینتر روشن، جفت‌شده (Paired) و در محدوده‌ی بلوتوث است."
            }
        }
    }

    fun printOrderReceipt(
        title: String,
        orderWithItems: OrderWithItems,
        paymentMethod: String = "نقدی / کارتخوان"
    ) {
        viewModelScope.launch {
            refreshWorkshopInfo() // اسم کارگاه رو تازه از پنل بگیر تا فاکتور همیشه با تنظیمات فعلی چاپ بشه
            val order = orderWithItems.order
            val itemsSummary = orderWithItems.items.map {
                "${it.carpetType} (${it.lengthMeter}x${it.widthMeter} م) - ${it.requestedServicesJson} - ${it.totalPrice} تومان"
            }
            val printed = PrinterManager.printReceipt(
                title = title,
                orderId = order.id,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                address = order.address,
                carpetDetails = itemsSummary.joinToString("\n"),
                totalPrice = order.totalAmount,
                discount = order.discountAmount,
                finalPrice = order.totalAmount - order.discountAmount,
                paymentStatus = paymentMethod,
                rackCode = order.rackCode
            )
            _syncToastMessage.value = if (printed) {
                "رسید حرارتی فاکتور ${order.id} با موفقیت چاپ شد"
            } else {
                "چاپ فاکتور ${order.id} ناموفق بود! ابتدا از منوی تنظیمات به یک پرینتر متصل شوید."
            }
        }
    }

    val isBackgroundServiceRunning = com.example.data.remote.ZomorrodBackgroundService.isServiceRunning
    val backgroundLastSyncTime = com.example.data.remote.ZomorrodBackgroundService.lastSyncTimestamp

    fun toggleBackgroundService(context: Context, enable: Boolean) {
        if (enable) {
            com.example.data.remote.ZomorrodBackgroundService.startService(context)
            _syncToastMessage.value = "سرویس پس‌زمینه فعال شد (دریافت لحظه‌ای ماموریت و پیام)"
        } else {
            com.example.data.remote.ZomorrodBackgroundService.stopService(context)
            _syncToastMessage.value = "سرویس پس‌زمینه متوقف گردید"
        }
    }

    fun testAlarmAndVibration(context: Context) {
        ZomorrodNotificationManager.testSoundAndVibration(context)
        _syncToastMessage.value = "هشدار صوتی (آلارم) و ویبره پخش شد"
    }

    fun sendTestNotification(context: Context) {
        testAlarmAndVibration(context)
    }

    fun refreshBackupInfo() {
        _backupInfo.value = DatabaseBackupManager.getBackupInfo(getApplication())
    }

    fun backupDatabase() {
        viewModelScope.launch {
            val (success, msg) = DatabaseBackupManager.createBackup(getApplication(), db)
            _syncToastMessage.value = msg
            refreshBackupInfo()
        }
    }

    fun restoreDatabase() {
        viewModelScope.launch {
            val (success, msg) = DatabaseBackupManager.restoreBackup(getApplication(), db)
            _syncToastMessage.value = msg
            refreshBackupInfo()
        }
    }
}
