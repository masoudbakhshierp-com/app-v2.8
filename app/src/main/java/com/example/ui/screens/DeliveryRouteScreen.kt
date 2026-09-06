package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as ListAllIcon
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.data.model.ScanStage
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.RealisticOrderMapPreview
import com.example.ui.components.ReturnToCleanWarehouseDialog
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryRouteScreen(
    orders: List<OrderWithItems>,
    onSelectOrderForSettlement: (OrderWithItems) -> Unit,
    onOpenScanner: (orderId: String) -> Unit,
    onReturnToCleanWarehouse: (orderId: String, cleanRackCode: String, reason: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    // Filter orders ready for delivery
    val deliveryOrders = orders.filter {
        (it.order.status == "READY_FOR_DELIVERY" ||
                (it.order.orderType == "DELIVERY" && it.order.status == "ASSIGNED")) &&
                it.order.status != "DELIVERED_SETTLED" &&
                it.order.status != "OFFICE_SETTLED" &&
                it.order.status != "RETURNED_TO_CLEAN_WAREHOUSE" &&
                it.order.status != "DELIVERED_TO_WORKSHOP" &&
                it.order.status != "COLLECTED_IN_INSPECTION" &&
                it.order.status != "WASHING"
    }

    val deliveredTodayCount = orders.count { it.order.status == "DELIVERED_SETTLED" }
    val totalTodayInvoices = deliveryOrders.size + deliveredTodayCount

    var selectedOrderForMap by remember(deliveryOrders) {
        mutableStateOf<OrderWithItems?>(deliveryOrders.firstOrNull())
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, READY
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDeliverySearchScanner by remember { mutableStateOf(false) }
    var orderForCleanWarehouseReturn by remember { mutableStateOf<OrderWithItems?>(null) }

    if (showDeliverySearchScanner) {
        BarcodeScannerModal(
            expectedOrder = null,
            allOrders = deliveryOrders,
            scanStage = ScanStage.DELIVERY,
            onDismiss = { showDeliverySearchScanner = false },
            onConfirmVerification = { result ->
                showDeliverySearchScanner = false
                val matchedOrder = result.orderWithItems
                searchQuery = matchedOrder.order.id
                onSelectOrderForSettlement(matchedOrder)
            },
            onReportMismatchToDispatch = { showDeliverySearchScanner = false }
        )
    }

    val activeReturnOrder = orderForCleanWarehouseReturn
    if (activeReturnOrder != null) {
        ReturnToCleanWarehouseDialog(
            orderId = activeReturnOrder.order.id,
            customerName = activeReturnOrder.order.customerName,
            currentRackCode = activeReturnOrder.order.rackCode,
            onDismiss = { orderForCleanWarehouseReturn = null },
            onConfirm = { cleanRackCode, reason ->
                onReturnToCleanWarehouse(activeReturnOrder.order.id, cleanRackCode, reason)
                orderForCleanWarehouseReturn = null
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فیلتر فاکتورهای تحویل", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
                HorizontalDivider(color = CleanLightOutline)

                FilterOptionRow(
                    title = "همه فاکتورها",
                    count = deliveryOrders.size,
                    isSelected = selectedFilter == "ALL",
                    onClick = {
                        selectedFilter = "ALL"
                        showFilterSheet = false
                    }
                )

                FilterOptionRow(
                    title = "فقط فاکتورهای آماده تحویل فوری",
                    count = deliveryOrders.count { it.order.status == "READY_FOR_DELIVERY" },
                    isSelected = selectedFilter == "READY",
                    onClick = {
                        selectedFilter = "READY"
                        showFilterSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    val filteredOrders = deliveryOrders.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.order.customerName.contains(searchQuery, true) ||
                item.order.id.contains(searchQuery, true) ||
                item.order.address.contains(searchQuery, true) ||
                item.order.customerPhone.contains(searchQuery)

        val matchesFilter = when (selectedFilter) {
            "READY" -> item.order.status == "READY_FOR_DELIVERY"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. KPI Summary Cards Row (Matching Screenshot)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: فاکتورهای امروز (Right Card in RTL)
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
                                text = FarsiUtils.toFarsiDigits(totalTodayInvoices.toString()),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "فاکتورهای امروز",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanLightOnSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CleanGreenPrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CleanGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(deliveredTodayCount.toString())} تحویل شده",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card 2: در نوبت تحویل (Left Card in RTL)
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
                                text = FarsiUtils.toFarsiDigits(deliveryOrders.size.toString()),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "در نوبت تحویل",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanLightOnSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = CleanGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(deliveryOrders.size.toString())} باقیمانده",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Search & Filter Bar — فشرده و آیکون‌محور
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Field — تک‌خط و فشرده
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "جستجو...",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFFA0AEC0)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    color = CleanLightOnSurface
                                ),
                                cursorBrush = SolidColor(CleanGreenPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "پاک کردن", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Filter Button — فقط آیکون
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showFilterSheet = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "فیلترها",
                            tint = CleanLightOnSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // All Filter Pill — آیکون + شمارنده (بدون متن «همه»)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanGreenPrimaryLight,
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedFilter = "ALL"; searchQuery = "" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ListAllIcon,
                            contentDescription = "همه",
                            tint = CleanGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FarsiUtils.toFarsiDigits(deliveryOrders.size.toString()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanGreenPrimary
                        )
                    }
                }

                // View Mode / Scan Shortcut Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanGreenPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDeliverySearchScanner = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "مرتب‌سازی و اسکن",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Realistic Interactive Live Map View (Matching Screenshot)
        item {
            val activeOrder = selectedOrderForMap ?: deliveryOrders.firstOrNull()
            if (activeOrder != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        RealisticOrderMapPreview(
                            customerName = activeOrder.order.customerName,
                            address = activeOrder.order.address,
                            orderId = activeOrder.order.id,
                            latitude = activeOrder.order.latitude,
                            longitude = activeOrder.order.longitude,
                            heightDp = 200,
                            isDeliveryMode = true,
                            onNavigate = {
                                NavigationUtils.launchNeshan(
                                    context,
                                    activeOrder.order.latitude,
                                    activeOrder.order.longitude,
                                    activeOrder.order.address
                                )
                            }
                        )

                        // Top-Right: 'نقشه زنده 🟢' Pill Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanGreenPrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نقشه زنده",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Delivery Orders List
        if (filteredOrders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CleanLightOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "هنوز فاکتوری برای این بخش ثبت نشده است.",
                            color = CleanLightOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "فاکتورهای تحویل آماده از طریق سیستم دیسپچ تخصیص می‌یابند.",
                            color = CleanLightOnSurfaceMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.order.id }) { item ->
                DeliveryOrderUnifiedCard(
                    orderWithItems = item,
                    isSelected = selectedOrderForMap?.order?.id == item.order.id,
                    onCardClick = { selectedOrderForMap = item },
                    onProceedToReceiveOrDeliver = {
                        onSelectOrderForSettlement(item)
                    },
                    onCallCustomer = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.order.customerPhone}"))
                        context.startActivity(intent)
                    },
                    onNavigate = {
                        NavigationUtils.launchNeshan(context, item.order.latitude, item.order.longitude, item.order.address)
                    },
                    onReturnToWarehouse = {
                        orderForCleanWarehouseReturn = item
                    }
                )
            }
        }
    }
}

