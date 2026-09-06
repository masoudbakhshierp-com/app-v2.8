package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.GpsLogEntity
import com.example.ui.theme.*
import com.example.utils.FarsiUtils

@Composable
fun GpsTrackingScreen(
    isGpsActive: Boolean,
    unsyncedCount: Int,
    isSyncing: Boolean,
    recentGpsLogs: List<GpsLogEntity>,
    onToggleGps: () -> Unit,
    onSyncNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanLightBackground)
            .padding(16.dp)
    ) {
        // Live GPS Status Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, CleanLightOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isGpsActive) Color(0xFF10B981) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isGpsActive) "ارسال موقعیت زنده (Live GPS) فعال است" else "ردیابی موقعیت خاموش می‌باشد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CleanLightOnSurface
                        )
                    }
                    Switch(
                        checked = isGpsActive,
                        onCheckedChange = { onToggleGps() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CleanGreenPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "مختصات جغرافیایی راننده هر ۳۰ ثانیه برای نمایش زنده روی نقشه مدیر در پنل وب ارسال می‌شود.",
                    fontSize = 11.sp,
                    color = CleanLightOnSurfaceMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Offline Sync Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, CleanLightOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مدیریت آفلاین و همگام‌سازی", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CleanLightOnSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (unsyncedCount > 0) "$unsyncedCount تغییر در انتظار ارسال به سرور" else "تمام داده‌ها با سرور همگام است",
                            fontSize = 11.sp,
                            color = if (unsyncedCount > 0) CleanOrangeAccent else CleanGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("همگام‌سازی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "آخرین نقاط ثبت‌شده موقعیت مکانی:",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = CleanLightOnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (recentGpsLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("هنوز هیچ موقعیت مکانی ثبت نشده است.", color = CleanLightOnSurfaceMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentGpsLogs) { log ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, CleanLightOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "عرض: ${String.format("%.4f", log.latitude)} ، طول: ${String.format("%.4f", log.longitude)}",
                                    fontSize = 12.sp,
                                    color = CleanLightOnSurface
                                )
                            }
                            Text(
                                text = if (log.isSynced) "ارسال شده" else "در صف",
                                fontSize = 11.sp,
                                color = if (log.isSynced) CleanGreenPrimary else CleanOrangeAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
