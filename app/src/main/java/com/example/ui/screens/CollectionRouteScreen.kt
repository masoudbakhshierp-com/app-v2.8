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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionRouteScreen(
    orders: List<OrderWithItems>,
    onSelectOrderForInvoice: (OrderWithItems) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, REGISTERED
    var showFilterSheet by remember { mutableStateOf(false) }

    // Filter pickup / collection orders
    val pickupOrders = orders.filter {
        val isPickup = it.order.orderType.equals("COLLECTION", ignoreCase = true) ||
                       it.order.orderType.equals("PICKUP", ignoreCase = true) ||
                       it.order.orderType.isBlank()
        isPickup && (it.order.status == "ASSIGNED" || it.order.status == "pickup_assigned" || it.order.status == "COLLECTED_IN_INSPECTION")
    }

    val pendingCount = pickupOrders.count { it.items.isEmpty() }
    val registeredCount = pickupOrders.count { it.items.isNotEmpty() }
    val totalCarpetsCount = pickupOrders.sumOf { it.items.size }

    val filteredOrders = pickupOrders.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.order.customerName.contains(searchQuery, true) ||
                item.order.customerPhone.contains(searchQuery) ||
                item.order.id.contains(searchQuery, true) ||
                item.order.address.contains(searchQuery, true)

        val matchesFilter = when (selectedFilter) {
            "PENDING" -> item.items.isEmpty()
            "REGISTERED" -> item.items.isNotEmpty()
            else -> true
        }

        matchesSearch && matchesFilter
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
                    Text("فیلتر مأموریت‌های جمع‌آوری", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
                HorizontalDivider(color = CleanLightOutline)

                FilterOptionRow(
                    title = "همه سفارش‌ها",
                    count = pickupOrders.size,
                    isSelected = selectedFilter == "ALL",
                    onClick = {
                        selectedFilter = "ALL"
                        showFilterSheet = false
                    }
                )

                FilterOptionRow(
                    title = "در انتظار ثبت اقلام",
                    count = pendingCount,
                    isSelected = selectedFilter == "PENDING",
                    onClick = {
                        selectedFilter = "PENDING"
                        showFilterSheet = false
                    }
                )

                FilterOptionRow(
                    title = "اقلام ثبت‌شده (آماده انبار)",
                    count = registeredCount,
                    isSelected = selectedFilter == "REGISTERED",
                    onClick = {
                        selectedFilter = "REGISTERED"
                        showFilterSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. KPI Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: مأموریت‌های جمع‌آوری (Right Card)
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
                                text = FarsiUtils.toFarsiDigits(pickupOrders.size.toString()),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "کل مأموریت‌ها",
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
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = CleanGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(registeredCount.toString())} دریافت شده",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card 2: فرش‌های دریافت شده (Left Card)
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
                                text = FarsiUtils.toFarsiDigits(totalCarpetsCount.toString()),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanGreenPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تخته فرش",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanLightOnSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = CleanGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "${FarsiUtils.toFarsiDigits(pendingCount.toString())} در انتظار دریافت",
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

                // Active Filter Pill — آیکون + شمارنده (بدون متن «همه»)
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
                            text = FarsiUtils.toFarsiDigits(pickupOrders.size.toString()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanGreenPrimary
                        )
                    }
                }
            }
        }

        // 3. Collection Orders List
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
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = CleanLightOnSurfaceMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "هنوز سفارشی برای جمع‌آوری در این لیست وجود ندارد.",
                            color = CleanLightOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سفارش‌های جدید جمع‌آوری از طریق دیسپچ تخصیص می‌یابند.",
                            color = CleanLightOnSurfaceMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.order.id }) { item ->
                CollectionOrderUnifiedCard(
                    orderWithItems = item,
                    onSelectForInvoice = { onSelectOrderForInvoice(item) },
                    onCallCustomer = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.order.customerPhone}"))
                        context.startActivity(intent)
                    },
                    onNavigate = {
                        NavigationUtils.launchNeshan(context, item.order.latitude, item.order.longitude, item.order.address)
                    }
                )
            }
        }
    }
}

@Composable
private fun CollectionOrderUnifiedCard(
    orderWithItems: OrderWithItems,
    onSelectForInvoice: () -> Unit,
    onCallCustomer: () -> Unit,
    onNavigate: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val isRegistered = items.isNotEmpty()
    val totalArea = items.sumOf { it.areaSqMeter }
    val itemCount = if (items.isNotEmpty()) items.size else 0

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
            // Top Row: Status Pill (Left) & Customer Name + Order Pill (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right Side: Customer + Order Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CleanGreenPrimary
                    ) {
                        Text(
                            text = "سفارش ${order.id}",
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

                // Left Side: Status Tag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRegistered) CleanGreenPrimaryLight else CleanWarningBg
                ) {
                    Text(
                        text = if (isRegistered) "ثبت اقلام شد (${FarsiUtils.toFarsiDigits(itemCount.toString())})" else "در انتظار دریافت",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegistered) CleanGreenPrimary else CleanWarningText,
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

            // 2-Column Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Items summary
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
                            text = if (isRegistered) "${FarsiUtils.toFarsiDigits(itemCount.toString())} تخته ثبت‌شده" else "اقلام در محل دریافت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        Text(
                            text = if (isRegistered) "${FarsiUtils.toFarsiDigits(String.format("%.1f", totalArea))} مترمربع" else "نیاز به اندازه‌گیری",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }
                }

                // Column 2: Date
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
                            text = "امروز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanLightOnSurface
                        )
                        Text(
                            text = "نوبت جمع‌آوری",
                            fontSize = 11.sp,
                            color = CleanLightOnSurfaceMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button: ثبت اقلام و دریافت
                Button(
                    onClick = onSelectForInvoice,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreenPrimary),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        if (isRegistered) Icons.Default.EditNote else Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRegistered) "ویرایش و مشاهده اقلام" else "ثبت اقلام و دریافت",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Secondary Call Button
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

                // Secondary Navigation Button
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
