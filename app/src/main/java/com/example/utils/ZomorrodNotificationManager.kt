package com.example.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * مدیریت اعلان‌ها، هشدارهای صوتی (آلارم) و لرزش (ویبراتور) برای سفارشات و پیام‌های دیسپچر
 */
object ZomorrodNotificationManager {

    const val CHANNEL_ORDERS_ID = "zomorrod_orders_channel"
    private const val CHANNEL_ORDERS_NAME = "ماموریت‌ها و سفارشات جدید"
    private const val CHANNEL_ORDERS_DESC = "هشدارهای صوتی و ویبره اختصاص سفارش جدید و ماموریت‌ها"

    const val CHANNEL_CHAT_ID = "zomorrod_chat_channel"
    private const val CHANNEL_CHAT_NAME = "پیام‌های دیسپچر و پنل وب"
    private const val CHANNEL_CHAT_DESC = "اعلان‌ها و هشدارهای پیام‌های دریافتی از پنل مرکزی"

    const val CHANNEL_SERVICE_ID = "zomorrod_service_channel"
    private const val CHANNEL_SERVICE_NAME = "سرویس پس‌زمینه راننده"
    private const val CHANNEL_SERVICE_DESC = "فعالیت مداوم برنامه در پس‌زمینه جهت دریافت برخط ماموریت‌ها"

    private var notificationIdCounter = 1000

