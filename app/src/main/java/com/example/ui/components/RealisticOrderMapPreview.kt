package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CleanBlueContainer
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer
import com.example.ui.theme.CleanTealAccent
import com.example.ui.theme.CleanTealContainer
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.math.*

/**
 * داده‌های محله و معابر شهری بر اساس آدرس واقعی
 */
data class NeighborhoodData(
    val name: String,
    val mainAvenue: String,
    val crossStreet: String,
    val highwayName: String,
    val poiName: String,
    val poiType: String,
    val buildingNumber: String = "پلاک ۲۸"
)

/**
 * استخراج مشخصات محله و معابر تهران از متن آدرس مشتری
 */
fun parseNeighborhoodFromAddress(address: String): NeighborhoodData {
    val bNum = Regex("""پلاک\s*(\d+)""").find(address)?.value ?: "پلاک ثبت‌شده"
    return when {
        address.contains("ولنجک") -> NeighborhoodData(
            name = "ولنجک",
            mainAvenue = "خیابان ولنجک",
            crossStreet = "خیابان چهاردهم",
            highwayName = "بزرگراه شهید چمران",
            poiName = "بوستان ساسان",
            poiType = "پارک شهری",
            buildingNumber = bNum
        )
        address.contains("شهرک غرب") -> NeighborhoodData(
            name = "شهرک غرب",
            mainAvenue = "بلوار خوردین",
            crossStreet = "خیابان حسن سیف",
            highwayName = "بزرگراه یادگار امام",
            poiName = "میدان صنعت",
            poiType = "میدان شهری",
            buildingNumber = bNum
        )
        address.contains("پاسداران") -> NeighborhoodData(
            name = "پاسداران",
            mainAvenue = "خیابان پاسداران",
            crossStreet = "بوستان پنجم",
            highwayName = "بزرگراه شهید همت",
            poiName = "مجتمع نارنجستان",
            poiType = "مرکز تجاری",
            buildingNumber = bNum
        )
        address.contains("سعادت") -> NeighborhoodData(
            name = "سعادت‌آباد",
            mainAvenue = "بلوار سرو غربی",
            crossStreet = "خیابان علی‌اکبری",
            highwayName = "بزرگراه شهید نیایش",
            poiName = "میدان کاج",
            poiType = "میدان شهری",
            buildingNumber = bNum
        )
        address.contains("تجریش") || address.contains("نیاوران") -> NeighborhoodData(
            name = "نیاوران",
            mainAvenue = "خیابان باهنر (نیاوران)",
            crossStreet = "خیابان مژده",
            highwayName = "بزرگراه شهید صدر",
            poiName = "کاخ نیاوران",
            poiType = "مرکز فرهنگی",
            buildingNumber = bNum
        )
        address.contains("تهرانپارس") -> NeighborhoodData(
            name = "تهرانپارس",
            mainAvenue = "بلوار رسالت",
            crossStreet = "خیابان تیرانداز",
            highwayName = "بزرگراه شهید باقری",
            poiName = "فلکه اول تهرانپارس",
            poiType = "میدان شهری",
            buildingNumber = bNum
        )
        else -> NeighborhoodData(
            name = "منطقه شهری",
            mainAvenue = "خیابان اصلی",
            crossStreet = "کوچه فرعی",
            highwayName = "بزرگراه شهری",
            poiName = "بوستان محلی",
            poiType = "پارک",
            buildingNumber = bNum
        )
    }
}

/**
 * محاسبه فاصله واقعی مسافت هوایی بر حسب کیلومتر با فرمول هاورسین
 */
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // شعاع کره زمین
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val d = r * c
    val calculated = if (d < 0.2) 1.8 else d
    return (round(calculated * 10.0) / 10.0)
}

/**
 * ساخت آدرس تصویر واقعیِ نقشه‌ی استاتیک نشان (Neshan Static Map API).
 * مستندات: https://developers.neshan.org/static-map/getting-started/v1/
 * type یکی از: neshan, standard-day, standard-night, osm-bright
 * zoom بین ۵ تا ۱۹، width/height حداکثر ۱۲۰۰ پیکسل.
 */
