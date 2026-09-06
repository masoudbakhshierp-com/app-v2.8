package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

object PrinterManager {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null

    private val _connectedPrinter = MutableStateFlow<BluetoothPrinterDevice?>(null)
    val connectedPrinter: StateFlow<BluetoothPrinterDevice?> = _connectedPrinter

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting

    private val _availablePrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val availablePrinters: StateFlow<List<BluetoothPrinterDevice>> = _availablePrinters

    /** فقط دستگاه‌های بلوتوث واقعیِ Paired (جفت‌شده با گوشی) را برمی‌گرداند.
     * قبلاً اگر هیچ پرینتری با اسم Thermal/POS/MTP/BTP پیدا نمی‌شد، سه
     * دستگاه ساختگی با آدرس MAC جعلی به لیست اضافه می‌شد که راننده می‌توانست
     * به آن‌ها «وصل» شود بدون این‌که واقعاً پرینتری وجود داشته باشد. */
    fun scanPrinters(context: Context) {
        val list = mutableListOf<BluetoothPrinterDevice>()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            @SuppressLint("MissingPermission")
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                pairedDevices?.forEach { device ->
                    @SuppressLint("MissingPermission")
                    list.add(BluetoothPrinterDevice(device.name ?: "پرینتر بلوتوثی", device.address))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _availablePrinters.value = list
    }

    /** به یک پرینتر بلوتوثی واقعی وصل می‌شود. اگر بلوتوث خاموش باشد، آدرس
     * نامعتبر باشد، یا اتصال RFCOMM رد شود، false برمی‌گرداند — دیگر هیچ
     * حالت fallbackِ «همیشه موفق» وجود ندارد؛ قبلاً حتی وقتی اتصال واقعی
     * شکست می‌خورد، بعد از یک تأخیر مصنوعی true برگردانده می‌شد و راننده
     * فکر می‌کرد پرینتر وصل شده، در حالی که هیچ سوکتی باز نشده بود. */
    @SuppressLint("MissingPermission")
    suspend fun connectPrinter(device: BluetoothPrinterDevice): Boolean {
        return withContext(Dispatchers.IO) {
            disconnectPrinter()
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    return@withContext false
                }
                if (!BluetoothAdapter.checkBluetoothAddress(device.address)) {
                    return@withContext false
                }
                val realDevice = bluetoothAdapter.getRemoteDevice(device.address)
                bluetoothAdapter.cancelDiscovery()
                val socket = realDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                bluetoothSocket = socket
                _connectedPrinter.value = device.copy(isConnected = true)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                bluetoothSocket = null
                _connectedPrinter.value = null
                false
            }
        }
    }

    fun disconnectPrinter() {
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bluetoothSocket = null
        _connectedPrinter.value = null
    }

    suspend fun printReceipt(
        title: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        address: String,
        carpetDetails: String,
        totalPrice: Long,
        discount: Long,
        finalPrice: Long,
        paymentStatus: String,
        rackCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            _isPrinting.value = true
            var success = false
            try {
                val receiptText = buildEscPosThermalReceiptText(
                    title = title,
                    orderId = orderId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    address = address,
                    carpetItemsSummary = carpetDetails.split("\n").filter { it.isNotBlank() },
                    totalPrice = totalPrice,
                    discount = discount,
                    netPayable = finalPrice,
                    paymentMethod = paymentStatus,
                    rackCode = rackCode,
                    includeTwoCopies = true
                )

                val socket = bluetoothSocket
                if (socket != null && socket.isConnected) {
                    val outputStream: OutputStream = socket.outputStream
                    val initPrinter = byteArrayOf(0x1B, 0x40) // ESC @ (Init)
                    val alignCenter = byteArrayOf(0x1B, 0x61, 0x01) // Center align
                    val selectCodePage = byteArrayOf(0x1B, 0x74, 0x16) // UTF-8
                    val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Cut paper

                    outputStream.write(initPrinter)
                    outputStream.write(alignCenter)
                    outputStream.write(selectCodePage)
                    outputStream.write(receiptText.toByteArray(Charsets.UTF_8))
                    outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
                    outputStream.write(feedAndCut)
                    outputStream.flush()
                    success = true
                } else {
                    // هیچ پرینتر واقعی‌ای وصل نیست — قبلاً اینجا یک تأخیر
                    // مصنوعی می‌گذاشت و success=true برمی‌گرداند، یعنی
                    // راننده فکر می‌کرد فاکتور چاپ شده در حالی که هیچ
                    // چیزی چاپ نشده بود.
                    success = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try { bluetoothSocket?.close() } catch (_: Exception) {}
                bluetoothSocket = null
                _connectedPrinter.value = _connectedPrinter.value?.copy(isConnected = false)
            } finally {
                _isPrinting.value = false
            }
            success
        }
    }

    suspend fun printDailySettlementReport(
        driverName: String,
        date: String,
        settledCount: Int,
        totalCash: Long,
        totalPos: Long,
        totalCardToCard: Long,
        totalAmount: Long,
        orderIds: List<String>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            _isPrinting.value = true
            var success = false
            try {
                val sb = StringBuilder()
                sb.append("===============================\n")
                sb.append("     *** ${com.example.data.WorkshopNameHolder.current} ***\n")
                sb.append("  گزارش تسویه حساب روزانه سفیر\n")
                sb.append("===============================\n")
                sb.append("نام سفیر: $driverName\n")
                sb.append("تاریخ تسویه: $date\n")
                sb.append("ساعت چاپ: ${FarsiUtils.formatCurrentTimeFarsi()}\n")
                sb.append("تعداد فاکتورهای تحویل‌شده: $settledCount سفارش\n")
                sb.append("-------------------------------\n")
                sb.append("دریافتی نقدی: ${FarsiUtils.formatPrice(totalCash)}\n")
                sb.append("دریافتی کارتخوان (POS): ${FarsiUtils.formatPrice(totalPos)}\n")
                if (totalCardToCard > 0) {
                    sb.append("کارت به کارت / آنلاین: ${FarsiUtils.formatPrice(totalCardToCard)}\n")
                }
                sb.append("-------------------------------\n")
                sb.append("جمع کل تسویه امروز: ${FarsiUtils.formatPrice(totalAmount)}\n")
                sb.append("-------------------------------\n")
                sb.append("لیست شماره سفارش‌ها:\n")
                orderIds.forEachIndexed { i, id ->
                    sb.append("${i + 1}. $id\n")
                }
                sb.append("===============================\n")
                sb.append("امضای سفیر:           امضای صندوق:\n\n\n")
                sb.append("................    ...............\n")
                sb.append("سامانه یکپارچه panel.yaselectrical.ir\n")
                sb.append("===============================\n")

                val socket = bluetoothSocket
                if (socket != null && socket.isConnected) {
                    val outputStream = socket.outputStream
                    val initPrinter = byteArrayOf(0x1B, 0x40)
                    val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

                    outputStream.write(initPrinter)
                    outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                    outputStream.write(feedAndCut)
                    outputStream.flush()
                    success = true
                } else {
                    success = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPrinting.value = false
            }
            success
        }
    }

    suspend fun printRawText(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            _isPrinting.value = true
            var success = false
            try {
                val socket = bluetoothSocket
                if (socket != null && socket.isConnected) {
                    val outputStream = socket.outputStream
                    val initPrinter = byteArrayOf(0x1B, 0x40)
                    val feedAndCut = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

                    outputStream.write(initPrinter)
                    outputStream.write(text.toByteArray(Charsets.UTF_8))
                    outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                    outputStream.write(feedAndCut)
                    outputStream.flush()
                    success = true
                } else {
                    success = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPrinting.value = false
            }
            success
        }
    }

    fun buildEscPosThermalReceiptText(
        title: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        address: String,
        carpetItemsSummary: List<String>,
        totalPrice: Long,
        discount: Long,
        netPayable: Long,
        paymentMethod: String,
        rackCode: String,
        includeTwoCopies: Boolean = true
    ): String {
        fun buildSingleCopy(copyTitle: String): String {
            val sb = StringBuilder()
            sb.append("===============================\n")
            sb.append("     *** ${com.example.data.WorkshopNameHolder.current} ***\n")
            sb.append("    $title\n")
            sb.append("     >>>> $copyTitle <<<<\n")
            sb.append("===============================\n")
            sb.append("شماره فاکتور: $orderId\n")
            sb.append("تاریخ و زمان: ${FarsiUtils.formatCurrentTimeFarsi()}\n")
            sb.append("نام مشتری: $customerName\n")
            sb.append("تلفن تماس: $customerPhone\n")
            sb.append("آدرس: $address\n")
            sb.append("-------------------------------\n")
            sb.append("اقلام سفارش (فرش‌ها):\n")
            carpetItemsSummary.forEachIndexed { index, item ->
                sb.append("${index + 1}. $item\n")
            }
            sb.append("-------------------------------\n")
            if (rackCode.isNotEmpty()) {
                sb.append("شماره قفسه انبار: $rackCode\n")
                sb.append("-------------------------------\n")
            }
            sb.append("مبلغ کل فرش‌ها: ${FarsiUtils.formatPrice(totalPrice)}\n")
            if (discount > 0) {
                sb.append("مبلغ تخفیف: ${FarsiUtils.formatPrice(discount)}\n")
            }
            sb.append("مبلغ قابل پرداخت: ${FarsiUtils.formatPrice(netPayable)}\n")
            sb.append("وضعیت تسویه: $paymentMethod\n")
            sb.append("-------------------------------\n")
            sb.append("  [ بارکد / QR کد پیگیری: ORD-$orderId ]\n")
            sb.append("===============================\n")
            sb.append(" امضاء و تایید تحویل‌گیرنده ($copyTitle):\n\n\n")
            sb.append("...............................\n")
            sb.append("سامانه انحصاری panel.yaselectrical.ir\n")
            sb.append("===============================\n")
            return sb.toString()
        }

        return if (includeTwoCopies) {
            buildSingleCopy("نسخه مشتری") +
                    "\n\n- - - - - - - - - - - - - - - -\n" +
                    "      محل برش کاغذ پرینتر      \n" +
                    "- - - - - - - - - - - - - - - -\n\n" +
                    buildSingleCopy("نسخه راننده")
        } else {
            buildSingleCopy("نسخه تک برگ")
        }
    }
}