    private val MISSION_VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 600, 200, 800)
    private val CHAT_VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300, 150, 400)

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build()

                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                // 1. کانال سفارشات و ماموریت‌های جدید با اولویت بالا و ویبره شدید
                val ordersChannel = NotificationChannel(
                    CHANNEL_ORDERS_ID,
                    CHANNEL_ORDERS_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_ORDERS_DESC
                    enableVibration(true)
                    vibrationPattern = MISSION_VIBRATION_PATTERN
                    enableLights(true)
                    setShowBadge(true)
                    setSound(defaultSoundUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                // 2. کانال پیام‌های دیسپچر
                val chatChannel = NotificationChannel(
                    CHANNEL_CHAT_ID,
                    CHANNEL_CHAT_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_CHAT_DESC
                    enableVibration(true)
                    vibrationPattern = CHAT_VIBRATION_PATTERN
                    enableLights(true)
                    setShowBadge(true)
                    setSound(defaultSoundUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                // 3. کانال سرویس پس‌زمینه (کم‌اهمیت و بدون ایجاد مزاحمت مداوم)
                val serviceChannel = NotificationChannel(
                    CHANNEL_SERVICE_ID,
                    CHANNEL_SERVICE_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_SERVICE_DESC
                    enableVibration(false)
                    enableLights(false)
                    setShowBadge(false)
                }

                notificationManager.createNotificationChannels(listOf(ordersChannel, chatChannel, serviceChannel))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var activeRingtone: android.media.Ringtone? = null
    private val soundHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingStopRunnable: Runnable? = null

    /**
     * پخش هشدار صوتی صریح (رینگتون کوتاه). قبلاً از نوع TYPE_ALARM استفاده
     * می‌شد که طبق طراحی اندروید تا وقتی صریحاً stop() نشود ممکن است ادامه
     * پیدا کند یا حلقه‌ای پخش شود (Ringtone قبلی هم هرگز نگه‌داشته و stop
     * نمی‌شد) — همین باعث می‌شد صدای هشدار قطع نشود. حالا:
     *  ۱. از TYPE_NOTIFICATION استفاده می‌کنیم (یک‌بار پخش و توقف طبیعی)
     *  ۲. رینگتون قبلی (اگر هنوز پخش است) قبل از پخش جدید صریحاً stop می‌شود
     *  ۳. یک تایمر ایمنی ۴ ثانیه‌ای هم گذاشته شده تا در بدترین حالت
     *     (رینگتون سفارشی طولانی/حلقه‌ای روی بعضی گوشی‌ها) صدا حتماً قطع شود
     */
    fun playAlarmSound(context: Context, isUrgent: Boolean = true) {
        try {
            stopAlarmSound()

            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
            activeRingtone = ringtone

            val stopRunnable = Runnable { stopAlarmSound() }
            pendingStopRunnable = stopRunnable
            soundHandler.postDelayed(stopRunnable, 4000L)
        } catch (e: Exception) {
            try {
                // Fallback to ToneGenerator if Ringtone fails
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, if (isUrgent) 600 else 300)
            } catch (_: Exception) {}
        }
    }

    /**
     * توقف صریح صدای هشدار در حال پخش (در صورت وجود) و لغو تایمر ایمنی.
     */
    fun stopAlarmSound() {
        try {
            pendingStopRunnable?.let { soundHandler.removeCallbacks(it) }
            pendingStopRunnable = null
            activeRingtone?.let { if (it.isPlaying) it.stop() }
            activeRingtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * فعال‌سازی لرزش ویبراتور دستگاه با الگوی مشخص
     */
    fun triggerVibration(context: Context, pattern: LongArray = MISSION_VIBRATION_PATTERN) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ارسال اعلان ماموریت/سفارش جدید با هشدار صوتی و ویبره
     */
    fun sendNewOrderNotification(
        context: Context,
        orderId: String,
        customerName: String,
        address: String,
        orderType: String = "جمع‌آوری"
    ) {
        try {
            playAlarmSound(context, isUrgent = true)
            triggerVibration(context, MISSION_VIBRATION_PATTERN)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_order_id", orderId)
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                orderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val typeTitle = if (orderType == "DELIVERY" || orderType == "تحویل") "🚚 تحویل فرش" else "🧺 جمع‌آوری فرش"
            val title = "🚨 ماموریت جدید: $typeTitle (#$orderId)"
            val message = "مشتری: $customerName\nآدرس: $address"

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText("سفارش $orderId برای شما اختصاص یافت: $customerName")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(defaultSoundUri)
                .setVibrate(MISSION_VIBRATION_PATTERN)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            if (hasNotificationPermission(context)) {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationIdCounter++, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ارسال اعلان پیام جدید دیسپچر از پنل وب با هشدار صوتی و ویبره
     */
    fun sendNewDispatcherMessageNotification(
        context: Context,
        senderName: String,
        messageText: String
    ) {
        try {
            playAlarmSound(context, isUrgent = false)
            triggerVibration(context, CHAT_VIBRATION_PATTERN)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "💬 پیام جدید از $senderName"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(defaultSoundUri)
                .setVibrate(CHAT_VIBRATION_PATTERN)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (hasNotificationPermission(context)) {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationIdCounter++, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * اعلان تغییر وضعیت سفارش
     */
    fun sendOrderStatusChangeNotification(
        context: Context,
        orderId: String,
        customerName: String,
        newStatusTitle: String
    ) {
        try {
            triggerVibration(context, longArrayOf(0, 200, 100, 200))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_order_id", orderId)
            }

            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                orderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🔔 تغییر وضعیت سفارش #$orderId"
            val message = "وضعیت سفارش $customerName به «$newStatusTitle» تغییر یافت."

            val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (hasNotificationPermission(context)) {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationIdCounter++, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ساخت اعلان ماندگار برای Foreground Service پس‌زمینه به همراه دکمه خروج
     */
    fun createServiceNotification(
        context: Context,
        statusText: String = "سامانه در پس‌زمینه فعال است | آماده دریافت سفارشات و پیام‌ها"
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // اینتنت خروج و متوقف ساختن کامل سرویس پس‌زمینه
        val stopServiceIntent = Intent(context, com.example.data.remote.ZomorrodBackgroundService::class.java).apply {
            action = com.example.data.remote.ZomorrodBackgroundService.ACTION_STOP
        }

        val stopServicePendingIntent = PendingIntent.getService(
            context,
            1102,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${com.example.data.WorkshopNameHolder.current} • سرویس آنلاین پس‌زمینه")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "خروج و توقف سرویس",
                stopServicePendingIntent
            )
            .build()
    }

    /**
     * تست زنده هشدار صوتی و ویبره
     */
    fun testSoundAndVibration(context: Context) {
        playAlarmSound(context, isUrgent = true)
        triggerVibration(context, MISSION_VIBRATION_PATTERN)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔊 تست موفق هشدار صوتی و ویبراتور")
            .setContentText("سیستم هشدار آنلاین در حالت آماده‌باش قرار دارد.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (hasNotificationPermission(context)) {
                with(NotificationManagerCompat.from(context)) {
                    notify(999, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendTestNotification(context: Context) {
        testSoundAndVibration(context)
    }
}
