package com.example.ui.screens

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
import com.example.data.model.ScanStage
import com.example.ui.components.RealisticOrderMapPreview
import com.example.ui.theme.*
import com.example.utils.FarsiUtils
import com.example.utils.NavigationUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionsListScreen(
    orders: List<OrderWithItems>,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    onSelectOrder: (String) -> Unit,
    onOpenInspectionForm: (String) -> Unit,
    onOpenRackAssignment: (String) -> Unit,
    onOpenSettlement: (OrderWithItems) -> Unit,
    onOpenScanner: (ScanStage, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrderTypeTab by remember { mutableStateOf(0) } // 0: All, 1: Pickup, 2: Delivery

    val filteredOrders = orders.filter { orderWithItems ->
        val order = orderWithItems.order
        val matchesSearch = searchQuery.isBlank() ||
                order.customerName.contains(searchQuery, true) ||
                order.customerPhone.contains(searchQuery) ||
                order.id.contains(searchQuery, true) ||
                order.address.contains(searchQuery, true)

        val isOrderPickup = order.orderType.equals("PICKUP", ignoreCase = true) ||
                            order.orderType.equals("COLLECTION", ignoreCase = true) ||
                            order.orderType.isBlank()
        val isOrderDelivery = order.orderType.equals("DELIVERY", ignoreCase = true) ||
                              order.status == "READY_FOR_DELIVERY" ||
                              order.status == "DELIVERED_SETTLED"

        val matchesType = when (selectedOrderTypeTab) {
            1 -> isOrderPickup && (order.status == "ASSIGNED" || order.status == "pickup_assigned")
            3 -> order.status == "COLLECTED_IN_INSPECTION"
            2 -> isOrderDelivery
            else -> true
        }

        val matchesStatus = when (statusFilter) {
            "ASSIGNED" -> order.status == "ASSIGNED" || order.status == "pickup_assigned"
            "COLLECTED" -> order.status == "COLLECTED_IN_INSPECTION"
            "DELIVERY" -> order.status == "READY_FOR_DELIVERY" || (order.orderType == "DELIVERY" && order.status == "ASSIGNED")
            "SETTLED" -> order.status == "DELIVERED_SETTLED"
            else -> true
        }

        matchesSearch && matchesType && matchesStatus
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 3-Phase Driver Mission Indicator Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CleanBlueContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Task, contentDescription = null, tint = CleanBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فرآیند سه مرحله‌ای مأموریت راننده قالیشویی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CleanBluePrimary
                        )
                    }
                    Text(
                        text = "ارتباط مستقیم با پنل وب",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Step Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Step 1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedOrderTypeTab == 1) CleanBluePrimary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOrderTypeTab = 1 }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "۱. جمع‌آوری",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOrderTypeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ثبت فاکتور در محل",
                                fontSize = 9.sp,
                                color = if (selectedOrderTypeTab == 1) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }

                    // Step 2
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedOrderTypeTab == 3) CleanBluePrimary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOrderTypeTab = 3 }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "۲. انبار و قفسه",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOrderTypeTab == 3) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تعیین قفسه و ارسال به پنل",
                                fontSize = 9.sp,
                                color = if (selectedOrderTypeTab == 3) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }

                    // Step 3
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedOrderTypeTab == 2) CleanBluePrimary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOrderTypeTab = 2 }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "۳. تحویل و تسویه",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOrderTypeTab == 2) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مسیر بهینه و تسویه",
                                fontSize = 9.sp,
                                color = if (selectedOrderTypeTab == 2) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Order Type Tabs
        TabRow(
            selectedTabIndex = selectedOrderTypeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            indicator = {},
            divider = {},
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            Tab(
                selected = selectedOrderTypeTab == 0,
                onClick = { selectedOrderTypeTab = 0 },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedOrderTypeTab == 0) CleanBluePrimary else Color.Transparent),
                text = {
                    Text(
                        "همه (${orders.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedOrderTypeTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedOrderTypeTab == 1,
                onClick = { selectedOrderTypeTab = 1 },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedOrderTypeTab == 1) CleanBluePrimary else Color.Transparent),
                text = {
                    Text(
                        "۱. جمع‌آوری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedOrderTypeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedOrderTypeTab == 3,
                onClick = { selectedOrderTypeTab = 3 },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedOrderTypeTab == 3) CleanBluePrimary else Color.Transparent),
                text = {
                    Text(
                        "۲. انبار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedOrderTypeTab == 3) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedOrderTypeTab == 2,
                onClick = { selectedOrderTypeTab = 2 },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedOrderTypeTab == 2) CleanBluePrimary else Color.Transparent),
                text = {
                    Text(
                        "۳. تحویل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedOrderTypeTab == 2) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar & Filter row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجوی نام مشتری، تلفن، آدرس یا کد...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CleanBluePrimary) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = CleanBluePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Single-row colorful icon status filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusItems = listOf(
                StatusFilterData("ALL", "همه", Icons.Default.FormatListBulleted, CleanBluePrimary, CleanBlueContainer),
                StatusFilterData("ASSIGNED", "جدید", Icons.Default.Schedule, Color(0xFFF57C00), Color(0xFFFFF3E0)),
                StatusFilterData("COLLECTED", "انبار", Icons.Default.Warehouse, CleanPurpleAccent, CleanPurpleContainer),
                StatusFilterData("DELIVERY", "تحویل", Icons.Default.LocalShipping, Color(0xFF0288D1), Color(0xFFE1F5FE)),
                StatusFilterData("SETTLED", "تسویه", Icons.Default.CheckCircle, CleanTealAccent, CleanTealContainer)
            )

            statusItems.forEach { item ->
                val isSelected = statusFilter == item.id
                Surface(
                    onClick = { onStatusFilterChange(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) item.accentColor else item.containerColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) item.accentColor else item.accentColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color.White else item.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else item.accentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Orders list
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("هیچ ماموریتی با این مشخصات یافت نشد.", fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders, key = { it.order.id }) { item ->
                    OrderMissionCard(
                        orderWithItems = item,
                        onSelect = { onSelectOrder(item.order.id) },
                        onCall = { NavigationUtils.makePhoneCall(context, item.order.customerPhone) },
                        onNavigateNeshan = {
                            NavigationUtils.launchNeshan(context, item.order.latitude, item.order.longitude, item.order.address)
                        },
                        onOpenInspectionForm = { onOpenInspectionForm(item.order.id) },
                        onOpenRackAssignment = { onOpenRackAssignment(item.order.id) },
                        onOpenSettlement = { onOpenSettlement(item) },
                        onOpenScanner = { stage -> onOpenScanner(stage, item.order.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderMissionCard(
    orderWithItems: OrderWithItems,
    onSelect: () -> Unit,
    onCall: () -> Unit,
    onNavigateNeshan: () -> Unit,
    onOpenInspectionForm: () -> Unit,
    onOpenRackAssignment: () -> Unit,
    onOpenSettlement: () -> Unit,
    onOpenScanner: (ScanStage) -> Unit = {}
) {
    val order = orderWithItems.order
    val isPickup = order.orderType.equals("PICKUP", ignoreCase = true) ||
                   order.orderType.equals("COLLECTION", ignoreCase = true) ||
                   order.orderType.isBlank()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Attached Realistic Map Preview with driver & destination route
            RealisticOrderMapPreview(
                customerName = order.customerName,
                address = order.address,
                orderId = order.id,
                latitude = order.latitude,
                longitude = order.longitude,
                heightDp = 125,
                isDeliveryMode = !isPickup,
                onNavigate = onNavigateNeshan
            )

            // 2. Card Body Content
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Row: Order Code, Type Badge, Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPickup) EmeraldMint else GoldLight
                        ) {
                            Text(
                                text = if (isPickup) "جمع‌آوری فرش" else "تحویل به مشتری",
                                color = if (isPickup) EmeraldDarkGreen else GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فاکتور ${order.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status Badge
                    StatusBadge(status = order.status)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer info with styled icon badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CleanBlueContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CleanBluePrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Customer Address with styled icon
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 17.sp
                    )
                }

                if (order.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "یادداشت: ${order.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Items count & Price summary row
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = CleanBluePrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${FarsiUtils.toFarsiDigits(orderWithItems.items.size.toString())} تخته فرش ثبت شده",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (order.rackCode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CleanPurpleAccent
                            ) {
                                Text(
                                    text = "قفسه: ${order.rackCode}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (order.totalAmount > 0) {
                            Text(
                                text = FarsiUtils.formatPrice(order.totalAmount),
                                fontWeight = FontWeight.Bold,
                                color = CleanBluePrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Action Row: Quick Call, Navigation Buttons, Main Step Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Action Icon Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Phone Call
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanBlueContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onCall() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "تماس با مشتری",
                                    tint = CleanBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 2. Neshan Navigation
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onNavigateNeshan() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TurnRight,
                                    contentDescription = "مسیریابی نشان",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 3. Barcode / QR Scan Button on Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanPurpleContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    val stage = if (isPickup) ScanStage.COLLECTION else ScanStage.DELIVERY
                                    onOpenScanner(stage)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "اسکن بارکد",
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Workflow action button based on order state (3 Mission Stages)
                    when (order.status) {
                        "ASSIGNED" -> {
                            Button(
                                onClick = onOpenInspectionForm,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("۱. فاکتور و دریافت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "COLLECTED_IN_INSPECTION" -> {
                            Button(
                                onClick = onOpenRackAssignment,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Warehouse, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("۲. تحویل انبار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "DELIVERED_TO_WORKSHOP" -> {
                            Button(
                                onClick = onOpenRackAssignment,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("قفسه: ${if (order.rackCode.isNotBlank()) order.rackCode else "تعیین قفسه"}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "READY_FOR_DELIVERY" -> {
                            Button(
                                onClick = { onOpenSettlement() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("۳. تحویل و تسویه", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = onSelect,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاهده فاکتور", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, txtColor, label) = when (status) {
        "ASSIGNED" -> Triple(StatusAssignedBg, StatusAssignedText, "تخصیص‌یافته")
        "COLLECTED_IN_INSPECTION" -> Triple(StatusInspectionBg, StatusInspectionText, "ثبت در محل")
        "DELIVERED_TO_WORKSHOP" -> Triple(StatusWorkshopBg, StatusWorkshopText, "در انبار کارگاه")
        "READY_FOR_DELIVERY" -> Triple(StatusAssignedBg, StatusAssignedText, "آماده تحویل")
        "DELIVERED_SETTLED" -> Triple(StatusSettledBg, StatusSettledText, "تسویه‌شده")
        else -> Triple(Color.LightGray, Color.DarkGray, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = txtColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private data class StatusFilterData(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val containerColor: Color
)
