package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.model.OrderWithItems
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.ReturnToCleanWarehouseDialog
import com.example.ui.components.SettlementDialog
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@Composable
fun DeliverySettlementScreen(
    orders: List<OrderWithItems>,
    isPrinting: Boolean = false,
    onSettlePayment: (orderId: String, paidAmount: Long, discountAmount: Long, paymentMethod: String) -> Unit,
    onPrintReceipt: (OrderWithItems, String) -> Unit,
    onOpenScanner: (orderId: String) -> Unit = {},
    onSettleWithOffice: () -> Unit = {},
    onPrintDailySettlementReport: () -> Unit = {},
    onSignatureCaptured: (orderId: String, signatureData: String) -> Unit = { _, _ -> },
    onReturnToCleanWarehouse: (orderId: String, cleanRackCode: String, reason: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    // Active pending delivery orders (محذوف تسویه‌شده)
    val pendingDeliveryOrders = orders.filter {
        (it.order.status == "READY_FOR_DELIVERY" ||
                (it.order.orderType == "DELIVERY" && it.order.status == "ASSIGNED")) &&
                it.order.status != "DELIVERED_SETTLED" &&    // 🔧 صریح exclude
                it.order.status != "OFFICE_SETTLED" &&
                it.order.status != "RETURNED_TO_CLEAN_WAREHOUSE" &&
                it.order.status != "DELIVERED_TO_WORKSHOP" &&
                it.order.status != "COLLECTED_IN_INSPECTION" &&
                it.order.status != "WASHING"
    }

    // Today's settled orders
    val settledTodayOrders = orders.filter {
        it.order.status == "DELIVERED_SETTLED"
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Pending, 1: Settled Today
    var selectedOrderForSettlement by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderForCleanWarehouseReturn by remember { mutableStateOf<OrderWithItems?>(null) }
    var showOfficeSettlementDialog by remember { mutableStateOf(false) }
    // پیش‌نمایش چاپ — قبلاً چاپ رسید (چه بعد از تسویه، چه «چاپ مجدد») بدون
    // هیچ تاییدی مستقیم روی چاپگر می‌رفت. حالا هر دو مسیر اول این پیش‌نمایش
    // را نشان می‌دهند و چاپ واقعی فقط بعد از تایید صریح راننده انجام می‌شود.
    var receiptPreview by remember { mutableStateOf<Pair<OrderWithItems, String>?>(null) }

    val totalReceived = settledTodayOrders.sumOf { it.order.paidAmount }
    val pendingCollection = pendingDeliveryOrders.sumOf { maxOf(0L, (it.order.totalAmount - it.order.discountAmount - it.order.paidAmount)) }

    val cashReceived = settledTodayOrders.filter {
        it.order.paidAmount > 0 && (it.order.paymentMethod.contains("CASH", ignoreCase = true) || it.order.paymentMethod.contains("نقدی") || it.order.paymentMethod.contains("نقد"))
    }.sumOf { it.order.paidAmount }

    val posReceived = maxOf(0L, totalReceived - cashReceived)

    val activeSettlement = selectedOrderForSettlement
    if (activeSettlement != null) {
        SettlementDialog(
            orderWithItems = activeSettlement,
            onDismiss = { selectedOrderForSettlement = null },
            onConfirmSettlement = { paid, discount, method, print ->
                onSettlePayment(activeSettlement.order.id, paid, discount, method)
                if (print) {
                    receiptPreview = activeSettlement to method
                }
                selectedOrderForSettlement = null
            },
            onSignatureCaptured = onSignatureCaptured
        )
    }

    val previewPair = receiptPreview
    if (previewPair != null) {
        val (previewOrder, previewMethod) = previewPair
        ReceiptPreviewDialog(
            title = "رسید تسویه حساب و تحویل فرش",
            orderWithItems = previewOrder,
            paymentMethodLabel = previewMethod,
            isPrinting = isPrinting,
            onDismiss = { receiptPreview = null },
            onPrintConfirm = {
                onPrintReceipt(previewOrder, previewMethod)
                receiptPreview = null
            }
        )
    }

    val activeReturn = orderForCleanWarehouseReturn
    if (activeReturn != null) {
        ReturnToCleanWarehouseDialog(
            orderId = activeReturn.order.id,
            customerName = activeReturn.order.customerName,
            currentRackCode = activeReturn.order.rackCode,
            onDismiss = { orderForCleanWarehouseReturn = null },
            onConfirm = { cleanRackCode, reason ->
                onReturnToCleanWarehouse(activeReturn.order.id, cleanRackCode, reason)
                orderForCleanWarehouseReturn = null
            }
        )
    }

    if (showOfficeSettlementDialog) {
        AlertDialog(
            onDismissRequest = { showOfficeSettlementDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showOfficeSettlementDialog = false
                        onSettleWithOffice()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأیید و ثبت در سیستم حسابداری", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showOfficeSettlementDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("انصراف")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CleanGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسویه حساب با امور مالی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("جمع مبالغ دریافتی امروز شما جهت تحویل به صندوق:")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CleanGreenPrimaryLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("کارتخوان سیار: ${FarsiUtils.formatPrice(posReceived)} تومان", fontWeight = FontWeight.Bold, color = CleanGreenPrimaryDark)
                            Text("وجه نقد دریافتی: ${FarsiUtils.formatPrice(cashReceived)} تومان", fontWeight = FontWeight.Bold, color = CleanGreenPrimaryDark)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = CleanLightOutline)
                            Text("مجموع کل دریافتی: ${FarsiUtils.formatPrice(totalReceived)} تومان", fontWeight = FontWeight.ExtraBold, color = CleanGreenPrimary)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. KPI Financial Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: مجموع دریافتی امروز
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FarsiUtils.formatPriceShort(totalReceived),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CleanGreenPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = CleanGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "وصولی امروز (تومان)",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card 2: باقیمانده وصولی
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FarsiUtils.formatPriceShort(pendingCollection),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanOrangeAccent
                            )
                            Icon(
                                Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = CleanOrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "مانده در مسیر (تومان)",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Tab Selector: Pending vs Settled Today
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pending Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 0) CleanGreenPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = 0 }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "در نوبت تسویه (${FarsiUtils.toFarsiDigits(pendingDeliveryOrders.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else CleanLightOnSurfaceMuted
                            )
                        }
                    }

                    // Settled Today Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 1) CleanGreenPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = 1 }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تسویه‌شده‌های امروز (${FarsiUtils.toFarsiDigits(settledTodayOrders.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else CleanLightOnSurfaceMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Actions for Daily Settlement with Office
        if (selectedTab == 1 && settledTodayOrders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showOfficeSettlementDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسویه با دفتر مدیریت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPrintDailySettlementReport,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = CleanGreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("چاپ گزارش روزانه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanGreenPrimary)
                    }
                }
            }
        }

        // 4. Orders List
        val currentOrdersList = if (selectedTab == 0) pendingDeliveryOrders else settledTodayOrders
        if (currentOrdersList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedTab == 0) "تمامی فاکتورها تسویه شده‌اند." else "هنوز فاکتوری در شیفت امروز تسویه نشده است.",
                            color = CleanLightOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(currentOrdersList, key = { it.order.id }) { item ->
                SettlementOrderUnifiedCard(
                    orderWithItems = item,
                    isSettled = selectedTab == 1,
                    onSettle = { selectedOrderForSettlement = item },
                    onPrintReceipt = { receiptPreview = item to item.order.paymentMethod }
                )
            }
        }
    }
}

