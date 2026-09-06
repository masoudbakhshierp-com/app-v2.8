package com.example.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.ZomorrodDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInfo(
    val exists: Boolean,
    val timestamp: String,
    val fileSizeKb: Long,
    val ordersCount: Int,
    val filePath: String
)

object DatabaseBackupManager {

    private const val BACKUP_FILE_NAME = "zomorrod_room_backup.json"
    private const val DB_BAK_FILE_NAME = "zomorrod_driver_db.bak"

    private fun getBackupDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun createBackup(context: Context, database: ZomorrodDatabase): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Fetch all data from DAOs
                val orders = database.orderDao().getAllOrdersDirect()
                val carpetItems = database.orderDao().getAllCarpetItemsDirect()
                val chatMessages = database.chatMessageDao().getAllChatMessagesDirect()
                val gpsLogs = database.gpsLogDao().getAllGpsLogsDirect()
                val syncQueue = database.syncQueueDao().getAllSyncQueueDirect()

                // 2. Build JSON structure
                val rootJson = JSONObject()
                rootJson.put("version", 1)
                rootJson.put("timestamp", System.currentTimeMillis())
                rootJson.put("created_at_str", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                // Orders
                val ordersArray = JSONArray()
                for (o in orders) {
                    val oj = JSONObject().apply {
                        put("id", o.id)
                        put("customerName", o.customerName)
                        put("customerPhone", o.customerPhone)
                        put("address", o.address)
                        put("notes", o.notes)
                        put("latitude", o.latitude)
                        put("longitude", o.longitude)
                        put("orderType", o.orderType)
                        put("status", o.status)
                        put("totalAmount", o.totalAmount)
                        put("paidAmount", o.paidAmount)
                        put("discountAmount", o.discountAmount)
                        put("paymentMethod", o.paymentMethod)
                        put("rackCode", o.rackCode)
                        put("routeOrder", o.routeOrder)
                        put("isSynced", o.isSynced)
                        put("createdAt", o.createdAt)
                        put("updatedAt", o.updatedAt)
                    }
                    ordersArray.put(oj)
                }
                rootJson.put("orders", ordersArray)

                // Carpet Items
                val carpetsArray = JSONArray()
                for (c in carpetItems) {
                    val cj = JSONObject().apply {
                        put("id", c.id)
                        put("orderId", c.orderId)
                        put("carpetType", c.carpetType)
                        put("lengthMeter", c.lengthMeter)
                        put("widthMeter", c.widthMeter)
                        put("areaSqMeter", c.areaSqMeter)
                        put("unitPricePerMeter", c.unitPricePerMeter)
                        put("requestedServicesJson", c.requestedServicesJson)
                        put("defectsJson", c.defectsJson)
                        put("totalPrice", c.totalPrice)
                        put("notes", c.notes)
                        put("barcodeTag", c.barcodeTag)
                    }
                    carpetsArray.put(cj)
                }
                rootJson.put("carpetItems", carpetsArray)

                // Chat Messages
                val chatsArray = JSONArray()
                for (m in chatMessages) {
                    val mj = JSONObject().apply {
                        put("id", m.id)
                        put("orderId", m.orderId)
                        put("sender", m.sender)
                        put("senderName", m.senderName)
                        put("messageText", m.messageText)
                        put("timestamp", m.timestamp)
                        put("isSynced", m.isSynced)
                    }
                    chatsArray.put(mj)
                }
                rootJson.put("chatMessages", chatsArray)

                // GPS Logs
                val gpsArray = JSONArray()
                for (g in gpsLogs) {
                    val gj = JSONObject().apply {
                        put("id", g.id)
                        put("timestamp", g.timestamp)
                        put("latitude", g.latitude)
                        put("longitude", g.longitude)
                        put("speedKmh", g.speedKmh.toDouble())
                        put("isSynced", g.isSynced)
                    }
                    gpsArray.put(gj)
                }
                rootJson.put("gpsLogs", gpsArray)

                // Sync Queue
                val queueArray = JSONArray()
                for (q in syncQueue) {
                    val qj = JSONObject().apply {
                        put("id", q.id)
                        put("actionType", q.actionType)
                        put("orderId", q.orderId)
                        put("title", q.title)
                        put("payloadJson", q.payloadJson)
                        put("timestamp", q.timestamp)
                        put("status", q.status)
                    }
                    queueArray.put(qj)
                }
                rootJson.put("syncQueue", queueArray)

                // 3. Save JSON File
                val backupDir = getBackupDir(context)
                val jsonFile = File(backupDir, BACKUP_FILE_NAME)
                FileOutputStream(jsonFile).use { fos ->
                    fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                }

                // Copy raw db file as secondary backup
                val dbFile = context.getDatabasePath("zomorrod_driver_db")
                if (dbFile.exists()) {
                    val rawBakFile = File(backupDir, DB_BAK_FILE_NAME)
                    copyFile(dbFile, rawBakFile)
                }

                // Copy to Download folder for convenient device transfer
                try {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadDir != null && downloadDir.exists()) {
                        val pubBackupFile = File(downloadDir, BACKUP_FILE_NAME)
                        copyFile(jsonFile, pubBackupFile)
                    }
                } catch (e: Exception) {
                    Log.d("Backup", "Public download copy skipped: ${e.message}")
                }

