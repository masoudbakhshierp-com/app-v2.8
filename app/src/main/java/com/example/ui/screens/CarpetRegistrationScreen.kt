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
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.model.OrderWithItems
import com.example.ui.components.AddCarpetItemDialog
import com.example.ui.components.BarcodeView
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.theme.*
import com.example.utils.FarsiUtils

@Composable
fun CarpetRegistrationScreen(
    orderWithItems: OrderWithItems?,
    isPrinting: Boolean,
    tariffSyncResult: com.example.data.model.TariffSyncResult = com.example.data.model.TariffSyncResult.createDefault(),
    onRefreshTariffs: () -> Unit = {},
    onBack: () -> Unit = {},
    onAddCarpetItem: (
        carpetType: String,
        length: Double,
        width: Double,
        unitPrice: Long,
        services: List<String>,
        defects: List<String>,
        notes: String,
        barcodeTag: String
    ) -> Unit,
    onDeleteCarpetItem: (itemId: Long) -> Unit,
    onPrintReceipt: () -> Unit,
    onProceedToWorkshop: () -> Unit
) {
    if (orderWithItems == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CleanLightBackground),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("لطفاً ابتدا یک سفارش را از لیست جمع‌آوری انتخاب کنید.", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary)
                    ) {
                        Text("بازگشت به لیست جمع‌آوری")
                    }
                }
            }
        }
        return
    }

    val order = orderWithItems.order
    val items = orderWithItems.items
    val totalArea = items.sumOf { it.areaSqMeter }
    val totalEstimatedPrice = items.sumOf { it.totalPrice }

    var showAddDialog by remember { mutableStateOf(false) }
    var showReceiptPreview by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddCarpetItemDialog(
            orderId = order.id,
            tariffSyncResult = tariffSyncResult,
            onRefreshTariffs = onRefreshTariffs,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, len, wid, price, servs, defs, notes, tag ->
                onAddCarpetItem(type, len, wid, price, servs, defs, notes, tag)
            }
        )
    }

    if (showReceiptPreview) {
        ReceiptPreviewDialog(
            title = "پیش‌فاکتور اولیه دریافت فرش در محل مشتری",
            orderWithItems = orderWithItems,
            paymentMethodLabel = "پیش‌فاکتور اولیه - در انتظار شستشو",
            isPrinting = isPrinting,
            onDismiss = { showReceiptPreview = false },
            onPrintConfirm = {
                onPrintReceipt()
                showReceiptPreview = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // 1. Customer & Order Header Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, CleanLightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CleanGreenPrimaryLight,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onBack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "بازگشت",
                                        tint = CleanGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "صدور پیش‌فاکتور و ثبت اقلام",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CleanLightOnSurface
                                )
                                Text(
                                    text = "مشتری: ${order.customerName} • ${FarsiUtils.toFarsiDigits(order.customerPhone)}",
                                    fontSize = 11.sp,
                                    color = CleanLightOnSurfaceMuted
                                )
                            }
                        }

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
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CleanLightOutline)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Step indicator row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepItem(number = "۱", title = "انتخاب مشتری", isDone = true, isActive = false)
                        StepDivider(isDone = true)
                        StepItem(number = "۲", title = "ثبت اقلام فرش", isDone = items.isNotEmpty(), isActive = items.isEmpty())
                        StepDivider(isDone = items.isNotEmpty())
                        StepItem(number = "۳", title = "چاپ پیش‌فاکتور", isDone = false, isActive = items.isNotEmpty())
                    }
                }
            }
        }

        // 2. Financial & Area Calculation Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Area Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${FarsiUtils.toFarsiDigits(String.format("%.1f", totalArea))}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Text("مساحت کل (مترمربع)", fontSize = 11.sp, color = CleanLightOnSurfaceMuted, fontWeight = FontWeight.Medium)
                    }
                }

                // Total Estimated Amount Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FarsiUtils.formatPriceShort(totalEstimatedPrice),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Icon(Icons.Default.Payments, contentDescription = null, tint = CleanGreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Text("مبلغ برآوردی (تومان)", fontSize = 11.sp, color = CleanLightOnSurfaceMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // 3. Section Title & Add Item CTA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست فرش‌های ثبت‌شده (${FarsiUtils.toFarsiDigits(items.size.toString())} تخته):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanLightOnSurface
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن تخته فرش جدید", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Carpet Items List
        if (items.isEmpty()) {
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
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "هنوز هیچ تخته فرشی برای این فاکتور ثبت نشده است.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "با دکمه بالا اقلام دریافتی را همراه با ابعاد و بارکد منگنه ثبت نمایید.",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                CarpetItemUnifiedCard(
                    item = item,
                    onDelete = { onDeleteCarpetItem(item.id) }
                )
            }
        }

        // 5. Final Action Buttons: Print Pre-Invoice & Finalize
        if (items.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Finalize & proceed to workshop button
                    Button(
                        onClick = onProceedToWorkshop,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأیید نهایی و تحویل به انبار", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Print receipt button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CleanGreenPrimaryLight,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showReceiptPreview = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "چاپ پیش‌فاکتور",
                                tint = CleanGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarpetItemUnifiedCard(
    item: CarpetItemEntity,
    onDelete: () -> Unit
) {
    val area = item.lengthMeter * item.widthMeter

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, CleanLightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Type + Tag and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanGreenPrimary
                    ) {
                        Text(
                            text = item.carpetType,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.barcodeTag.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanGreenPrimaryLight
                        ) {
                            Text(
                                text = "بارکد: ${item.barcodeTag}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanGreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف قلم", tint = CleanRedError, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dimensions and Calculations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ابعاد: ${FarsiUtils.toFarsiDigits(item.lengthMeter.toString())} × ${FarsiUtils.toFarsiDigits(item.widthMeter.toString())} متر (${FarsiUtils.toFarsiDigits(String.format("%.1f", area))} م²)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanLightOnSurface
                )

                Text(
                    text = "${FarsiUtils.formatPrice(item.totalPrice)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanGreenPrimary
                )
            }

            // Services or Defects if present
            if (item.requestedServicesJson.isNotBlank() || item.defectsJson.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CleanLightBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (item.requestedServicesJson.isNotBlank()) {
                            Text("خدمات درخواستی: ${item.requestedServicesJson}", fontSize = 11.sp, color = CleanGreenPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        if (item.defectsJson.isNotBlank()) {
                            Text("ایرادات اولیه فرش: ${item.defectsJson}", fontSize = 11.sp, color = CleanWarningText)
                        }
                    }
                }
            }

            // Barcode preview
            if (item.barcodeTag.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BarcodeView(code = item.barcodeTag, modifier = Modifier.fillMaxWidth().height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    title: String,
    isDone: Boolean,
    isActive: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> CleanGreenPrimary
                        isActive -> CleanOrangeAccent
                        else -> CleanLightOutline
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = FarsiUtils.toFarsiDigits(number),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else CleanLightOnSurfaceMuted
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isDone) CleanLightOnSurface else CleanLightOnSurfaceMuted
        )
    }
}

@Composable
private fun StepDivider(isDone: Boolean) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(if (isDone) CleanGreenPrimary else CleanLightOutline)
    )
}
