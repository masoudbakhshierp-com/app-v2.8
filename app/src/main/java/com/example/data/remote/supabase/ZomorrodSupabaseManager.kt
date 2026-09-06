package com.example.data.remote.supabase

import android.util.Log
import com.example.data.WorkshopNameHolder
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.model.CarpetTariffItem
import com.example.data.model.DefectTariffItem
import com.example.data.model.ServiceTariffItem
import com.example.data.model.TariffSyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * مدیر ارتباطی و همگام‌ساز واقعی با Supabase برای اپلیکیشن راننده.
 *
 * طبق قوانین ثابت معماری:
 * ۱. هر عملیات شبکه فقط یک endpoint واقعی و مشخص را صدا می‌زند — هیچ آدرس حدسی مجاز نیست.
 * ۲. هیچ نام کارگاه/شرکتی هاردکد نمی‌شود و از /driver-api/workshop دریافت می‌گردد.
 */
class ZomorrodSupabaseManager(
    private var supabaseUrl: String = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
    private var driverApiKey: String = ZomorrodSupabaseConfig.DRIVER_API_KEY
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun functionsBase(): String = "${supabaseUrl.trim().removeSuffix("/")}/functions/v1"

    fun updateCredentials(url: String, key: String = driverApiKey) {
        this.supabaseUrl = url.trim().removeSuffix("/")
        if (key.isNotBlank()) this.driverApiKey = key.trim()
    }

    private fun baseRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("x-driver-api-key", driverApiKey)
            .addHeader("x-api-key", driverApiKey)
            .addHeader("apikey", driverApiKey)
            .addHeader("Authorization", "Bearer $driverApiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
    }

    // ==========================================================================
    // سلامت اتصال
    // GET /driver-api/health
    // ==========================================================================

    suspend fun checkHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val endpoint = "${functionsBase()}/driver-api/health"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Pair(true, "ارتباط موفق با سرور ($supabaseUrl) | تأخیر: ${duration}ms")
                } else {
                    Pair(false, "پاسخ نامعتبر از سرور (HTTP ${response.code} | تأخیر: ${duration}ms)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "عدم برقراری ارتباط با $supabaseUrl: ${e.localizedMessage ?: "Timeout"}")
        }
    }

    // ==========================================================================
    // موقعیت زنده راننده (GPS)
    // POST /driver-api/driver/location
    // ==========================================================================

    suspend fun syncDriverStatus(driver: DriverEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("driverId", driver.id)
                put("latitude", driver.currentLat)
                put("longitude", driver.currentLng)
                put("speed", driver.speed)
                put("batteryLevel", driver.batteryLevel)
                put("status", driver.status)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val endpoint = "${functionsBase()}/driver-api/driver/location"
            val request = baseRequest(endpoint)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Location sync failed: ${e.message}")
            false
        }
    }

    // ==========================================================================
    // اطلاعات کارگاه (نام، آدرس، موقعیت، تلفن)
    // GET /driver-api/workshop
    // ==========================================================================

    suspend fun fetchWorkshopInfo(): WorkshopInfo? = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${functionsBase()}/driver-api/workshop"
            val request = baseRequest(endpoint).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string()?.trim() ?: return@withContext null
                val root = JSONObject(body)
                // پاسخ واقعی سرور اینه: {"success": true, "workshop": {"name": ...}}
                // یعنی همه‌چیز داخل یک آبجکت تودرتوی "workshop" است، نه در ریشه.
                // قبلاً این تابع مستقیم دنبال "name" در ریشه‌ی JSON می‌گشت، پس
                // همیشه خالی برمی‌گشت و اسم کارگاه هیچ‌وقت از پنل به‌روزرسانی
                // نمی‌شد (همیشه مقدار پیش‌فرض کش‌شده باقی می‌ماند). حالا اول
                // آبجکت تودرتو را می‌خوانیم، و در نبودش (سازگاری با شکل‌های
                // احتمالی دیگر) به ریشه هم رجوع می‌کنیم.
                val json = root.optJSONObject("workshop") ?: root
                val name = json.optString("name").ifBlank {
                    json.optString("workshop_name").ifBlank {
                        json.optString("title", "")
                    }
                }.trim()
                val address = json.optString("address").ifBlank { json.optString("workshop_address", "") }.trim()
                // ستون واقعی روی سرور "phones" (آرایه) است، نه یک رشته‌ی
                // "phone"/"telephone" — اولین شماره‌ی آرایه را برمی‌داریم.
                val phone = json.optString("phone").ifBlank {
                    json.optString("telephone").ifBlank {
                        json.optJSONArray("phones")?.let { arr -> if (arr.length() > 0) arr.optString(0, "") else "" } ?: ""
                    }
                }.trim()
                val lat = if (json.has("latitude")) json.optDouble("latitude") else if (json.has("lat")) json.optDouble("lat") else 35.7219
                val lng = if (json.has("longitude")) json.optDouble("longitude") else if (json.has("lng")) json.optDouble("lng") else 51.3347

                if (name.isNotBlank()) {
                    WorkshopNameHolder.current = name
                    WorkshopInfo(name, address, phone, lat, lng)
                } else null
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Fetch workshop info failed: ${e.message}")
            null
        }
    }

    // ==========================================================================
    // سفارشات راننده
    // GET /driver-api/routes/collection?driverId={id}
    // GET /driver-api/routes/delivery?driverId={id}
    // ==========================================================================

    suspend fun fetchDriverOrders(driverId: String): List<SupabaseOrderDto> = withContext(Dispatchers.IO) {
        val ordersMap = mutableMapOf<String, SupabaseOrderDto>()

        // 1. سفارش‌های جمع‌آوری
        try {
            val collectionUrl = "${functionsBase()}/driver-api/routes/collection?driverId=$driverId"
            val collectionOrders = fetchRoute(collectionUrl, "PICKUP", driverId)
            for (order in collectionOrders) {
                if (order.id.isNotBlank()) {
                    ordersMap[order.id] = order
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Error fetching collection orders: ${e.message}")
        }

        // 2. سفارش‌های تحویل
        try {
            val deliveryUrl = "${functionsBase()}/driver-api/routes/delivery?driverId=$driverId"
            val deliveryOrders = fetchRoute(deliveryUrl, "DELIVERY", driverId)
            for (order in deliveryOrders) {
                if (order.id.isNotBlank()) {
                    ordersMap[order.id] = order
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Error fetching delivery orders: ${e.message}")
        }

        ordersMap.values.toList()
    }

    private fun fetchRoute(url: String, defaultOrderType: String, driverId: String): List<SupabaseOrderDto> {
        val request = baseRequest(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string()?.trim() ?: return emptyList()
            return parseOrdersFromRawJson(body, defaultOrderType, driverId)
        }
    }

    private fun parseOrdersFromRawJson(body: String, defaultOrderType: String, driverId: String): List<SupabaseOrderDto> {
        val list = mutableListOf<SupabaseOrderDto>()
        try {
            if (body.startsWith("[")) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseSingleOrderJson(obj, defaultOrderType, driverId)?.let { list.add(it) }
                }
            } else if (body.startsWith("{")) {
                val root = JSONObject(body)
                val directArrays = listOf(
                    Pair(root.optJSONArray("orders"), defaultOrderType),
                    Pair(root.optJSONArray("data"), defaultOrderType),
                    Pair(root.optJSONArray("items"), defaultOrderType),
                    Pair(root.optJSONArray("routes"), defaultOrderType),
                    Pair(root.optJSONArray("collection"), "PICKUP"),
                    Pair(root.optJSONArray("delivery"), "DELIVERY"),
                    Pair(root.optJSONArray("result"), defaultOrderType)
                )

                var foundAnyArray = false
                for ((arr, type) in directArrays) {
                    if (arr != null && arr.length() > 0) {
                        foundAnyArray = true
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseSingleOrderJson(obj, type, driverId)?.let { list.add(it) }
                        }
                    }
                }

                if (!foundAnyArray) {
                    val nestedData = root.optJSONObject("data")
                    if (nestedData != null) {
                        val nestedOrders = nestedData.optJSONArray("orders") ?: nestedData.optJSONArray("items") ?: nestedData.optJSONArray("routes")
                        if (nestedOrders != null) {
                            for (i in 0 until nestedOrders.length()) {
                                val obj = nestedOrders.optJSONObject(i) ?: continue
                                parseSingleOrderJson(obj, defaultOrderType, driverId)?.let { list.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error parsing orders json", e)
        }
        return list
    }

    private fun parseSingleOrderJson(obj: JSONObject, defaultOrderType: String, driverId: String): SupabaseOrderDto? {
        val id = obj.optString("id").ifBlank {
            obj.optString("order_id").ifBlank {
                obj.optString("orderId").ifBlank {
                    obj.optString("tracking_code").ifBlank {
                        obj.optString("trackingCode").ifBlank { obj.optString("code") }
                    }
                }
            }
        }.trim()

        if (id.isBlank()) return null

        val trackingCode = obj.optString("tracking_code").ifBlank {
            obj.optString("trackingCode").ifBlank {
                obj.optString("code").ifBlank { id }
            }
        }.trim()

        val customerName = obj.optString("customer_name").ifBlank {
            obj.optString("customerName").ifBlank {
                obj.optString("name").ifBlank {
                    obj.optString("customer", "مشتری ${WorkshopNameHolder.current}")
                }
            }
        }.trim()

        val customerPhone = obj.optString("customer_phone").ifBlank {
            obj.optString("customerPhone").ifBlank {
                obj.optString("phone").ifBlank {
                    obj.optString("mobile", "")
                }
            }
        }.trim()

        val customerAddress = obj.optString("customer_address").ifBlank {
            obj.optString("customerAddress").ifBlank {
                obj.optString("address").ifBlank {
                    obj.optString("location", "تهران")
                }
            }
        }.trim()

        val lat = if (obj.has("latitude")) obj.optDouble("latitude") else if (obj.has("lat")) obj.optDouble("lat") else 35.7796
        val lng = if (obj.has("longitude")) obj.optDouble("longitude") else if (obj.has("lng")) obj.optDouble("lng") else if (obj.has("lon")) obj.optDouble("lon") else 51.4058

        val rawStatus = obj.optString("status").ifBlank {
            obj.optString("stage").ifBlank {
                obj.optString("order_status").ifBlank { "ASSIGNED" }
            }
        }.trim()

        val rawType = obj.optString("order_type").ifBlank {
            obj.optString("orderType").ifBlank {
                obj.optString("type").ifBlank { defaultOrderType }
            }
        }.trim().uppercase()

        val normalizedOrderType = if (
            rawType.contains("DELIVERY") || rawType.contains("تحویل") ||
            rawStatus.equals("READY_FOR_DELIVERY", ignoreCase = true) ||
            rawStatus.equals("ready_for_delivery", ignoreCase = true) ||
            rawStatus.equals("out_for_delivery", ignoreCase = true)
        ) {
            "DELIVERY"
        } else {
            "PICKUP"
        }

        val totalAmount = if (obj.has("total_amount")) obj.optLong("total_amount")
        else if (obj.has("totalAmount")) obj.optLong("totalAmount")
        else if (obj.has("totalPrice")) obj.optLong("totalPrice")
        else if (obj.has("price")) obj.optLong("price")
        else obj.optLong("amount", 0L)

        val paidAmount = if (obj.has("paid_amount")) obj.optLong("paid_amount")
        else if (obj.has("paidAmount")) obj.optLong("paidAmount")
        else if (obj.has("prepaid_amount")) obj.optLong("prepaid_amount")
        else if (obj.has("prepaidAmount")) obj.optLong("prepaidAmount")
        else if (obj.has("deposit_amount")) obj.optLong("deposit_amount")
        else 0L

        val discountAmount = if (obj.has("discount_amount")) obj.optLong("discount_amount")
        else if (obj.has("discountAmount")) obj.optLong("discountAmount")
        else if (obj.has("discount")) obj.optLong("discount")
        else 0L

        val remainingAmount = if (obj.has("remaining_amount")) obj.optLong("remaining_amount")
        else if (obj.has("remainingAmount")) obj.optLong("remainingAmount")
        else (totalAmount - discountAmount - paidAmount).coerceAtLeast(0L)

        val finalPayable = if (obj.has("final_payable")) obj.optLong("final_payable")
        else if (obj.has("finalPayable")) obj.optLong("finalPayable")
        else (totalAmount - discountAmount).coerceAtLeast(0L)

        val paymentMethod = driverApiPaymentMethodToLocal(
            obj.optString("payment_method").ifBlank {
                obj.optString("paymentMethod").ifBlank {
                    obj.optString("payment_type", "PENDING")
                }
            }
        )

        val paymentStatus = obj.optString("payment_status").ifBlank {
            obj.optString("paymentStatus", if (remainingAmount <= 0L && totalAmount > 0L) "paid" else "unpaid")
        }

        val rackCode = obj.optString("rack_code").ifBlank {
            obj.optString("rackCode").ifBlank {
                obj.optString("shelf", "")
            }
        }

        val cleanRackCode = obj.optString("clean_rack_code").ifBlank {
            obj.optString("cleanRackCode", "")
        }

        val returnReason = obj.optString("return_reason").ifBlank {
            obj.optString("returnReason", "")
        }

        val customerSignatureUrl = obj.optString("customer_signature_url").ifBlank {
            obj.optString("customerSignatureUrl").ifBlank {
                obj.optString("signature_url", "")
            }
        }

        val notes = obj.optString("notes").ifBlank {
            obj.optString("note").ifBlank {
                obj.optString("description", "")
            }
        }

        val orderDriverId = obj.optString("driver_id").ifBlank {
            obj.optString("driverId", driverId)
        }

        val orderDriverName = obj.optString("driver_name").ifBlank {
            obj.optString("driverName", "سفیر")
        }

        return SupabaseOrderDto(
            id = id,
            tracking_code = trackingCode,
            customer_name = customerName,
            customer_phone = customerPhone,
            customer_address = customerAddress,
            lat = lat,
            lng = lng,
            stage = driverApiStatusToLocalStage(rawStatus),
            status = driverApiStatusToLocalStatus(rawStatus),
            order_type = normalizedOrderType,
            driver_id = orderDriverId,
            driver_name = orderDriverName,
            total_amount = totalAmount,
            discount_amount = discountAmount,
            final_payable = finalPayable,
            paid_amount = paidAmount,
            payment_method = paymentMethod,
            payment_status = paymentStatus,
            rack_code = rackCode,
            clean_rack_code = cleanRackCode,
            return_reason = returnReason,
            customer_signature_url = customerSignatureUrl,
            notes = notes
        )
    }

    // ==========================================================================
    // آپدیت وضعیت و عملیات روی سفارش
    // PUT /driver-api/orders/{orderId}/status
    // POST /driver-api/orders/{orderId}/return-to-warehouse
    // POST /driver-api/orders/{orderId}/settle
    // POST /driver-api/orders/{orderId}/items
    // ==========================================================================

    suspend fun upsertOrder(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            when (order.status) {
                "RETURNED_TO_CLEAN_WAREHOUSE" -> pushReturnToWarehouse(order)
                "DELIVERED_SETTLED" -> pushSettle(order)
                else -> pushStatusUpdate(order)
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: push order update: ${e.message}")
            false
        }
    }

    private suspend fun pushStatusUpdate(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("status", localStatusToDriverApiStatus(order.status))
                put("rackCode", order.rackCode)
                put("notes", order.notes)
            }.toString()

            val endpoint = "${functionsBase()}/driver-api/orders/${order.id}/status"
            val request = baseRequest(endpoint)
                .put(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Status update failed: ${e.message}")
            false
        }
    }

    private suspend fun pushReturnToWarehouse(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("cleanRackCode", order.cleanRackCode)
                put("returnReason", order.returnReason)
                put("driverId", order.driverId)
            }.toString()

            val request = baseRequest("${functionsBase()}/driver-api/orders/${order.id}/return-to-warehouse")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Return to warehouse failed: ${e.message}")
            false
        }
    }

    private suspend fun pushSettle(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("paymentType", localPaymentMethodToDriverApi(order.paymentMethod))
                put("paidAmount", order.paidAmount)
                put("remainingAmount", (order.finalPayable - order.paidAmount).coerceAtLeast(0L))
                put("verifiedBarcodes", JSONArray())
            }.toString()

            val request = baseRequest("${functionsBase()}/driver-api/orders/${order.id}/settle")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Settle failed: ${e.message}")
            false
        }
    }

    suspend fun upsertCarpetItems(items: List<CarpetItemEntity>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext true
        try {
            val orderId = items.first().orderId
            val itemsArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.barcodeTag.ifBlank { "ITEM-${item.id}" })
                    put("carpetType", item.carpetType)
                    put("length", item.lengthMeter)
                    put("width", item.widthMeter)
                    put("area", item.areaSqMeter)
                    put("unitPricePerMeter", item.unitPricePerMeter)
                    put("totalPrice", item.totalPrice)
                    put("services", JSONArray(item.requestedServicesJson.split("،", ",").map { it.trim() }.filter { it.isNotBlank() }))
                    put("hasStain", item.defectsJson.isNotBlank())
                    put("stainDetails", item.defectsJson)
                    put("notes", item.notes)
                    put("barcodeTag", item.barcodeTag)
                    put("rackLocation", "")
                }
                itemsArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("items", itemsArray)
                put("prepaidAmount", 0)
            }.toString()

            val request = baseRequest("${functionsBase()}/driver-api/orders/$orderId/items")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Upsert carpet items failed: ${e.message}")
            false
        }
    }

    // ==========================================================================
    // تسویه‌حساب پایان روز راننده با دفتر
    // POST /driver-api/driver/office-settlement
    // ==========================================================================

    suspend fun upsertDriverSettlement(settlement: DriverSettlementEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderIds = try {
                val arr = JSONArray(settlement.orderIdsJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }

            val payload = JSONObject().apply {
                put("driverId", settlement.driverId)
                put("totalCash", settlement.totalCash)
                put("totalPos", settlement.totalPos)
                put("totalCardToCard", settlement.totalCardToCard)
                put("totalOnline", settlement.totalOnline)
                put("settledOrderIds", JSONArray(orderIds))
            }.toString()

            val request = baseRequest("${functionsBase()}/driver-api/driver/office-settlement")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error saving driver settlement", e)
            false
        }
    }

    // ==========================================================================
    // ورود راننده با کد پیامکی (OTP)
    // POST /otp/request
    // POST /otp/verify
    // ==========================================================================

    suspend fun requestOtp(phone: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val endpoint = "${functionsBase()}/otp/request"
        try {
            val payload = JSONObject().apply {
                put("phone", phone)
                put("mobile", phone)
            }.toString()

            val request = baseRequest(endpoint)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string() ?: ""
                val json = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }

                // مهم: حتی اگر ارسال پیامک واقعی شکست بخورد (مثلاً قالب
                // sms.ir موقتاً برای چیز دیگری استفاده می‌شود)، کد تایید از
                // قبل روی سرور تولید و ذخیره شده و به‌صورت زنده روی «کارت
                // راننده» در پنل نمایش داده می‌شود. پس در این حالت باید به
                // راننده اجازه‌ی ورود به صفحه‌ی وارد کردن کد را داد (با یک
                // هشدار)، نه اینکه کامل مسدودش کرد.
                val codeGenerated = json.optBoolean("codeGenerated", false)
                if (codeGenerated) {
                    val warnMsg = json.optString("error", "پیامک ارسال نشد؛ کد تایید را از دفتر مرکزی بپرسید.")
                    return@use Pair(true, warnMsg)
                }

                if (response.isSuccessful) {
                    val isOk = json.optBoolean("success", true) &&
                            json.optBoolean("ok", true) &&
                            json.optString("status", "success") != "error"

                    val serverMsg = json.optString("message",
                        json.optString("msg",
                            json.optString("detail", "کد تأیید ورود با موفقیت ارسال شد.")
                        )
                    )

                    if (isOk) {
                        Pair(true, serverMsg)
                    } else {
                        val errReason = json.optString("error", json.optString("message", "خطا در درخواست کد"))
                        Pair(false, "سرور: $errReason")
                    }
                } else {
                    val extractedError = when {
                        json.has("error") -> json.optString("error")
                        json.has("message") -> json.optString("message")
                        json.has("msg") -> json.optString("msg")
                        json.has("detail") -> json.optString("detail")
                        rawBody.isNotBlank() && rawBody.length < 200 -> rawBody
                        else -> "پاسخ ناموفق سرور با کد ${response.code}"
                    }
                    Pair(false, "خطای سرور (${response.code}): $extractedError")
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Network call failed for $endpoint: ${e.message}", e)
            Pair(false, "خطای ارتباط شبکه: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    suspend fun verifyOtp(phone: String, code: String): Triple<Boolean, String, String> = withContext(Dispatchers.IO) {
        val endpoint = "${functionsBase()}/otp/verify"
        try {
            val payload = JSONObject().apply {
                put("phone", phone)
                put("mobile", phone)
                put("code", code)
            }.toString()

            val request = baseRequest(endpoint)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string() ?: ""
                val json = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }

                if (response.isSuccessful) {
                    val isOk = json.optBoolean("success", true) &&
                            json.optBoolean("ok", true) &&
                            json.optString("status", "success") != "error"

                    if (isOk) {
                        val driverId = json.optString("driverId",
                            json.optString("driver_id",
                                json.optString("id", "DRV-101")
                            )
                        )
                        val driverName = json.optString("driverName", json.optString("driver_name", ""))
                        Triple(true, driverId.ifBlank { "DRV-101" }, driverName)
                    } else {
                        val errReason = json.optString("error", json.optString("message", "کد واردشده نادرست یا منقضی است."))
                        Triple(false, "سرور: $errReason", "")
                    }
                } else {
                    val extractedError = when {
                        json.has("error") -> json.optString("error")
                        json.has("message") -> json.optString("message")
                        json.has("msg") -> json.optString("msg")
                        json.has("detail") -> json.optString("detail")
                        rawBody.isNotBlank() && rawBody.length < 200 -> rawBody
                        else -> "پاسخ ناموفق سرور با کد ${response.code}"
                    }
                    Triple(false, "خطای سرور (${response.code}): $extractedError", "")
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Network call failed for verify $endpoint: ${e.message}", e)
            Triple(false, "خطای ارتباط شبکه: ${e.localizedMessage ?: e.javaClass.simpleName}", "")
        }
    }

    // ==========================================================================
    // چت با دیسپچر
    // POST /driver-api/chat/send
    // GET /driver-api/chat/messages?driverId={id}
    // ==========================================================================

    /**
     * @param driverId شناسه‌ی واقعی راننده‌ی لاگین‌کرده (نه message.orderId —
     *   که فیلدش برای شناسه‌ی موضوع/تِرد چت است، مقدار پیش‌فرضش هم "GENERAL"
     *   است و ربطی به شناسه‌ی راننده ندارد؛ قبلاً اشتباهاً همینجا استفاده
     *   می‌شد).
     */
    suspend fun sendChatMessage(message: ChatMessageEntity, driverId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("driverId", driverId.ifBlank { "DRV-101" })
                put("sender", message.sender)
                put("senderName", message.senderName)
                put("message", message.messageText)
                put("timestamp", message.timestamp)
            }.toString()

            val endpoint = "${functionsBase()}/driver-api/chat/send"
            val request = baseRequest(endpoint)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Send chat message failed: ${e.message}")
            false
        }
    }

    suspend fun fetchChatMessages(driverId: String): List<SupabaseChatMessageDto> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${functionsBase()}/driver-api/chat/messages?driverId=$driverId"
            val request = baseRequest(endpoint).get().build()
            val responseList = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()?.trim() ?: return@use null
                parseChatMessagesFromRawJson(body, driverId)
            }
            responseList ?: emptyList()
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Fetch chat messages failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseChatMessagesFromRawJson(body: String, driverId: String): List<SupabaseChatMessageDto> {
        val list = mutableListOf<SupabaseChatMessageDto>()
        try {
            if (body.startsWith("[")) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseSingleChatMessageJson(obj, driverId)?.let { list.add(it) }
                }
            } else if (body.startsWith("{")) {
                val root = JSONObject(body)
                val directArrays = listOf(
                    root.optJSONArray("messages"),
                    root.optJSONArray("data"),
                    root.optJSONArray("chat"),
                    root.optJSONArray("chat_messages"),
                    root.optJSONArray("items"),
                    root.optJSONArray("result")
                )
                for (arr in directArrays) {
                    if (arr != null && arr.length() > 0) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseSingleChatMessageJson(obj, driverId)?.let { list.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: Chat json parse: ${e.message}")
        }
        return list
    }

    private fun parseSingleChatMessageJson(obj: JSONObject, defaultDriverId: String): SupabaseChatMessageDto? {
        val text = obj.optString("text").ifBlank {
            obj.optString("message").ifBlank {
                obj.optString("message_text").ifBlank {
                    obj.optString("messageText").ifBlank {
                        obj.optString("content", "")
                    }
                }
            }
        }.trim()
        if (text.isBlank()) return null

        val id = obj.optString("id").ifBlank {
            obj.optString("message_id").ifBlank {
                obj.optString("messageId", "")
            }
        }.trim()

        val sender = obj.optString("sender").ifBlank {
            obj.optString("role").ifBlank {
                obj.optString("sender_type", "DISPATCHER")
            }
        }.trim()

        val senderName = obj.optString("sender_name").ifBlank {
            obj.optString("senderName").ifBlank {
                obj.optString("name", if (sender.equals("DRIVER", ignoreCase = true)) "سفیر راننده" else "دیسپچینگ ${WorkshopNameHolder.current}")
            }
        }.trim()

        val ts = obj.optString("timestamp").ifBlank {
            obj.optString("created_at").ifBlank {
                obj.optString("createdAt").ifBlank {
                    obj.optString("time", System.currentTimeMillis().toString())
                }
            }
        }.trim()

        val orderDriverId = obj.optString("driver_id").ifBlank {
            obj.optString("driverId", defaultDriverId)
        }.trim()

        return SupabaseChatMessageDto(
            id = id,
            driver_id = orderDriverId,
            sender = sender,
            sender_name = senderName,
            text = text,
            timestamp = ts
        )
    }

    // ==========================================================================
    // نرخ‌نامه و تعرفه خدمات
    // GET /driver-api/tariffs
    // ==========================================================================

    suspend fun fetchTariffs(): TariffSyncResult = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${functionsBase()}/driver-api/tariffs"
            val request = baseRequest(endpoint).get().build()
            val result = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()?.trim() ?: return@use null
                parseTariffSyncResult(body)
            }
            if (result != null && (result.carpetTariffs.isNotEmpty() || result.serviceTariffs.isNotEmpty())) {
                return@withContext result
            }
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: tariff endpoint failed: ${e.message}")
        }

        TariffSyncResult.createDefault()
    }

    private fun parseTariffSyncResult(body: String): TariffSyncResult? {
        try {
            val carpets = mutableListOf<CarpetTariffItem>()
            val services = mutableListOf<ServiceTariffItem>()
            val defects = mutableListOf<DefectTariffItem>()

            if (body.startsWith("[")) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseGenericTariffObject(obj, carpets, services, defects)
                }
            } else if (body.startsWith("{")) {
                val root = JSONObject(body)

                val carpetArrays = listOf(
                    root.optJSONArray("carpets"),
                    root.optJSONArray("carpet_types"),
                    root.optJSONArray("carpetTypes"),
                    root.optJSONArray("tariffs"),
                    root.optJSONArray("pricing"),
                    root.optJSONArray("rates"),
                    root.optJSONArray("items")
                )
                for (arr in carpetArrays) {
                    if (arr != null && arr.length() > 0) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseGenericTariffObject(obj, carpets, services, defects)
                        }
                    }
                }

                val serviceArrays = listOf(
                    root.optJSONArray("services"),
                    root.optJSONArray("service_types"),
                    root.optJSONArray("extra_services"),
                    root.optJSONArray("repair_services")
                )
                for (arr in serviceArrays) {
                    if (arr != null && arr.length() > 0) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val srv = parseSingleServiceTariff(obj)
                            if (srv != null) services.add(srv)
                        }
                    }
                }

                val defectArrays = listOf(
                    root.optJSONArray("defects"),
                    root.optJSONArray("flaws"),
                    root.optJSONArray("initial_defects")
                )
                for (arr in defectArrays) {
                    if (arr != null && arr.length() > 0) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val defTitle = obj.optString("title").ifBlank { obj.optString("name") }.trim()
                            if (defTitle.isNotBlank()) {
                                defects.add(
                                    DefectTariffItem(
                                        id = obj.optString("id", "DEF-$i"),
                                        title = defTitle,
                                        description = obj.optString("description")
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val finalCarpets = if (carpets.isNotEmpty()) carpets else TariffSyncResult.DEFAULT_CARPET_TARIFFS
            val finalServices = if (services.isNotEmpty()) services else TariffSyncResult.DEFAULT_SERVICE_TARIFFS
            val finalDefects = if (defects.isNotEmpty()) defects else TariffSyncResult.DEFAULT_DEFECT_TARIFFS

            return TariffSyncResult(
                carpetTariffs = finalCarpets,
                serviceTariffs = finalServices,
                defectTariffs = finalDefects,
                lastSyncTime = System.currentTimeMillis(),
                isLiveFromSupabase = carpets.isNotEmpty() || services.isNotEmpty(),
                sourceDescription = if (carpets.isNotEmpty() || services.isNotEmpty()) "همگام‌شده آنلاین با پنل وب" else "نرخ‌نامه مصوب ${WorkshopNameHolder.current}"
            )
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Notice: parse tariffs failed: ${e.message}")
            return null
        }
    }

    private fun parseGenericTariffObject(
        obj: JSONObject,
        carpets: MutableList<CarpetTariffItem>,
        services: MutableList<ServiceTariffItem>,
        defects: MutableList<DefectTariffItem>
    ) {
        val title = obj.optString("title").ifBlank {
            obj.optString("name").ifBlank {
                obj.optString("carpet_type").ifBlank {
                    obj.optString("carpetType").ifBlank {
                        obj.optString("service_name").ifBlank { obj.optString("label", "") }
                    }
                }
            }
        }.trim()

        if (title.isBlank()) return

        val itemType = obj.optString("type").ifBlank { obj.optString("category", "") }.trim()

        if (itemType.contains("service", ignoreCase = true) || itemType.contains("خدمت") || itemType.contains("ترمیم") || itemType.contains("شستشو_اضافه")) {
            val srv = parseSingleServiceTariff(obj)
            if (srv != null) services.add(srv)
            return
        }

        val unitPrice = obj.optLong("unit_price", 0L).let { if (it > 0) it else obj.optLong("unitPrice", 0L) }
            .let { if (it > 0) it else obj.optLong("price_per_meter", 0L) }
            .let { if (it > 0) it else obj.optLong("price_per_sqm", 0L) }
            .let { if (it > 0) it else obj.optLong("price", 0L) }
            .let { if (it > 0) it else obj.optLong("rate", 0L) }
            .let { if (it > 0) it else obj.optLong("base_price", 120_000L) }

        val length = obj.optDouble("default_length", 0.0).let { if (it > 0.0) it else obj.optDouble("length", 3.0) }
        val width = obj.optDouble("default_width", 0.0).let { if (it > 0.0) it else obj.optDouble("width", 2.0) }
        val category = if (itemType.isNotBlank()) itemType else when {
            title.contains("دستبافت") -> "دستبافت"
            title.contains("گلیم") || title.contains("گبه") -> "گلیم"
            title.contains("موکت") -> "موکت"
            title.contains("پتو") -> "پتو"
            title.contains("پرده") -> "پرده"
            else -> "ماشینی"
        }

        carpets.add(
            CarpetTariffItem(
                id = obj.optString("id").ifBlank { "CT-${carpets.size + 1}" },
                title = title,
                category = category,
                unitPricePerMeter = unitPrice,
                defaultLength = length,
                defaultWidth = width,
                unit = obj.optString("unit", "متر مربع"),
                description = obj.optString("description", "")
            )
        )
    }

    private fun parseSingleServiceTariff(obj: JSONObject): ServiceTariffItem? {
        val title = obj.optString("title").ifBlank {
            obj.optString("name").ifBlank {
                obj.optString("service_name").ifBlank { obj.optString("label", "") }
            }
        }.trim()
        if (title.isBlank()) return null

        val price = obj.optLong("price", 0L).let { if (it > 0) it else obj.optLong("unit_price", 0L) }
            .let { if (it > 0) it else obj.optLong("unitPrice", 0L) }
            .let { if (it > 0) it else obj.optLong("cost", 0L) }
            .let { if (it > 0) it else obj.optLong("fee", 50_000L) }

        val isPercentage = obj.optBoolean("is_percentage", false) || obj.optBoolean("isPercentage", false)
        val percentage = obj.optDouble("percentage", 0.0).let { if (it > 0.0) it else obj.optDouble("percent", 0.0) }

        return ServiceTariffItem(
            id = obj.optString("id").ifBlank { "SRV-${title.hashCode()}" },
            title = title,
            price = price,
            isPercentage = isPercentage,
            percentage = percentage,
            description = obj.optString("description", "")
        )
    }

    // ==========================================================================
    // آپلود امضای دیجیتال
    // POST /driver-api/signature/upload
    // ==========================================================================

    suspend fun uploadSignature(orderId: String, signatureBase64: String): String? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("orderId", orderId)
                put("signatureBase64", signatureBase64)
            }.toString()

            val endpoint = "${functionsBase()}/driver-api/signature/upload"
            val request = baseRequest(endpoint)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            val url = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()?.trim() ?: return@use null
                if (body.startsWith("{")) {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", true)) {
                        json.optString("url").ifBlank {
                            json.optString("signatureUrl").ifBlank {
                                json.optString("signature_url").ifBlank { null }
                            }
                        }
                    } else null
                } else null
            }
            url
        } catch (e: Exception) {
            Log.d("SupabaseManager", "Signature upload failed: ${e.message}")
            null
        }
    }

    // ==========================================================================
    // نگاشت مقادیر بین اپ اندروید و قرارداد JSON واقعی driver-api
    // ==========================================================================

    private fun localStatusToDriverApiStatus(status: String): String =
        if (status == "COLLECTED_IN_INSPECTION") "COLLECTED" else status

    private fun driverApiStatusToLocalStatus(status: String): String =
        if (status == "COLLECTED") "COLLECTED_IN_INSPECTION" else status

    private fun driverApiStatusToLocalStage(status: String): String = when (status) {
        "ASSIGNED" -> "pickup_assigned"
        "COLLECTED" -> "collected"
        "DELIVERED_TO_WORKSHOP" -> "factory_received"
        "WASHING" -> "washing"
        "READY_FOR_DELIVERY" -> "ready_for_delivery"
        "DELIVERED_SETTLED" -> "delivered"
        "RETURNED_TO_CLEAN_WAREHOUSE" -> "returned_to_clean_warehouse"
        "OFFICE_SETTLED" -> "office_settled"
        else -> "pickup_assigned"
    }

    private fun localPaymentMethodToDriverApi(method: String): String = when (method) {
        "cash" -> "CASH"
        "pos" -> "POS"
        "card_to_card", "online" -> "CREDIT"
        else -> "PENDING"
    }

    private fun driverApiPaymentMethodToLocal(method: String): String = when (method) {
        "CASH" -> "cash"
        "POS" -> "pos"
        "CREDIT" -> "card_to_card"
        else -> "unpaid"
    }
}
