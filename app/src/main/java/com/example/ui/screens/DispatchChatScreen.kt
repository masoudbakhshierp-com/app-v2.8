package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ChatMessageEntity
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import kotlinx.coroutines.launch

@Composable
fun DispatchChatScreen(
    messages: List<ChatMessageEntity>,
    onSendMessage: (text: String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickMacros = listOf(
        "رسیدم به آدرس مشتری",
        "مشتری پاسخگوی تلفن نیست",
        "آدرس مشتری اشتباه است",
        "تخفیف مشتری نیاز به تایید دارد",
        "فرش‌ها به کارگاه منتقل شد"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Dispatch Support Header Card with Quick Call Action
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, CleanLightOutline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CleanGreenPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = CleanGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مرکز دیسپچ و پشتیبانی ناوگان",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanLightOnSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "اپراتورهای پشتیبانی آنلاین هستند",
                                fontSize = 11.sp,
                                color = CleanLightOnSurfaceMuted
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CleanGreenPrimaryLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:02191000000"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تماس اضطراری", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanGreenPrimary)
                    }
                }
            }
        }

        // 2. Chat Messages Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "جهت ارتباط با دیسپچ پیام خود را بنویسید یا از پیام‌های سریع استفاده کنید.",
                            color = CleanLightOnSurfaceMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }
            }
        }

        // 3. Quick Macro Pills
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .heightIn(max = 40.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickMacros.forEach { macro ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CleanLightBackground,
                            border = BorderStroke(1.dp, CleanLightOutline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSendMessage(macro)
                                }
                        ) {
                            Text(
                                text = macro,
                                fontSize = 11.sp,
                                color = CleanLightOnSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("متن پیام برای دیسپچ...", fontSize = 12.sp, color = CleanLightOnSurfaceMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CleanGreenPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "ارسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessageEntity) {
    val isDriver = message.sender.equals("DRIVER", ignoreCase = true) || message.sender.equals("me", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isDriver) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isDriver) 4.dp else 16.dp,
                bottomEnd = if (isDriver) 16.dp else 4.dp
            ),
            color = if (isDriver) CleanGreenPrimary else Color.White,
            border = if (isDriver) null else BorderStroke(1.dp, CleanLightOutline),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isDriver) "سفیر قالیشویی" else "پشتیبانی دیسپچ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDriver) Color(0xFFD1FAE5) else CleanGreenPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message.messageText,
                    fontSize = 13.sp,
                    color = if (isDriver) Color.White else CleanLightOnSurface
                )
            }
        }
    }
}
