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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.data.model.ScanStage
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.RackAssignmentDialog
import com.example.ui.theme.*
import com.example.utils.FarsiUtils

@Composable
fun WarehouseHandoverScreen(
    orders: List<OrderWithItems>,
    isPrinting: Boolean = false,
    onConfirmWarehouseHandover: (orderId: String, rackCode: String) -> Unit,
    onPrintWarehouseReceipt: (OrderWithItems) -> Unit = {},
    onOpenScanner: (orderId: String) -> Unit = {},
    onLoadCarpetIntoVan: (orderId: String) -> Unit = {}
) {
    // 1. Inbound to warehouse: collected from customers, waiting for rack assignment
    val inboundOrders = orders.filter {
        it.order.status == "COLLECTED_IN_INSPECTION" ||
                (it.order.orderType == "COLLECTION" && it.order.status == "ASSIGNED" && it.items.isNotEmpty())
    }

    // 2. Outbound from warehouse: washed carpets waiting to be loaded onto van
    // for delivery — شامل سفارش‌های برگشتی از تحویل ناموفق هم می‌شود
    // (RETURNED_TO_CLEAN_WAREHOUSE) تا راننده بتواند دوباره آن‌ها را برای
    // تلاش مجدد تحویل بارگیری کند؛ قبلاً این دسته اصلاً اینجا دیده نمی‌شد و
    // بعد از برگشت به انبار، سفارش عملاً از مسیر کاری راننده گم می‌شد.
    val outboundOrders = orders.filter {
        it.order.status == "DELIVERED_TO_WORKSHOP" || it.order.status == "WASHING" ||
                it.order.status == "READY_FOR_DELIVERY" || it.order.status == "RETURNED_TO_CLEAN_WAREHOUSE"
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Inbound (تحویل به کارگاه), 1: Outbound (بارگیری از انبار)
    var selectedOrderForRack by remember { mutableStateOf<OrderWithItems?>(null) }
    var showScanModalForVerification by remember { mutableStateOf(false) }
    // پیش‌نمایش رسید انباردار — قبلاً onPrintWarehouseReceipt اصلاً به هیچ
    // دکمه‌ای وصل نبود (نه مستقیم نه با پیش‌نمایش)؛ حالا بعد از تخصیص قفسه
    // پیش‌نمایش رسید نشان داده می‌شود و چاپ فقط با تایید راننده انجام می‌شود.
    var receiptPreviewOrder by remember { mutableStateOf<OrderWithItems?>(null) }

    val activeRackOrder = selectedOrderForRack
    if (activeRackOrder != null) {
        RackAssignmentDialog(
            orderId = activeRackOrder.order.id,
            currentRackCode = activeRackOrder.order.rackCode,
            onDismiss = { selectedOrderForRack = null },
            onConfirm = { rack ->
                onConfirmWarehouseHandover(activeRackOrder.order.id, rack)
                selectedOrderForRack = null
                receiptPreviewOrder = activeRackOrder
            }
        )
    }

    val previewOrder = receiptPreviewOrder
    if (previewOrder != null) {
        com.example.ui.components.ReceiptPreviewDialog(
            title = "رسید تحویل و نگهداری انباردار",
            orderWithItems = previewOrder,
            paymentMethodLabel = "در حال شست‌وشو - بدون نیاز به دریافت وجه",
            isPrinting = isPrinting,
            onDismiss = { receiptPreviewOrder = null },
            onPrintConfirm = {
                onPrintWarehouseReceipt(previewOrder)
                receiptPreviewOrder = null
            }
        )
    }

    if (showScanModalForVerification) {
        BarcodeScannerModal(
            expectedOrder = null,
            allOrders = if (selectedTab == 0) inboundOrders else outboundOrders,
            scanStage = if (selectedTab == 0) ScanStage.WORKSHOP else ScanStage.DELIVERY,
            onDismiss = { showScanModalForVerification = false },
            onConfirmVerification = { result ->
                showScanModalForVerification = false
                val matched = result.orderWithItems
                if (selectedTab == 0) {
                    selectedOrderForRack = matched
                } else {
                    onLoadCarpetIntoVan(matched.order.id)
                }
            },
            onReportMismatchToDispatch = { showScanModalForVerification = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: تحویل به انبار (Inbound)
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
                                text = FarsiUtils.toFarsiDigits(inboundOrders.size.toString()),
                                fontSize = 24.sp,
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
                                    Icons.Default.Input,
                                    contentDescription = null,
                                    tint = CleanGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "فرش‌های ورودی به انبار",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card 2: آماده بارگیری (Outbound)
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
                                text = FarsiUtils.toFarsiDigits(outboundOrders.size.toString()),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Icon(
                                Icons.Default.Output,
                                contentDescription = null,
                                tint = CleanGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "فرش‌های خروجی تمیز",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Tab Navigation
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
                    // Inbound Tab
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
                                text = "تحویل به کارگاه (${FarsiUtils.toFarsiDigits(inboundOrders.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else CleanLightOnSurfaceMuted
                            )
                        }
                    }

                    // Outbound Tab
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
                                text = "بارگیری از انبار (${FarsiUtils.toFarsiDigits(outboundOrders.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else CleanLightOnSurfaceMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Scanner Barcode Action
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CleanGreenPrimaryLight,
                border = BorderStroke(1.dp, CleanGreenAccent.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showScanModalForVerification = true }
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CleanGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (selectedTab == 0) "اسکن سریع بارکد فرش جهت قفسه‌گذاری" else "اسکن بارکد جهت بارگیری و تطابق",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanGreenPrimaryDark
                            )
                            Text(
                                text = "دوربین اسکنر بارکد منگنه",
                                fontSize = 11.sp,
                                color = CleanLightOnSurfaceMuted
                            )
                        }
                    }

                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 4. Warehouse Orders List
        val currentOrders = if (selectedTab == 0) inboundOrders else outboundOrders
        if (currentOrders.isEmpty()) {
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
                            Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedTab == 0) "هیچ فرشی در انتظار تحویل به انبار نیست." else "هیچ فرشی در انتظار بارگیری نمی‌باشد.",
                            color = CleanLightOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(currentOrders, key = { it.order.id }) { item ->
                WarehouseOrderUnifiedCard(
                    orderWithItems = item,
                    isInbound = selectedTab == 0,
                    onAssignRack = { selectedOrderForRack = item },
                    onLoadVan = { onLoadCarpetIntoVan(item.order.id) }
                )
            }
        }
    }
}

