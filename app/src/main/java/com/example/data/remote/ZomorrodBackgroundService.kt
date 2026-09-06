package com.example.data.remote

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.ZomorrodDatabase
import com.example.data.repository.ZomorrodRepository
import com.example.utils.FarsiUtils
import com.example.utils.ZomorrodNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سرویس مداوم در پس‌زمینه (Foreground Service) جهت دریافت لحظه‌ای ماموریت‌ها،
 * سفارشات جدید و پیام‌های دیسپچر از پنل وب قالیشویی صبا به همراه هشدار صوتی و ویبره.
 */
class ZomorrodBackgroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var syncLoopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var repository: ZomorrodRepository

    companion object {
        private const val TAG = "ZomorrodBgService"
        const val NOTIFICATION_ID = 1100
        const val ACTION_START = "com.example.action.START_BACKGROUND_SYNC"
        const val ACTION_STOP = "com.example.action.STOP_BACKGROUND_SYNC"
        const val ACTION_FORCE_SYNC = "com.example.action.FORCE_SYNC"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private val _lastSyncTimestamp = MutableStateFlow<String?>(null)
        val lastSyncTimestamp: StateFlow<String?> = _lastSyncTimestamp

        fun startService(context: Context) {
            try {
                val intent = Intent(context, ZomorrodBackgroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background service", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ZomorrodBackgroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping background service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            ZomorrodNotificationManager.createNotificationChannel(this)
            val db = ZomorrodDatabase.getDatabase(this)
            repository = ZomorrodRepository(db)

            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Zomorrod:BackgroundSyncWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_FORCE_SYNC -> {
                triggerSingleSync()
            }
            else -> {
                startForegroundActiveService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundActiveService() {
        _isServiceRunning.value = true

        val notification = ZomorrodNotificationManager.createServiceNotification(
            this,
            "سفیر آنلاین: آماده دریافت ماموریت و پیام‌های دیسپچینگ"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    serviceType
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        serviceType
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (inner: Exception) {
                Log.e(TAG, "Fallback startForeground failed", inner)
            }
        }

        startPeriodicSyncLoop()
    }

    private fun startPeriodicSyncLoop() {
        syncLoopJob?.cancel()
        syncLoopJob = serviceScope.launch {
            while (isActive) {
                try {
                    wakeLock?.acquire(3000L) // 3 seconds partial lock for network fetch
                    performSyncCycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Sync cycle exception", e)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (_: Exception) {}
                }

                // Poll every 15 seconds for incoming web panel missions and dispatcher messages
                delay(15_000L)
            }
        }
    }

    private fun triggerSingleSync() {
        serviceScope.launch {
            try {
                performSyncCycle()
            } catch (e: Exception) {
                Log.e(TAG, "Manual sync error", e)
            }
        }
    }

    private suspend fun performSyncCycle() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        val farsiTime = FarsiUtils.toFarsiDigits(currentTime)

        val prefs = applicationContext.getSharedPreferences("zomorrod_driver_prefs", Context.MODE_PRIVATE)
        val driverId = prefs.getString("driver_id", "DRV-101") ?: "DRV-101"

        repository.performBackgroundSync(
            driverId = driverId,
            onNewOrder = { order ->
                Log.d(TAG, "New mission received from web panel: ${order.id}")
                ZomorrodNotificationManager.sendNewOrderNotification(
                    context = applicationContext,
                    orderId = order.id,
                    customerName = order.customerName,
                    address = order.address,
                    orderType = order.orderType
                )
            },
            onNewMessage = { message ->
                Log.d(TAG, "New dispatcher message received from web panel: ${message.messageText}")
                ZomorrodNotificationManager.sendNewDispatcherMessageNotification(
                    context = applicationContext,
                    senderName = message.senderName,
                    messageText = message.messageText
                )
            }
        )

        _lastSyncTimestamp.value = farsiTime
    }

    private fun stopForegroundService() {
        _isServiceRunning.value = false
        syncLoopJob?.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing foreground notification", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        syncLoopJob?.cancel()
        serviceJob.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
