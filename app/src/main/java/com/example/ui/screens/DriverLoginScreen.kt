package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SupabaseConfigDialog
import com.example.ui.theme.*
import com.example.utils.FarsiUtils

@Composable
fun DriverLoginScreen(
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onResetOtp: () -> Unit,
    otpSent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    generatedOtpHint: String? = null,
    serverUrl: String = "",
    driverApiKey: String = "",
    workshopName: String = com.example.data.WorkshopNameHolder.current,
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    onTestConnection: (url: String, apiKey: String) -> Unit = { _, _ -> },
    onSaveServerConfig: (url: String, apiKey: String) -> Unit = { _, _ -> }
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Supabase Manual Configuration Modal triggered by long-pressing on the banner
    if (showConfigDialog) {
        SupabaseConfigDialog(
            initialUrl = serverUrl,
            initialApiKey = driverApiKey,
            isTesting = isTestingConnection,
            testResult = connectionTestResult,
            onDismiss = { showConfigDialog = false },
            onTestConnection = onTestConnection,
            onSaveConfig = { url, key ->
                onSaveServerConfig(url, key)
                showConfigDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanLightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Main Brand Luxury Banner Image with Long-Press Action for Supabase & API Key Config
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showConfigDialog = true
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.yas_electric_banner_1787716844702),
                        contentDescription = "بنر گروه مهندسی یاس الکتریک",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )

                    // Multi-layer Gradient overlay for crystal-clear readability
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CleanGreenPrimary
                            ) {
                                Text(
                                    text = "سامانه اختصاصی",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "گروه مهندسی یاس الکتریک",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نرم‌افزار هوشمند اتوماسیون قالیشویی و ناوبری سفیران",
                            color = Color(0xFFE2F6EE),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Login Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!otpSent) "ورود سفیر قالیشویی" else "تأیید شماره تلفن همراه",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanLightOnSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (!otpSent)
                            "جهت دسترسی به ماموریت‌ها، شماره موبایل خود را وارد کنید"
                        else
                            "کد ارسال‌شده به شماره ${FarsiUtils.toFarsiDigits(phoneNumber)} را وارد فرمایید",
                        fontSize = 12.sp,
                        color = CleanLightOnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!otpSent) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("شماره موبایل سفیر") },
                            placeholder = { Text("۰۹۱۲۱۲۳۴۵۶۷") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CleanGreenPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CleanGreenPrimary,
                                focusedLabelColor = CleanGreenPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onSendOtp(phoneNumber) },
                            enabled = phoneNumber.length >= 10 && !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("دریافت کد تأیید ورود", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("کد تأیید پیامک‌شده") },
                            placeholder = { Text("۱۲۳۴") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CleanGreenPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CleanGreenPrimary,
                                focusedLabelColor = CleanGreenPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onVerifyOtp(phoneNumber, otpCode) },
                            enabled = otpCode.length >= 4 && !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ورود به سامانه رانندگان", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(onClick = onResetOtp) {
                            Text("تغییر شماره همراه", color = CleanGreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CleanRedContainer
                        ) {
                            Text(
                                text = errorMessage,
                                color = CleanRedError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Engineering & Support Footer Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "طراحی و پشتیبانی: گروه مهندسی یاس الکتریک",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanGreenPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📧 yas.electrical.co@gmail.com  |  📞 ۰۹۰۱۱۹۹۰۰۱۴",
                        fontSize = 11.sp,
                        color = CleanLightOnSurfaceMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