@Composable
private fun SettlementOrderUnifiedCard(
    orderWithItems: OrderWithItems,
    isSettled: Boolean,
    onSettle: () -> Unit,
    onPrintReceipt: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val totalAmount = order.totalAmount
    val discount = order.discountAmount
    val paid = order.paidAmount
    val remaining = maxOf(0L, totalAmount - discount - paid)

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
            // Header Row: Customer & Order ID + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CleanGreenPrimary
                    ) {
                        Text(
                            text = "فاکتور ${order.id}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = order.customerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanLightOnSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSettled) CleanGreenPrimaryLight else CleanWarningBg
                ) {
                    Text(
                        text = if (isSettled) "تسویه شد ☑️" else "در انتظار پرداخت",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) CleanGreenPrimary else CleanWarningText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Financial Summary Block
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CleanLightBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مبلغ کل فاکتور:", fontSize = 11.sp, color = CleanLightOnSurfaceMuted)
                        Text(
                            text = "${FarsiUtils.formatPrice(totalAmount)} تومان",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isSettled) "روش پرداخت:" else "مبلغ قابل پرداخت:",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                        Text(
                            text = if (isSettled) order.paymentMethod.ifBlank { "کارتخوان سیار" } else "${FarsiUtils.formatPrice(remaining)} تومان",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanGreenPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSettled) {
                    Button(
                        onClick = onSettle,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت تسویه و دریافت وجه", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = CleanGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("چاپ مجدد رسید تسویه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanGreenPrimary)
                    }
                }
            }
        }
    }
}