/**
 * Unified Delivery Card matching the exact visual design of the reference image
 */
@Composable
private fun DeliveryOrderUnifiedCard(
    orderWithItems: OrderWithItems,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onProceedToReceiveOrDeliver: () -> Unit,
    onCallCustomer: () -> Unit,
    onNavigate: () -> Unit,
    onReturnToWarehouse: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val totalArea = items.sumOf { it.areaSqMeter }
    val itemCount = if (items.isNotEmpty()) items.size else 1
    val areaStr = if (totalArea > 0.0) String.format("%.0f", totalArea) else "۶"
    val dimensionsStr = if (items.isNotEmpty()) "(${items.first().lengthMeter.toInt()}×${items.first().widthMeter.toInt()})" else "(۲×۳)"

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CleanGreenPrimary else CleanLightOutline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Status Tag (Left) & Customer Name + Order ID Pill (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right Side (in RTL): Customer Name + Order Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dark Green Order ID Badge
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

                    // Customer Name with User Icon
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

                // Left Side (in RTL): Soft Amber Status Pill
                val statusText = when (order.status) {
                    "READY_FOR_DELIVERY" -> "در انتظار تحویل"
                    "ASSIGNED" -> "در انتظار دریافت"
                    else -> "در انتظار دریافت"
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CleanWarningBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanWarningText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FarsiUtils.toFarsiDigits(order.customerPhone),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanLightOnSurface
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = CircleShape,
                    color = CleanGreenPrimaryLight,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onCallCustomer() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "تماس با مشتری",
                            tint = CleanGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = order.address,
                    fontSize = 13.sp,
                    color = CleanLightOnSurfaceMuted,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "آدرس",
                    tint = CleanLightOnSurfaceMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CleanLightOutline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 2-Column Metadata Info (Items/Area and Delivery Deadline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Items & Dimensions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanGreenPrimaryLight.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = CleanGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(itemCount.toString())} کالا",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(areaStr)} متر $dimensionsStr",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }
                }

                // Column 2: Date & Deadline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CleanGreenPrimaryLight.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = CleanGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "۱۴۰۳/۰۵/۱۴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        Text(
                            text = "مهلت تحویل",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Matching Screenshot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button (Deep Green): ثبت اقلام و دریافت ☑️
                Button(
                    onClick = onProceedToReceiveOrDeliver,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ثبت اقلام و دریافت",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Secondary Button 1: Call (Light Mint)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanGreenPrimaryLight,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCallCustomer() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "تماس تلفنی",
                            tint = CleanGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Secondary Button 2: Navigation (Light Mint)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanGreenPrimaryLight,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.TurnRight,
                            contentDescription = "مسیریابی نشان",
                            tint = CleanGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionRow(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CleanGreenPrimaryLight else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CleanGreenPrimary else CleanLightOnSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) CleanGreenPrimary else CleanLightSurfaceVariant
            ) {
                Text(
                    text = FarsiUtils.toFarsiDigits(count.toString()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else CleanLightOnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
