package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CarpetTariffItem
import com.example.data.model.ServiceTariffItem
import com.example.data.model.TariffSyncResult
import com.example.ui.theme.*
import com.example.utils.FarsiUtils

/**
 * دیالوگ ثبت اقلام فاکتور فرش هماهنگ با نرخ‌نامه وب‌پنل و سرور Supabase
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCarpetItemDialog(
    orderId: String,
    tariffSyncResult: TariffSyncResult = TariffSyncResult.createDefault(),
    onRefreshTariffs: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (
        carpetType: String,
        length: Double,
        width: Double,
        unitPrice: Long,
        services: List<String>,
        defects: List<String>,
        notes: String,
        barcodeTag: String
    ) -> Unit
) {
    val carpetTariffs = if (tariffSyncResult.carpetTariffs.isNotEmpty()) {
        tariffSyncResult.carpetTariffs
    } else {
        TariffSyncResult.DEFAULT_CARPET_TARIFFS
    }

    val serviceTariffs = if (tariffSyncResult.serviceTariffs.isNotEmpty()) {
        tariffSyncResult.serviceTariffs
    } else {
        TariffSyncResult.DEFAULT_SERVICE_TARIFFS
    }

    val defectTariffs = if (tariffSyncResult.defectTariffs.isNotEmpty()) {
        tariffSyncResult.defectTariffs
    } else {
        TariffSyncResult.DEFAULT_DEFECT_TARIFFS
    }

    // Step 1: Pre-printed Stapled Barcode Tag
    var barcodeTagText by remember {
        mutableStateOf("ST-${orderId.takeLast(4)}-${(10..99).random()}")
    }
    var showStapleScanner by remember { mutableStateOf(false) }

    if (showStapleScanner) {
        StapleTagScannerModal(
            onDismiss = { showStapleScanner = false },
            onTagScanned = { scannedCode ->
                barcodeTagText = scannedCode.uppercase()
                showStapleScanner = false
            }
        )
    }

    // Selected Carpet Tariff
    var selectedTariffItem by remember { mutableStateOf(carpetTariffs[0]) }
    var selectedType by remember { mutableStateOf(carpetTariffs[0].title) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    var lengthText by remember { mutableStateOf(selectedTariffItem.defaultLength.toString()) }
    var widthText by remember { mutableStateOf(selectedTariffItem.defaultWidth.toString()) }
    var unitPriceText by remember { mutableStateOf(selectedTariffItem.unitPricePerMeter.toString()) }

    // Selected Services with calculated service fee
    val selectedServices = remember {
        mutableStateListOf<ServiceTariffItem>().apply {
            if (serviceTariffs.isNotEmpty()) add(serviceTariffs[0])
        }
    }
    var servicesDropdownExpanded by remember { mutableStateOf(false) }

    // Defects
    val selectedDefects = remember { mutableStateListOf("بدون عیب اولیه") }
    var defectsDropdownExpanded by remember { mutableStateOf(false) }

    var customNotes by remember { mutableStateOf("") }

    val length = lengthText.toDoubleOrNull() ?: 0.0
    val width = widthText.toDoubleOrNull() ?: 0.0
    val area = length * width
    val unitPrice = unitPriceText.toLongOrNull() ?: 0L
    val baseWashPrice = (area * unitPrice).toLong()

    // Additional Services Total
    val servicesTotalFee = selectedServices.sumOf { srv ->
        if (srv.isPercentage && srv.percentage > 0) {
            (baseWashPrice * (srv.percentage / 100.0)).toLong()
        } else {
            srv.price
        }
    }
    val totalPrice = baseWashPrice + servicesTotalFee

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CleanPurpleContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت اقلام فاکتور فرش",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "سفارش کد ${FarsiUtils.toFarsiDigits(orderId)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rate Sheet Status Banner (Supabase Synchronized)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (tariffSyncResult.isLiveFromSupabase) CleanTealContainer.copy(alpha = 0.6f) else CleanPurpleContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (tariffSyncResult.isLiveFromSupabase) CleanTealAccent.copy(alpha = 0.4f) else CleanPurpleAccent.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (tariffSyncResult.isLiveFromSupabase) Icons.Default.CloudDone else Icons.Default.PriceCheck,
                                contentDescription = null,
                                tint = if (tariffSyncResult.isLiveFromSupabase) CleanTealAccent else CleanPurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (tariffSyncResult.isLiveFromSupabase) "نرخ‌نامه هماهنگ با پنل وب و Supabase" else "نرخ‌نامه مصوب ${com.example.data.WorkshopNameHolder.current}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tariffSyncResult.isLiveFromSupabase) CleanTealAccent else CleanPurpleAccent
                                )
                                Text(
                                    text = "${carpetTariffs.size} نوع فرش و ${serviceTariffs.size} خدمت فعال در تعرفه",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshTariffs,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "بروزرسانی نرخ‌نامه",
                                tint = CleanPurpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Stapled Barcode Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanPurpleContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = CleanPurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شناسه / بارکد تگ منگنه فرش:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CleanPurpleAccent
                                )
                            }

                            TextButton(
                                onClick = {
                                    barcodeTagText = "ST-${orderId.takeLast(4)}-${(10..99).random()}"
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = CleanPurpleAccent)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("کد تصادفی", fontSize = 10.sp, color = CleanPurpleAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = barcodeTagText,
                                onValueChange = { barcodeTagText = it.uppercase() },
                                label = { Text("کد بارکد تگ", fontSize = 11.sp) },
                                placeholder = { Text("ST-...", fontSize = 11.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    IconButton(onClick = { showStapleScanner = true }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "اسکن بارکد",
                                            tint = CleanPurpleAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (barcodeTagText.isNotBlank()) {
                                        IconButton(onClick = { barcodeTagText = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "پاک کردن",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { showStapleScanner = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اسکن دوربین", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Carpet Type Selection (From Supabase Tariff List)
                Text(
                    text = "۱. انتخاب نوع فرش از نرخ‌نامه رسمی:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CleanPurpleAccent
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع و تعرفه فرش") },
                        leadingIcon = {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = CleanPurpleAccent)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        carpetTariffs.forEach { tariffItem ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(tariffItem.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${FarsiUtils.formatPrice(tariffItem.unitPricePerMeter)} تومان / ${tariffItem.unit} | دسته: ${tariffItem.category}",
                                            fontSize = 10.sp,
                                            color = CleanPurpleAccent
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (selectedType == tariffItem.title) CleanPurpleAccent else Color.Transparent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    selectedTariffItem = tariffItem
                                    selectedType = tariffItem.title
                                    unitPriceText = tariffItem.unitPricePerMeter.toString()
                                    lengthText = tariffItem.defaultLength.toString()
                                    widthText = tariffItem.defaultWidth.toString()
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Size Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { lengthText = "4.0"; widthText = "3.0" },
                        label = { Text("۱۲ متری (۳×۴)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { lengthText = "3.5"; widthText = "2.5" },
                        label = { Text("۹ متری (۲٫۵×۳٫۵)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { lengthText = "3.0"; widthText = "2.0" },
                        label = { Text("۶ متری (۲×۳)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { lengthText = "2.25"; widthText = "1.5" },
                        label = { Text("قالیچه (۱٫۵×۲٫۲۵)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Dimensions & Unit Price
                Text(
                    text = "۲. ابعاد و نرخ شستشو:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CleanPurpleAccent
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("طول (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("عرض (متر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = it },
                    label = { Text("نرخ شستشو (تومان هر متر مربع / تخته)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (unitPrice != selectedTariffItem.unitPricePerMeter) {
                            TextButton(onClick = { unitPriceText = selectedTariffItem.unitPricePerMeter.toString() }) {
                                Text("نرخ مصوب", fontSize = 10.sp, color = CleanPurpleAccent)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Requested Services (From Supabase Tariff List)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleanHands, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "۳. خدمات درخواستی و تکمیلی (نرخ‌نامه):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CleanPurpleAccent
                        )
                    }

                    if (selectedServices.isNotEmpty()) {
                        Text(
                            text = "+ ${FarsiUtils.formatPrice(servicesTotalFee)} تومان",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPurpleAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = servicesDropdownExpanded,
                    onExpandedChange = { servicesDropdownExpanded = !servicesDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedServices.isEmpty()) "هیچ خدماتی انتخاب نشده" else selectedServices.joinToString("، ") { it.title },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("لیست خدمات درخواستی") },
                        leadingIcon = {
                            Icon(Icons.Default.Build, contentDescription = null, tint = CleanPurpleAccent)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = servicesDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = servicesDropdownExpanded,
                        onDismissRequest = { servicesDropdownExpanded = false }
                    ) {
                        serviceTariffs.forEach { serviceItem ->
                            val isSelected = selectedServices.any { it.id == serviceItem.id }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(serviceItem.title, fontSize = 13.sp)
                                        Text(
                                            if (serviceItem.price > 0) "+ ${FarsiUtils.formatPrice(serviceItem.price)} تومان" else "رایگان",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanPurpleAccent
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = CleanPurpleAccent)
                                    )
                                },
                                onClick = {
                                    if (isSelected) {
                                        selectedServices.removeAll { it.id == serviceItem.id }
                                    } else {
                                        selectedServices.add(serviceItem)
                                    }
                                }
                            )
                        }
                    }
                }

                if (selectedServices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedServices.forEach { serviceItem ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedServices.remove(serviceItem) },
                                label = {
                                    Text(
                                        "${serviceItem.title} (${FarsiUtils.formatPrice(serviceItem.price)} ت)",
                                        fontSize = 11.sp
                                    )
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(12.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CleanPurpleContainer,
                                    selectedLabelColor = CleanPurpleAccent
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Initial Defects / Flaws (From Supabase Defect List)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۴. ثبت عیوب اولیه (قبل از شستشو):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = defectsDropdownExpanded,
                    onExpandedChange = { defectsDropdownExpanded = !defectsDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedDefects.isEmpty()) "بدون عیب ثبت‌شده" else selectedDefects.joinToString("، "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("لیست عیوب فرش") },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = defectsDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = defectsDropdownExpanded,
                        onDismissRequest = { defectsDropdownExpanded = false }
                    ) {
                        defectTariffs.forEach { defectItem ->
                            val defect = defectItem.title
                            val isSelected = selectedDefects.contains(defect)
                            DropdownMenuItem(
                                text = { Text(defect, fontSize = 13.sp) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                                    )
                                },
                                onClick = {
                                    if (defect == "بدون عیب اولیه") {
                                        selectedDefects.clear()
                                        selectedDefects.add("بدون عیب اولیه")
                                    } else {
                                        selectedDefects.remove("بدون عیب اولیه")
                                        if (isSelected) selectedDefects.remove(defect)
                                        else selectedDefects.add(defect)
                                    }
                                }
                            )
                        }
                    }
                }

                if (selectedDefects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedDefects.forEach { defect ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    selectedDefects.remove(defect)
                                    if (selectedDefects.isEmpty()) selectedDefects.add("بدون عیب اولیه")
                                },
                                label = { Text(defect, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(12.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Notes
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("یادداشت / توضیحات راننده", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Summary Calculation Card (Breakdown)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanPurpleContainer.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanPurpleAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مساحت کل:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                FarsiUtils.formatArea(area),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CleanPurpleAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مبلغ پایه شستشو:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${FarsiUtils.formatPrice(baseWashPrice)} تومان",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (servicesTotalFee > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("هزینه خدمات اضافی:", fontSize = 12.sp, color = CleanPurpleAccent)
                                Text(
                                    "+ ${FarsiUtils.formatPrice(servicesTotalFee)} تومان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPurpleAccent
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = CleanPurpleAccent.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ نهایی این فرش:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CleanPurpleAccent
                            )
                            Text(
                                text = FarsiUtils.formatPrice(totalPrice),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = CleanPurpleAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (length > 0 && width > 0 && unitPrice >= 0 && barcodeTagText.isNotBlank()) {
                            val serviceNames = selectedServices.map { it.title }
                            onConfirm(
                                selectedType,
                                length,
                                width,
                                unitPrice,
                                serviceNames,
                                selectedDefects.toList(),
                                customNotes,
                                barcodeTagText
                            )
                            onDismiss()
                        }
                    },
                    enabled = barcodeTagText.isNotBlank() && length > 0 && width > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت و افزودن به فاکتور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StapleTagScannerModal(
    onDismiss: () -> Unit,
    onTagScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var manualCodeInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CleanPurpleAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "اسکن بارکد منگنه‌شده فرش",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "تگ الصاق‌شده به فرش را مقابل دوربین قرار دهید",
                                color = CleanTealAccent,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Camera Scanner Viewport or Permission Request
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E1E2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        RealCameraPreviewView(
                            isFlashlightOn = false,
                            onBarcodeDetected = { code ->
                                onTagScanned(code)
                            }
                        )

                        // Reticle Overlay
                        Box(
                            modifier = Modifier
                                .size(200.dp, 120.dp)
                                .border(2.dp, CleanTealAccent, RoundedCornerShape(12.dp))
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232332)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CleanPurpleAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("دسترسی به دوربین جهت اسکن بارکد تگ الزامی است", color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent)
                            ) {
                                Text("اعطای دسترسی دوربین")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Manual Entry Option
                Text("یا ورود دستی شماره بارکد تگ:", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it.uppercase() },
                        placeholder = { Text("مثلاً ST-9081-45", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CleanPurpleAccent,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (manualCodeInput.isNotBlank()) {
                                onTagScanned(manualCodeInput.trim())
                            }
                        },
                        enabled = manualCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تایید")
                    }
                }
            }
        }
    }
}
