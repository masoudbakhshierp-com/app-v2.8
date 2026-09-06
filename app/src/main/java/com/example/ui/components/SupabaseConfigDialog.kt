package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.supabase.ZomorrodSupabaseConfig
import com.example.ui.theme.*

@Composable
fun SupabaseConfigDialog(
    initialUrl: String,
    initialApiKey: String,
    isTesting: Boolean,
    testResult: String?,
    onDismiss: () -> Unit,
    onTestConnection: (url: String, apiKey: String) -> Unit,
    onSaveConfig: (url: String, apiKey: String) -> Unit
) {
    var urlText by remember { mutableStateOf(initialUrl.ifBlank { ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL }) }
    var apiKeyText by remember { mutableStateOf(initialApiKey.ifBlank { ZomorrodSupabaseConfig.DRIVER_API_KEY }) }
    var showSuccessSaved by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, CleanLightOutline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CleanGreenPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = CleanGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "تنظیمات اتصال Supabase",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanLightOnSurface
                            )
                            Text(
                                text = "پیکربندی دستی سرور و کلید راننده",
                                fontSize = 11.sp,
                                color = CleanLightOnSurfaceMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CleanLightOutline)
                Spacer(modifier = Modifier.height(16.dp))

                // Guide Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanGreenPrimaryLight.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, CleanGreenAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = CleanGreenPrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "با وارد کردن آدرس پروژه و کلید امنیتی (driver-api-key)، ارتباط راننده با سرور و پیامک OTP برقرار می‌شود.",
                            fontSize = 11.sp,
                            color = CleanGreenPrimaryDark,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 1: Supabase URL
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آدرس پروژه (Supabase URL)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    urlText = clip.trim()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("چسباندن", fontSize = 11.sp, color = CleanGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        placeholder = { Text("https://xyz.supabase.co", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (urlText.isNotBlank()) {
                                IconButton(onClick = { urlText = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            textDirection = TextDirection.Ltr
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanGreenPrimary,
                            unfocusedBorderColor = CleanLightOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Driver API Key
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کلید راننده (driver-api-key)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    apiKeyText = clip.trim()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("چسباندن", fontSize = 11.sp, color = CleanGreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        placeholder = { Text("oVKBYHRpHalUpmlYUGXOU...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (apiKeyText.isNotBlank()) {
                                IconButton(onClick = { apiKeyText = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            textDirection = TextDirection.Ltr
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanGreenPrimary,
                            unfocusedBorderColor = CleanLightOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Button & Result Feedback
                OutlinedButton(
                    onClick = { onTestConnection(urlText.trim(), apiKeyText.trim()) },
                    enabled = urlText.isNotBlank() && !isTesting,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CleanGreenPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanGreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = CleanGreenPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال بررسی اتصال به سرور...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تست ارتباط آنلاین با سرور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!testResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isSuccess = testResult.contains("موفق") || testResult.contains("200") || testResult.contains("OK")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccess) CleanGreenPrimaryLight else CleanRedContainer,
                        border = BorderStroke(1.dp, if (isSuccess) CleanGreenPrimary.copy(alpha = 0.3f) else CleanRedError.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) CleanGreenPrimary else CleanRedError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testResult,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSuccess) CleanGreenPrimaryDark else CleanRedError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = CleanLightOutline)
                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Reset / Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            urlText = ZomorrodSupabaseConfig.DEFAULT_SUPABASE_URL
                            apiKeyText = ZomorrodSupabaseConfig.DRIVER_API_KEY
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "پیش‌فرض",
                            fontSize = 12.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }

                    Button(
                        onClick = {
                            onSaveConfig(urlText.trim(), apiKeyText.trim())
                            onDismiss()
                        },
                        enabled = urlText.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ذخیره و اعمال",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
