package com.example.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FarsiUtils {

    fun formatPrice(amountToman: Long): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amountToman)
        return "${toFarsiDigits(formatted)} تومان"
    }

    fun formatPriceShort(amountToman: Long): String {
        val formatter = DecimalFormat("#,###")
        return toFarsiDigits(formatter.format(amountToman))
    }

    fun toFarsiDigits(input: String): String {
        val farsiDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(farsiDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun toEnglishDigits(input: String): String {
        return input
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
    }

    fun formatArea(sqMeters: Double): String {
        val formatted = String.format(Locale.US, "%.2f", sqMeters)
        return "${toFarsiDigits(formatted)} متر مربع"
    }

    fun formatCurrentTimeFarsi(): String {
        val sdf = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault())
        return toFarsiDigits(sdf.format(Date()))
    }

    fun formatShortTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return toFarsiDigits(sdf.format(Date(timestamp)))
    }

    fun getCarpetTypeLabel(type: String): String {
        return when (type) {
            "MACHINE" -> "فرش ماشینی"
            "HANDMADE" -> "فرش دستبافت"
            "SILK" -> "فرش ابریشم / گل‌ابریشم"
            "GELIM" -> "گلیم / گبه / جاجیم"
            "FANTASY" -> "فرش مدرن / فانتزی"
            else -> type
        }
    }

    fun getOrderStatusLabel(status: String): String {
        return when (status) {
            "ASSIGNED" -> "تخصیص‌یافته به راننده"
            "COLLECTED_IN_INSPECTION" -> "ثبت شده در محل (پیش‌فاکتور)"
            "DELIVERED_TO_WORKSHOP" -> "تحویل به کارگاه (قفسه‌بندی)"
            "WASHING" -> "در حال شست‌وشو"
            "READY_FOR_DELIVERY" -> "آماده تحویل به مشتری"
            "RETURNED_TO_CLEAN_WAREHOUSE" -> "برگشت به قفسه تمیز انبار (عدم حضور مشتری)"
            "OFFICE_SETTLED" -> "تسویه‌شده با دفتر مدیریت"
            "DELIVERED_SETTLED" -> "تحویل داده شده و تسویه کامل"
            "CANCELLED" -> "لغو شده"
            else -> status
        }
    }
}
