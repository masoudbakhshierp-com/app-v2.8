package com.example.data.model

/**
 * مدل‌های نرخ‌نامه و تعرفه خدمات قالیشویی صبا
 * هماهنگ با پنل وب و جداول دیتابیس Supabase
 */
data class CarpetTariffItem(
    val id: String,
    val title: String,
    val category: String,
    val unitPricePerMeter: Long,
    val defaultLength: Double = 3.0,
    val defaultWidth: Double = 2.0,
    val unit: String = "متر مربع",
    val description: String = ""
)

data class ServiceTariffItem(
    val id: String,
    val title: String,
    val price: Long,
    val isPercentage: Boolean = false,
    val percentage: Double = 0.0,
    val description: String = ""
)

data class DefectTariffItem(
    val id: String,
    val title: String,
    val description: String = ""
)

data class TariffSyncResult(
    val carpetTariffs: List<CarpetTariffItem>,
    val serviceTariffs: List<ServiceTariffItem>,
    val defectTariffs: List<DefectTariffItem>,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isLiveFromSupabase: Boolean = false,
    val sourceDescription: String = ""
) {
    companion object {
        val DEFAULT_CARPET_TARIFFS = listOf(
            CarpetTariffItem(
                id = "CT-M6",
                title = "ماشینی ۶ متری (۲×۳)",
                category = "ماشینی",
                unitPricePerMeter = 120_000L,
                defaultLength = 3.0,
                defaultWidth = 2.0,
                description = "شستشوی مکانیزه فرش ماشینی ۶ متری"
            ),
            CarpetTariffItem(
                id = "CT-M9",
                title = "ماشینی ۹ متری (۲٫۵×۳٫۵)",
                category = "ماشینی",
                unitPricePerMeter = 120_000L,
                defaultLength = 3.5,
                defaultWidth = 2.5,
                description = "شستشوی مکانیزه فرش ماشینی ۹ متری"
            ),
            CarpetTariffItem(
                id = "CT-M12",
                title = "ماشینی ۱۲ متری (۳×۴)",
                category = "ماشینی",
                unitPricePerMeter = 120_000L,
                defaultLength = 4.0,
                defaultWidth = 3.0,
                description = "شستشوی مکانیزه فرش ماشینی ۱۲ متری"
            ),
            CarpetTariffItem(
                id = "CT-MCUST",
                title = "ماشینی سایز سفارشی / کناره / پادری",
                category = "ماشینی",
                unitPricePerMeter = 120_000L,
                defaultLength = 2.0,
                defaultWidth = 1.0,
                description = "فرش ماشینی با ابعاد متغیر"
            ),
            CarpetTariffItem(
                id = "CT-HWOOL",
                title = "دستبافت پشمی / کاشان و مشهد",
                category = "دستبافت",
                unitPricePerMeter = 240_000L,
                defaultLength = 3.0,
                defaultWidth = 2.0,
                description = "شستشوی سنتی با تثبیت‌کننده گیاهی رنگ"
            ),
            CarpetTariffItem(
                id = "CT-HNAIN",
                title = "دستبافت نائین / تبریز (چله ابریشم)",
                category = "دستبافت",
                unitPricePerMeter = 320_000L,
                defaultLength = 3.0,
                defaultWidth = 2.0,
                description = "شستشوی تخصصی چله ابریشم و نائین"
            ),
            CarpetTariffItem(
                id = "CT-HSILK",
                title = "دستبافت تمام ابریشم / قم و اصفهان",
                category = "دستبافت",
                unitPricePerMeter = 480_000L,
                defaultLength = 2.0,
                defaultWidth = 1.5,
                description = "شستشوی دستبافت تمام ابریشم صادراتی"
            ),
            CarpetTariffItem(
                id = "CT-GABBEH",
                title = "گلیم / گبه / جاجیم و سنتی",
                category = "گلیم",
                unitPricePerMeter = 150_000L,
                defaultLength = 2.0,
                defaultWidth = 1.5,
                description = "شستشوی تخصصی دستبافت عشایری"
            ),
            CarpetTariffItem(
                id = "CT-MOKET",
                title = "موکت معمولی / کبریتی",
                category = "موکت",
                unitPricePerMeter = 60_000L,
                defaultLength = 3.0,
                defaultWidth = 2.0,
                description = "شستشوی متری موکت معمولی"
            ),
            CarpetTariffItem(
                id = "CT-PALAZ",
                title = "موکت پالاز / پرز بلند / تایل",
                category = "موکت",
                unitPricePerMeter = 85_000L,
                defaultLength = 3.0,
                defaultWidth = 2.0,
                description = "شستشوی مکانیزه موکت تافتینگ"
            ),
            CarpetTariffItem(
                id = "CT-BLANKET",
                title = "پتوشویی (تک‌نفره / دونفره)",
                category = "پتو",
                unitPricePerMeter = 160_000L,
                defaultLength = 1.0,
                defaultWidth = 1.0,
                unit = "تخته",
                description = "شستشو، ضدعفونی و نرم‌کننده پتو"
            ),
            CarpetTariffItem(
                id = "CT-CURTAIN",
                title = "پرده تور / مخمل / زبرا",
                category = "پرده",
                unitPricePerMeter = 90_000L,
                defaultLength = 2.0,
                defaultWidth = 2.0,
                description = "شستشو و اتوکشی پرده"
            )
        )

        val DEFAULT_SERVICE_TARIFFS = listOf(
            ServiceTariffItem(
                id = "SRV-NANO",
                title = "شستشوی ویژه اعلا (نانوشویی و ضدباکتری)",
                price = 50_000L,
                description = "افزودن مواد شوینده نانو و خوشبوکننده ویژه"
            ),
            ServiceTariffItem(
                id = "SRV-SILK_WASH",
                title = "ابریشم‌شویی و براق‌کننده الیاف",
                price = 120_000L,
                description = "شستشو با شوینده‌های خنثی و بدون اسید"
            ),
            ServiceTariffItem(
                id = "SRV-REPAIR",
                title = "رفوگری، مرمت و ریشه‌بافی",
                price = 180_000L,
                description = "ترمیم پوسیدگی و احیای ریشه‌های آسیب‌دیده"
            ),
            ServiceTariffItem(
                id = "SRV-SHIRAZEH",
                title = "شیرازه‌دوزی و زیگزاگ لبه",
                price = 90_000L,
                description = "دوخت و تقویت حاشیه‌های طولی فرش"
            ),
            ServiceTariffItem(
                id = "SRV-STAIN",
                title = "لکه‌بری تخصصی (قهوه، جوهر، چربی)",
                price = 70_000L,
                description = "استفاده از حلال‌های ارگانیک لکه‌بر"
            ),
            ServiceTariffItem(
                id = "SRV-IRON_COVER",
                title = "ضدعفونی، اتوکشی و کاور نایلونی",
                price = 40_000L,
                description = "پرس حرارتی و بسته‌بندی بهداشتی"
            ),
            ServiceTariffItem(
                id = "SRV-COLOR_FIX",
                title = "داروکشی، رنگ‌برداری و یکدست‌سازی",
                price = 160_000L,
                description = "برطرف کردن تداخل رنگ فرش‌های دستبافت"
            ),
            ServiceTariffItem(
                id = "SRV-LEATHER",
                title = "چرم‌دوزی پشت فرش (ضد لغزش)",
                price = 110_000L,
                description = "دوخت نوار چرم طبیعی جهت جلوگیری از سرخوردن"
            )
        )

        val DEFAULT_DEFECT_TARIFFS = listOf(
            DefectTariffItem(id = "DEF-NONE", title = "بدون عیب اولیه", description = "فرش کاملاً سالم تحویل گرفته شد"),
            DefectTariffItem(id = "DEF-BURN", title = "سوختگی جزئی / لکه زغال و اتو"),
            DefectTariffItem(id = "DEF-ROTTEN", title = "پوسیدگی ریشه و حاشیه"),
            DefectTariffItem(id = "DEF-TEAR", title = "پارگی / شکافتگی تار و پود"),
            DefectTariffItem(id = "DEF-MOTH", title = "بیدزدگی و خوردگی پشم"),
            DefectTariffItem(id = "DEF-COLOR_BLEED", title = "تغییر رنگ / رنگ‌دویدگی قبلی"),
            DefectTariffItem(id = "DEF-DIRT_STAIN", title = "لکه شدید و رسوب چربی عمیق")
        )

        fun createDefault(): TariffSyncResult {
            return TariffSyncResult(
                carpetTariffs = DEFAULT_CARPET_TARIFFS,
                serviceTariffs = DEFAULT_SERVICE_TARIFFS,
                defectTariffs = DEFAULT_DEFECT_TARIFFS,
                lastSyncTime = System.currentTimeMillis(),
                isLiveFromSupabase = false,
                sourceDescription = "نرخ‌نامه مصوب ${com.example.data.WorkshopNameHolder.current} (حافظه محلی)"
            )
        }
    }
}
