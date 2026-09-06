package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    connectedPrinterName: String?,
    onOpenPrinterDialog: () -> Unit,
    onPrintTestReceipt: () -> Unit = {},
    onSyncNow: () -> Unit,
    savedServerUrl: String = com.example.data.remote.supabase.ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL,
    savedApiKey: String = com.example.data.remote.supabase.ZomorrodSupabaseConfig.DRIVER_API_KEY,
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    onUpdateServerUrl: (String) -> Unit = {},
    onUpdateServerConfig: (String, String) -> Unit = { _, _ -> },
    onTestConnection: (String, String) -> Unit = { _, _ -> },
    tariffSyncResult: com.example.data.model.TariffSyncResult = com.example.data.model.TariffSyncResult.createDefault(),
    onRefreshTariffs: () -> Unit = {},
    backupInfo: com.example.utils.BackupInfo? = null,
    onBackupDatabase: () -> Unit = {},
    onRestoreDatabase: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    var serverUrl by remember(savedServerUrl) { mutableStateOf(savedServerUrl) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var autoPrintReceipt by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanLightBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server Connection Card
        SettingsSectionCard(
            title = "ارتباط با سرور متمرکز Supabase",
            subtitle = "مدیریت آدرس پروژه، کلید راننده و همگام‌سازی بی‌درنگ فاکتورها",
            icon = Icons.Default.CloudSync
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        onUpdateServerConfig(it, apiKey)
                    },
                    label = { Text("آدرس Supabase URL", fontSize = 11.sp) },
                    placeholder = { Text("https://xyz.supabase.co") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CleanGreenPrimary,
                        focusedLabelColor = CleanGreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onUpdateServerConfig(serverUrl, it)
                    },
                    label = { Text("کلید ارتباطی راننده (Driver API Key)", fontSize = 11.sp) },
                    placeholder = { Text("oVKBYHRpHalUpmlYUGXOU...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CleanGreenPrimary,
                        focusedLabelColor = CleanGreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Connection Test Button
                Button(
                    onClick = { onTestConnection(serverUrl, apiKey) },
                    enabled = !isTestingConnection,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال بررسی اتصال به سرور...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست ارتباط با Supabase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!connectionTestResult.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (connectionTestResult.contains("موفق") || connectionTestResult.contains("200")) CleanGreenPrimaryLight else CleanRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = connectionTestResult,
                            color = if (connectionTestResult.contains("موفق") || connectionTestResult.contains("200")) CleanGreenPrimary else CleanRedError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Bluetooth Printer Management
        SettingsSectionCard(
            title = "چاپگر همراه و رسید حرارتی",
            subtitle = "اتصال به پرینتر کمری بلوتوثی جهت چاپ فاکتورها",
            icon = Icons.Default.Print
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("چاپگر متصل فعلی:", fontSize = 12.sp, color = CleanLightOnSurfaceMuted)
                        Text(
                            text = connectedPrinterName ?: "چاپگری متصل نیست",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (connectedPrinterName != null) CleanGreenPrimary else CleanLightOnSurfaceMuted
                        )
                    }

                    Button(
                        onClick = onOpenPrinterDialog,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary)
                    ) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جستجوی بلوتوث", fontSize = 12.sp)
                    }
                }

                if (connectedPrinterName != null) {
                    OutlinedButton(
                        onClick = onPrintTestReceipt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = CleanGreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("چاپ فاکتور تستی", fontSize = 12.sp, color = CleanGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Appearance & Dark Mode
        SettingsSectionCard(
            title = "ظاهر برنامه و حالت تاریک",
            subtitle = "شخصی‌سازی رابط کاربری و روشنایی صفحه",
            icon = Icons.Default.DarkMode
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("حالت شب (تم تاریک)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanLightOnSurface)
                    Text("کاهش مصرف باتری و نور صفحه در شیفت شب", fontSize = 11.sp, color = CleanLightOnSurfaceMuted)
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CleanGreenPrimary
                    )
                )
            }
        }

        // Database Backup & Sync
        SettingsSectionCard(
            title = "پشتیبان‌گیری محلی و دیتابیس",
            subtitle = "حفظ اطلاعات فاکتورها در حافظه آفلاین دستگاه",
            icon = Icons.Default.Storage
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBackupDatabase,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پشتیبان‌گیری", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRestoreDatabase,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = CleanGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بازیابی", fontSize = 12.sp, color = CleanGreenPrimary)
                }
            }
        }

        // Logout
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CleanRedContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, CleanRedError.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("خروج از حساب سفیر", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CleanRedError)
                    Text("پایان شیفت کاری و بستن سشن", fontSize = 11.sp, color = CleanLightOnSurfaceMuted)
                }

                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanRedError)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروج", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, CleanLightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanGreenPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanLightOnSurface)
                    Text(text = subtitle, fontSize = 11.sp, color = CleanLightOnSurfaceMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CleanLightOutline)
            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}