                val msg = "پشتیبان‌گیری کامل انجام شد (${orders.size} سفارش، ${carpetItems.size} تخته فرش)"
                Pair(true, msg)
            } catch (e: Exception) {
                Log.e("Backup", "Backup error", e)
                Pair(false, "خطا در پشتیبان‌گیری: ${e.localizedMessage}")
            }
        }
    }

    suspend fun restoreBackup(context: Context, database: ZomorrodDatabase): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val backupDir = getBackupDir(context)
                val jsonFile = File(backupDir, BACKUP_FILE_NAME)

                var targetFile = jsonFile
                if (!targetFile.exists()) {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val pubBackupFile = File(downloadDir, BACKUP_FILE_NAME)
                    if (pubBackupFile.exists()) {
                        targetFile = pubBackupFile
                    } else {
                        return@withContext Pair(false, "هیچ فایل پشتیبانی یافت نشد. ابتدا پشتیبان‌گیری ایجاد کنید.")
                    }
                }

                val jsonContent = FileInputStream(targetFile).bufferedReader().use { it.readText() }
                val rootJson = JSONObject(jsonContent)

                // Restore Orders
                val ordersArray = rootJson.optJSONArray("orders") ?: JSONArray()
                val restoredOrders = mutableListOf<OrderEntity>()
                for (i in 0 until ordersArray.length()) {
                    val oj = ordersArray.getJSONObject(i)
                    restoredOrders.add(
                        OrderEntity(
                            id = oj.getString("id"),
                            customerName = oj.optString("customerName", ""),
                            customerPhone = oj.optString("customerPhone", ""),
                            address = oj.optString("address", ""),
                            notes = oj.optString("notes", ""),
                            latitude = oj.optDouble("latitude", 35.779),
                            longitude = oj.optDouble("longitude", 51.405),
                            orderType = oj.optString("orderType", "PICKUP"),
                            status = oj.optString("status", "ASSIGNED"),
                            totalAmount = oj.optLong("totalAmount", 0L),
                            paidAmount = oj.optLong("paidAmount", 0L),
                            discountAmount = oj.optLong("discountAmount", 0L),
                            paymentMethod = oj.optString("paymentMethod", ""),
                            rackCode = oj.optString("rackCode", ""),
                            routeOrder = oj.optInt("routeOrder", 1),
                            isSynced = oj.optBoolean("isSynced", true),
                            createdAt = oj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = oj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (restoredOrders.isNotEmpty()) {
                    database.orderDao().insertOrders(restoredOrders)
                }

                // Restore Carpets
                val carpetsArray = rootJson.optJSONArray("carpetItems") ?: JSONArray()
                val restoredCarpets = mutableListOf<CarpetItemEntity>()
                for (i in 0 until carpetsArray.length()) {
                    val cj = carpetsArray.getJSONObject(i)
                    restoredCarpets.add(
                        CarpetItemEntity(
                            id = cj.optLong("id", 0L),
                            orderId = cj.getString("orderId"),
                            carpetType = cj.optString("carpetType", "ماشینی"),
                            lengthMeter = cj.optDouble("lengthMeter", 4.0),
                            widthMeter = cj.optDouble("widthMeter", 3.0),
                            areaSqMeter = cj.optDouble("areaSqMeter", 12.0),
                            unitPricePerMeter = cj.optLong("unitPricePerMeter", 50000L),
                            requestedServicesJson = cj.optString("requestedServicesJson", "اعلاشویی"),
                            defectsJson = cj.optString("defectsJson", "بدون عیب"),
                            totalPrice = cj.optLong("totalPrice", 600000L),
                            notes = cj.optString("notes", ""),
                            barcodeTag = cj.optString("barcodeTag", "")
                        )
                    )
                }
                if (restoredCarpets.isNotEmpty()) {
                    database.orderDao().insertCarpetItems(restoredCarpets)
                }

                // Restore Chat Messages
                val chatsArray = rootJson.optJSONArray("chatMessages") ?: JSONArray()
                val restoredChats = mutableListOf<ChatMessageEntity>()
                for (i in 0 until chatsArray.length()) {
                    val mj = chatsArray.getJSONObject(i)
                    restoredChats.add(
                        ChatMessageEntity(
                            id = mj.optLong("id", 0L),
                            orderId = mj.optString("orderId", "GENERAL"),
                            sender = mj.optString("sender", "DISPATCHER"),
                            senderName = mj.optString("senderName", "امور مشتریان"),
                            messageText = mj.optString("messageText", ""),
                            timestamp = mj.optLong("timestamp", System.currentTimeMillis()),
                            isSynced = mj.optBoolean("isSynced", true)
                        )
                    )
                }
                if (restoredChats.isNotEmpty()) {
                    database.chatMessageDao().insertMessages(restoredChats)
                }

                // Restore GPS Logs
                val gpsArray = rootJson.optJSONArray("gpsLogs") ?: JSONArray()
                val restoredGps = mutableListOf<GpsLogEntity>()
                for (i in 0 until gpsArray.length()) {
                    val gj = gpsArray.getJSONObject(i)
                    restoredGps.add(
                        GpsLogEntity(
                            id = gj.optLong("id", 0L),
                            timestamp = gj.optLong("timestamp", System.currentTimeMillis()),
                            latitude = gj.optDouble("latitude", 35.779),
                            longitude = gj.optDouble("longitude", 51.405),
                            speedKmh = gj.optDouble("speedKmh", 0.0).toFloat(),
                            isSynced = gj.optBoolean("isSynced", true)
                        )
                    )
                }
                if (restoredGps.isNotEmpty()) {
                    database.gpsLogDao().insertGpsLogs(restoredGps)
                }

                // Restore Sync Queue
                val queueArray = rootJson.optJSONArray("syncQueue") ?: JSONArray()
                val restoredQueue = mutableListOf<SyncQueueEntity>()
                for (i in 0 until queueArray.length()) {
                    val qj = queueArray.getJSONObject(i)
                    restoredQueue.add(
                        SyncQueueEntity(
                            id = qj.optLong("id", 0L),
                            actionType = qj.optString("actionType", ""),
                            orderId = qj.optString("orderId", ""),
                            title = qj.optString("title", ""),
                            payloadJson = qj.optString("payloadJson", "{}"),
                            timestamp = qj.optLong("timestamp", System.currentTimeMillis()),
                            status = qj.optString("status", "PENDING")
                        )
                    )
                }
                if (restoredQueue.isNotEmpty()) {
                    database.syncQueueDao().insertSyncQueueItems(restoredQueue)
                }

                val msg = "بازیابی دیتابیس با موفقیت انجام شد (${restoredOrders.size} سفارش و ${restoredCarpets.size} آیتم بازیابی شد)"
                Pair(true, msg)
            } catch (e: Exception) {
                Log.e("Backup", "Restore error", e)
                Pair(false, "خطا در بازیابی دیتابیس: ${e.localizedMessage}")
            }
        }
    }

    fun getBackupInfo(context: Context): BackupInfo {
        val backupDir = getBackupDir(context)
        val jsonFile = File(backupDir, BACKUP_FILE_NAME)
        if (!jsonFile.exists()) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pubBackupFile = File(downloadDir, BACKUP_FILE_NAME)
            if (pubBackupFile.exists()) {
                val lastMod = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(pubBackupFile.lastModified()))
                val sizeKb = pubBackupFile.length() / 1024
                return BackupInfo(true, lastMod, sizeKb, 0, pubBackupFile.absolutePath)
            }
            return BackupInfo(false, "-", 0L, 0, "")
        }

        val lastMod = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(jsonFile.lastModified()))
        val sizeKb = jsonFile.length() / 1024

        var orderCount = 0
        try {
            val text = jsonFile.readText()
            val root = JSONObject(text)
            orderCount = root.optJSONArray("orders")?.length() ?: 0
        } catch (e: Exception) {
            // ignore
        }

        return BackupInfo(true, lastMod, sizeKb, orderCount, jsonFile.absolutePath)
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inStream.read(buffer).also { bytesRead = it } > 0) {
                    outStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }
}
