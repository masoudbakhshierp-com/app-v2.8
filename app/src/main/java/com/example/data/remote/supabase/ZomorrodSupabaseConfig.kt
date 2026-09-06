package com.example.data.remote.supabase

import com.example.BuildConfig

/**
 * پیکربندی و تنظیمات ارتباط با پروژه واقعی Supabase قالیشویی صبا.
 *
 * توجه مهم: اپ اندروید مستقیم به جدول‌های دیتابیس (Postgrest) وصل نمی‌شود —
 * چون جدول‌های orders/drivers در Supabase با Row Level Security فقط برای
 * کاربر لاگین‌کرده (Auth) در پنل وب باز هستند. به‌جایش، همه‌ی درخواست‌های
 * راننده باید از طریق Edge Functionهایی بره که برای همین منظور در پروژه‌ی
 * وب (supabase/functions/driver-api و supabase/functions/otp) ساخته شده‌اند
 * و با DRIVER_API_KEY احراز هویت می‌شوند.
 *
 * مقادیر واقعی هیچ‌وقت اینجا هاردکد نمی‌شوند (قبلاً یک نسخه‌ی واقعی از
 * URL/کلید اینجا هاردکد شده بود که همراه با کل کدبیس لو رفت — همان کلید
 * روی Supabase باید rotate شود). این مقادیر همیشه فقط از BuildConfig
 * می‌آیند که خودش از فایل .env (گیت‌ایگنورشده، کنار همین ماژول app) پر
 * می‌شود (پلاگین Secrets Gradle، پایین‌تر در app/build.gradle.kts تنظیم
 * شده). اسکریپت android_deploy.py همین .env را هر بار قبل از build از
 * شما می‌پرسد و می‌سازد. اگر .env وجود نداشته باشد، مقدار placeholder از
 * .env.example جایگزین می‌شود تا فقط کامپایل انجام شود — عمداً هیچ
 * درخواست شبکه‌ای با placeholder موفق نمی‌شود.
 */
object ZomorrodSupabaseConfig {
    // پروژه‌های قدیمی/دموی Supabase که قبلاً در این کدبیس دیده شده‌اند —
    // اگر BuildConfig یکی از این‌ها را برگرداند (یا خودِ placeholder را)،
    // یعنی .env واقعی و به‌روز ساخته نشده، پس باید فقط PLACEHOLDER استفاده
    // شود تا درخواست شبکه‌ای بی‌صدا به پروژه‌ی اشتباه نرود.
    val KNOWN_INVALID_PROJECT_REFS = setOf(
        "oagrzbdjxhhkrqlfjqri",
        "eofavazsqwqzrmjvknrw",
        "cymeqpkoxwoqjyuuzhur", // پروژه‌ی fallback/دموی پنل وب — پروژه‌ی راننده نیست
        "your-project-ref",     // placeholder خود .env.example
    )
    private const val KNOWN_INVALID_DRIVER_API_KEY_LITERAL = "kg0zE1kxIg_KjssvT7lHu0qIDoVLxBLS"
    const val KNOWN_INVALID_DRIVER_API_KEY = KNOWN_INVALID_DRIVER_API_KEY_LITERAL

    private const val PLACEHOLDER_SUPABASE_URL = "https://your-project-ref.supabase.co"
    private const val PLACEHOLDER_DRIVER_API_KEY = "REPLACE_WITH_REAL_DRIVER_API_KEY"

    val DEFAULT_SUPABASE_URL: String = run {
        val buildUrl = try { BuildConfig.SUPABASE_URL } catch (_: Throwable) { "" }
        val isKnownInvalid = KNOWN_INVALID_PROJECT_REFS.any { buildUrl.contains(it) }
        if (buildUrl.isNotBlank() && !isKnownInvalid) {
            buildUrl.trim().removeSuffix("/")
        } else {
            PLACEHOLDER_SUPABASE_URL
        }
    }

    val DRIVER_API_KEY: String = run {
        val buildKey = try { BuildConfig.DRIVER_API_KEY } catch (_: Throwable) { "" }
        if (buildKey.isNotBlank() && buildKey != KNOWN_INVALID_DRIVER_API_KEY_LITERAL) buildKey.trim() else PLACEHOLDER_DRIVER_API_KEY
    }

    // پایه‌ی آدرس Edge Functionها
    val FUNCTIONS_BASE_URL: String = "$DEFAULT_SUPABASE_URL/functions/v1"

    // مسیرهای واقعی Edge Function driver-api (بدون /driver-api چون در URL کامل اضافه می‌شود)
    object DriverApiPaths {
        val COLLECTION_ROUTE = "$FUNCTIONS_BASE_URL/driver-api/routes/collection"
        val DELIVERY_ROUTE = "$FUNCTIONS_BASE_URL/driver-api/routes/delivery"
        val HEALTH_CHECK = "$FUNCTIONS_BASE_URL/driver-api/health"
        val TARIFFS = "$FUNCTIONS_BASE_URL/driver-api/tariffs"
        val WORKSHOP = "$FUNCTIONS_BASE_URL/driver-api/workshop"
        val CHAT_SEND = "$FUNCTIONS_BASE_URL/driver-api/chat/send"
        val CHAT_MESSAGES = "$FUNCTIONS_BASE_URL/driver-api/chat/messages"
        val SIGNATURE_UPLOAD = "$FUNCTIONS_BASE_URL/driver-api/signature/upload"
        fun orderItems(orderId: String) = "$FUNCTIONS_BASE_URL/driver-api/orders/$orderId/items"
        fun orderStatus(orderId: String) = "$FUNCTIONS_BASE_URL/driver-api/orders/$orderId/status"
        fun returnToWarehouse(orderId: String) = "$FUNCTIONS_BASE_URL/driver-api/orders/$orderId/return-to-warehouse"
        fun settle(orderId: String) = "$FUNCTIONS_BASE_URL/driver-api/orders/$orderId/settle"
        val OFFICE_SETTLEMENT = "$FUNCTIONS_BASE_URL/driver-api/driver/office-settlement"
        val LOCATION = "$FUNCTIONS_BASE_URL/driver-api/driver/location"
    }

    // مسیرهای Edge Function otp (ورود راننده با کد پیامکی)
    object OtpPaths {
        val REQUEST = "$FUNCTIONS_BASE_URL/otp/request"
        val VERIFY = "$FUNCTIONS_BASE_URL/otp/verify"
    }
}