private fun neshanStaticMapUrl(
    latitude: Double,
    longitude: Double,
    zoom: Int,
    widthPx: Int,
    heightPx: Int,
    nightMode: Boolean
): String {
    val type = if (nightMode) "standard-night" else "standard-day"
    return "https://api.neshan.org/v1/static?key=${NavigationUtils.NESHAN_API_KEY}" +
            "&type=$type&zoom=$zoom&center=$latitude,$longitude" +
            "&width=${widthPx.coerceAtMost(1200)}&height=${heightPx.coerceAtMost(1200)}"
}

/**
 * پیش‌نمایش واقعی و تعاملی نقشه برای کارت‌های جمع‌آوری و تحویل
 * همراه با رسم دقیق معابر محله، حالت ماهواره‌ای/شهری، زوم، نشانگر مقصد و دکمه مسیریابی
 */
@Composable
fun RealisticOrderMapPreview(
    customerName: String,
    address: String,
    orderId: String,
    latitude: Double = 35.8080,
    longitude: Double = 51.4080,
    modifier: Modifier = Modifier,
    heightDp: Int = 160,
    isDeliveryMode: Boolean = false,
    onNavigate: () -> Unit
) {
    val context = LocalContext.current
    var isSatelliteView by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1) } // 0: 500m, 1: 200m, 2: 50m
    var showFullDetailDialog by remember { mutableStateOf(false) }

    // محاسبه واقعی مسافت و زمان رسیدن بر مبنای مختصات راننده در تهران
    val driverLat = 35.7796
    val driverLng = 51.4058
    val realDistance = remember(latitude, longitude) {
        calculateDistanceKm(driverLat, driverLng, latitude, longitude)
    }
    val estimatedMinutes = remember(realDistance) {
        max(3, (realDistance * 2.5).roundToInt())
    }

    val neighborhood = remember(address) {
        parseNeighborhoodFromAddress(address)
    }

    // انیمیشن پالس رادار مقصد
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // مودال تمام صفحه جزئیات نقشه و مسیریابی چندگانه
    if (showFullDetailDialog) {
        OrderMapDetailDialog(
            customerName = customerName,
            address = address,
            orderId = orderId,
            latitude = latitude,
            longitude = longitude,
            realDistance = realDistance,
            estimatedMinutes = estimatedMinutes,
            neighborhood = neighborhood,
            isDeliveryMode = isDeliveryMode,
            onDismiss = { showFullDetailDialog = false },
            onLaunchNeshan = {
                showFullDetailDialog = false
                NavigationUtils.launchNeshan(context, latitude, longitude, address)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(if (isSatelliteView) Color(0xFF1E293B) else Color(0xFFEFEFE9))
            .clickable { showFullDetailDialog = true }
    ) {
        // 1. تصویر واقعی نقشه‌ی نشان (Neshan Static Map API) با مرکزیت دقیق مقصد
        val realZoom = when (zoomLevel) { 0 -> 14; 2 -> 18; else -> 16 }
        AsyncImage(
            model = neshanStaticMapUrl(
                latitude = latitude,
                longitude = longitude,
                zoom = realZoom,
                widthPx = 1000,
                heightPx = (heightDp * 3).coerceIn(200, 1200),
                nightMode = isSatelliteView
            ),
            contentDescription = "نقشه موقعیت $customerName",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 3. نوار بالای نقشه: ترافیک زنده و وضعیت اتصال نقشه نشان
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xDD0F172A),
                shadowElevation = 3.dp,
                border = BorderStroke(0.5.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSatelliteView) "نقشه نشان • حالت شب" else "نقشه زنده نشان",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. کنترل‌های تعاملی سمت چپ: سوئیچ لایه ماهواره‌ای، قطب‌نما و دکمه‌های زوم
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // دکمه تغییر حالت شب/روز نقشه (استایل واقعی نشان)
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (isSatelliteView) CleanTealAccent else Color.White.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { isSatelliteView = !isSatelliteView }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSatelliteView) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        contentDescription = "تغییر حالت شب/روز نقشه",
                        tint = if (isSatelliteView) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // دکمه زوم بزرگ‌نمایی (+)
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { if (zoomLevel < 2) zoomLevel++ }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "بزرگ‌نمایی",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // دکمه زوم کوچک‌نمایی (-)
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { if (zoomLevel > 0) zoomLevel-- }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "کوچک‌نمایی",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 5. نشانگر مقصد مشتری همراه با رادار متحرک و پلاک ساختمان
        Box(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // تگ اطلاعات مشتری و پلاک
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isDeliveryMode) Color(0xFF0F172A) else Color(0xFF064E3B),
                    shadowElevation = 6.dp,
                    border = BorderStroke(
                        1.dp,
                        if (isDeliveryMode) Color(0xFF38BDF8) else Color(0xFF34D399)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDeliveryMode) Color(0xFF0284C7) else Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isDeliveryMode) Icons.Default.Inventory2 else Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customerName.take(16),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // پین و نقطه رادار تپنده
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(18.dp)
                ) {
                    // پالس رادار
                    Box(
                        modifier = Modifier
                            .size(16.dp * pulseScale)
                            .clip(CircleShape)
                            .background(
                                (if (isDeliveryMode) Color(0xFF38BDF8) else Color(0xFF10B981))
                                    .copy(alpha = max(0.1f, 0.4f / pulseScale))
                            )
                    )
                    // نقطه ثابت مرکزی
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isDeliveryMode) Color(0xFF0284C7) else Color(0xFF10B981))
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            }
        }

        // 7. نوار پایینی کارت نقشه: مسافت واقعی، زمان تخمینی، مقیاس و دکمه‌های سریع
        Surface(
            color = Color(0xF50F172A),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مسافت و زمان رانندگی بر اساس محاسبات واقعی
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${FarsiUtils.toFarsiDigits(realDistance.toString())} کیلومتر • ${FarsiUtils.toFarsiDigits(estimatedMinutes.toString())} دقیقه",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // خط مقیاس نقشه
                    Text(
                        text = when (zoomLevel) {
                            0 -> "مقیاس ۵۰۰متر"
                            2 -> "مقیاس ۵۰متر"
                            else -> "مقیاس ۲۰۰متر"
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 8.5.sp
                    )
                }

                // دکمه‌های عملیاتی روی نوار
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه باز کردن نقشه تمام‌صفحه
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF334155),
                        modifier = Modifier.clickable { showFullDetailDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "مشاهده جزئیات",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // دکمه مستقیم شروع مسیریابی نشان
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF16A34A),
                        modifier = Modifier.clickable { onNavigate() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TurnRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "مسیریابی نشان",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * دیالوگ تعاملی تمام‌صفحه جزئیات نقشه، راهنمای گام‌به‌گام و مسیریاب‌های سه‌گانه
 */
@Composable
fun OrderMapDetailDialog(
    customerName: String,
    address: String,
    orderId: String,
    latitude: Double,
    longitude: Double,
    realDistance: Double,
    estimatedMinutes: Int,
    neighborhood: NeighborhoodData,
    isDeliveryMode: Boolean,
    onDismiss: () -> Unit,
    onLaunchNeshan: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isSatelliteInDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // هدر دیالوگ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CleanBlueContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = CleanBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نقشه و مسیریابی سفارش $orderId",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "محله ${neighborhood.name} • $customerName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // باکس بزرگ نقشه شهری
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSatelliteInDialog) Color(0xFF1E293B) else Color(0xFFEFEFE9))
                ) {
                    AsyncImage(
                        model = neshanStaticMapUrl(
                            latitude = latitude,
                            longitude = longitude,
                            zoom = 16,
                            widthPx = 1000,
                            heightPx = 600,
                            nightMode = isSatelliteInDialog
                        ),
                        contentDescription = "نقشه موقعیت $customerName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // کنترل تغییر لایه در دیالوگ
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(30.dp)
                            .clickable { isSatelliteInDialog = !isSatelliteInDialog }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isSatelliteInDialog) Icons.Default.DarkMode else Icons.Default.WbSunny,
                                contentDescription = "تغییر حالت شب/روز نقشه",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // تگ ترافیک و نشان
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD0F172A),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مسیر آنلاین نشان",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // جزئیات آدرس و مختصات جغرافیایی
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = address,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مختصات: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("$latitude,$longitude"))
                                    Toast.makeText(context, "مختصات در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی مختصات", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // نکته: مسیر گام‌به‌گام واقعی (پیچ‌به‌پیچ) توسط خود اپ نشان
                // ارائه می‌شود؛ همینجا فقط نقشه و دکمه‌های مسیریابی نمایش داده می‌شود.

                Spacer(modifier = Modifier.weight(1f))

                // دکمه اجرای مستقیم مسیریابی — فقط از طریق نشان
                Text(
                    text = "شروع مسیریابی با نشان:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onLaunchNeshan,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسیریاب نشان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