@Composable
private fun WarehouseOrderUnifiedCard(
    orderWithItems: OrderWithItems,
    isInbound: Boolean,
    onAssignRack: () -> Unit,
    onLoadVan: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val totalArea = items.sumOf { it.areaSqMeter }
    val itemCount = if (items.isNotEmpty()) items.size else 1

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
                    color = if (order.status == "RETURNED_TO_CLEAN_WAREHOUSE") Color(0xFFFFF3E0) else CleanGreenPrimaryLight
                ) {
                    val isReturned = order.status == "RETURNED_TO_CLEAN_WAREHOUSE"
                    val rackLabel = if (isReturned) order.cleanRackCode else order.rackCode
                    Text(
                        text = when {
                            isReturned && rackLabel.isNotBlank() -> "برگشتی • قفسه انبار تمیز: $rackLabel"
                            isReturned -> "برگشتی از تحویل (عدم حضور مشتری)"
                            rackLabel.isNotBlank() -> "قفسه: $rackLabel"
                            else -> "فاقد قفسه"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReturned) Color(0xFFB45309) else CleanGreenPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${FarsiUtils.toFarsiDigits(itemCount.toString())} تخته فرش (${FarsiUtils.toFarsiDigits(String.format("%.1f", totalArea))} مترمربع)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanLightOnSurface
                )

                Text(
                    text = order.address.take(25) + if (order.address.length > 25) "..." else "",
                    fontSize = 11.sp,
                    color = CleanLightOnSurfaceMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = if (isInbound) onAssignRack else onLoadVan,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    if (isInbound) Icons.Default.QrCodeScanner else Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isInbound) "تخصیص قفسه و ثبت در کارگاه"
                        else if (order.status == "RETURNED_TO_CLEAN_WAREHOUSE") "بارگیری مجدد جهت تلاش دوباره تحویل"
                        else "تأیید بارگیری در خودرو",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
