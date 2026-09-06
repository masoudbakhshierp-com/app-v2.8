package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.local.model.OrderWithItems
import com.example.data.model.ScanStage
import com.example.data.model.ScanVerificationResult
import com.example.ui.theme.CleanBlueContainer
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanTealAccent
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerModal(
    expectedOrder: OrderWithItems?,
    allOrders: List<OrderWithItems>,
    scanStage: ScanStage = ScanStage.DELIVERY,
    onDismiss: () -> Unit,
    onConfirmVerification: (ScanVerificationResult.Success) -> Unit,
    onReportMismatchToDispatch: (String) -> Unit
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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isBeepEnabled by remember { mutableStateOf(true) }
    var activeResult by remember { mutableStateOf<ScanVerificationResult?>(null) }

    // Laser Animation Effect
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    // Helper function to evaluate code against DB
    fun processScannedCode(rawCode: String) {
        val cleaned = rawCode.trim().uppercase()
        if (cleaned.isEmpty()) return

        // Search for matching order/carpet in DB
        // Try exact order match, or order containing, or carpet item barcode match
        var matchedOrder: OrderWithItems? = null
        var matchedCarpet = expectedOrder?.items?.firstOrNull()

        // Match algorithm
        val targetOrder = expectedOrder ?: allOrders.firstOrNull()

        // Extract order ID part if code format is ZM-1403-1015-C1 or ZM-1403-1015
        val codeParts = cleaned.split("-")
        val possibleOrderId = when {
            cleaned.startsWith("ZM-") -> {
                if (codeParts.size >= 3) "${codeParts[0]}-${codeParts[1]}-${codeParts[2]}" else cleaned
            }
            cleaned.startsWith("ORD-") -> cleaned.removePrefix("ORD-")
            else -> cleaned
        }

        // First check if rawCode matches a stapled carpet barcode tag (e.g. ST-1042-01 or user scanned label)
        val carpetByTag = allOrders.flatMap { o -> o.items.map { item -> Pair(o, item) } }
            .find { (_, item) -> item.barcodeTag.isNotBlank() && (item.barcodeTag.equals(cleaned, ignoreCase = true) || cleaned.contains(item.barcodeTag.uppercase())) }

        if (carpetByTag != null) {
            matchedOrder = carpetByTag.first
            matchedCarpet = carpetByTag.second
        } else {
            matchedOrder = allOrders.find { order ->
                order.order.id.uppercase() == possibleOrderId ||
                        order.order.id.uppercase().contains(possibleOrderId) ||
                        possibleOrderId.contains(order.order.id.uppercase().takeLast(4)) ||
                        order.items.any { it.barcodeTag.contains(cleaned) }
            }
        }

        if (matchedOrder != null) {
            // Check if carpet item index specified, e.g. C1 or C2
            val carpetIndex = if (cleaned.contains("-C")) {
                cleaned.substringAfter("-C").toIntOrNull()?.minus(1) ?: 0
            } else 0

            val carpet = matchedOrder.items.getOrNull(carpetIndex) ?: matchedOrder.items.firstOrNull()

            if (targetOrder != null && matchedOrder.order.id != targetOrder.order.id) {
                // MISMATCH DETECTED!
                activeResult = ScanVerificationResult.Mismatch(
                    scannedCode = cleaned,
                    expectedOrderId = targetOrder.order.id,
                    targetOrderWithItems = targetOrder,
                    actualOrderWithItems = matchedOrder,
                    actualCarpetItem = carpet,
                    scanStage = scanStage,
                    warningTitle = "⚠️ هشدار عدم تطابق فرش با سفارش جاری!",
                    warningMessage = "فرش اسکن شده متعلق به سفارش ${matchedOrder.order.id} (مشتری: ${matchedOrder.order.customerName}) می‌باشد، اما شما در حال پردازش سفارش ${targetOrder.order.id} (مشتری: ${targetOrder.order.customerName}) هستید."
                )
            } else {
                // MATCH SUCCESS!
                activeResult = ScanVerificationResult.Success(
                    scannedCode = cleaned,
                    orderWithItems = matchedOrder,
                    carpetItem = carpet,
                    scanStage = scanStage,
                    message = "بارکد $cleaned با موفقیت تأیید گردید."
                )
            }
        } else {
            // NOT FOUND IN DB
            activeResult = ScanVerificationResult.NotFound(
                scannedCode = cleaned,
                message = "کد $cleaned در لیست سفارشات دیتابیس قالیشویی یافت نشد."
            )
        }
    }

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
            Box(modifier = Modifier.fillMaxSize()) {
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
                                    .background(CleanBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "اسکن بارکد / QR کد فرش",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "مرحله: ${scanStage.titleFarsi}",
                                    color = CleanTealAccent,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Camera Viewfinder with Real ML Kit CameraX Scanner Frame
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFlashlightOn) Color(0xFF2C3E50) else Color(0xFF121820)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (hasCameraPermission) {
                                // Live Camera Feed using CameraX + ML Kit
                                RealCameraPreviewView(
                                    isFlashlightOn = isFlashlightOn,
                                    onBarcodeDetected = { scanned ->
                                        processScannedCode(scanned)
                                    }
                                )
                            } else {
                                // Camera Permission Request Button
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = CleanTealAccent,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "مجوز دسترسی به دوربین برای اسکن بارکد واقعی لازم است",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فعالسازی دوربین گوشی", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            // Viewfinder Center Reticle Overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (!hasCameraPermission) {
                                    // Grid texture if camera not enabled
                                    val gridStep = 24.dp.toPx()
                                    for (x in 0..size.width.toInt() step gridStep.toInt()) {
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.05f),
                                            start = Offset(x.toFloat(), 0f),
                                            end = Offset(x.toFloat(), size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                                }

                                val boxWidth = size.width * 0.72f
                                val boxHeight = size.height * 0.65f
                                val left = (size.width - boxWidth) / 2
                                val top = (size.height - boxHeight) / 2
                                val cornerLength = 28.dp.toPx()
                                val cornerStroke = 4.dp.toPx()

                                // Semi-transparent overlay outside reticle
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.45f)
                                )

                                // Clear target rectangle
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(left, top),
                                    size = Size(boxWidth, boxHeight)
                                )

                                // Corner brackets
                                val cornerColor = CleanTealAccent
                                drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
                                drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
                                drawLine(cornerColor, Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), cornerStroke)
                                drawLine(cornerColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), cornerStroke)
                                drawLine(cornerColor, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), cornerStroke)
                                drawLine(cornerColor, Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), cornerStroke)
                                drawLine(cornerColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), cornerStroke)
                                drawLine(cornerColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), cornerStroke)

                                // Animated Scanning Laser Line
                                val currentLaserY = top + (boxHeight * laserYRatio)
                                drawLine(
                                    color = Color(0xFFFF5252),
                                    start = Offset(left + 8f, currentLaserY),
                                    end = Offset(left + boxWidth - 8f, currentLaserY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }

                            // Viewfinder Controls Overlay Top
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = if (hasCameraPermission) CleanTealAccent else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasCameraPermission) "دوربین زنده فعال - اسکن بارکد/QR" else "دوربین غیرفعال است",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Flashlight & Sound Controls
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilterChip(
                                    selected = isFlashlightOn,
                                    onClick = { isFlashlightOn = !isFlashlightOn },
                                    label = { Text(if (isFlashlightOn) "چراغ روشن" else "چراغ‌قوه", fontSize = 11.sp, color = Color.White) },
                                    leadingIcon = {
                                        Icon(
                                            if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = null,
                                            tint = if (isFlashlightOn) Color.Yellow else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f),
                                        selectedContainerColor = CleanBluePrimary
                                    )
                                )

                                FilterChip(
                                    selected = isBeepEnabled,
                                    onClick = { isBeepEnabled = !isBeepEnabled },
                                    label = { Text("صدای بوق", fontSize = 11.sp, color = Color.White) },
                                    leadingIcon = {
                                        Icon(
                                            if (isBeepEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f),
                                        selectedContainerColor = CleanBluePrimary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Code Input Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E242C))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ورود دستی یا اسکن بارکد/QR:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualCodeInput,
                                    onValueChange = { manualCodeInput = it },
                                    placeholder = { Text("مثلاً ZM-1403-1015-C1", fontSize = 12.sp, color = Color.Gray) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF12161C),
                                        unfocusedContainerColor = Color(0xFF12161C),
                                        focusedBorderColor = CleanBluePrimary,
                                        unfocusedBorderColor = Color.DarkGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { processScannedCode(manualCodeInput) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CleanBluePrimary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text("بررسی", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Simulation Buttons for Demo/Testing
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171B21))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = CleanTealAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "کلیدهای اسکن سریع تست (آزمایش تطابق / عدم تطابق):",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. Scan Correct Carpet Button
                            val currentOrderId = expectedOrder?.order?.id ?: "ZM-1403-1015"
                            OutlinedButton(
                                onClick = { processScannedCode("$currentOrderId-C1") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اسکن فرش صحیح ($currentOrderId-C1)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Scan Wrong Carpet Button (MISMATCH)
                            val wrongOrderId = allOrders.find { it.order.id != currentOrderId }?.order?.id ?: "ZM-1403-0994"
                            Button(
                                onClick = { processScannedCode("$wrongOrderId-C1") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("⚠️ اسکن فرش اشتباه برای تست هشدار ($wrongOrderId-C1)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3. Scan Unknown Barcode
                            OutlinedButton(
                                onClick = { processScannedCode("ZM-9999-0000") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D))
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اسکن کد نامشخص (ZM-9999-0000)", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Overlay Dialog when Scan Result is Available
                activeResult?.let { result ->
                    ScanResultDialogOverlay(
                        result = result,
                        onDismiss = { activeResult = null },
                        onConfirm = { successRes ->
                            onConfirmVerification(successRes)
                            activeResult = null
                            onDismiss()
                        },
                        onReportDispatch = { alertMsg ->
                            onReportMismatchToDispatch(alertMsg)
                            activeResult = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Result Overlay for Scan Matches and Mismatch Warnings
 */
@Composable
private fun ScanResultDialogOverlay(
    result: ScanVerificationResult,
    onDismiss: () -> Unit,
    onConfirm: (ScanVerificationResult.Success) -> Unit,
    onReportDispatch: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (result) {
                    is ScanVerificationResult.Success -> {
                        // Success Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "تطابق کامل اطلاعات فرش! 🟢",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Text(
                            text = result.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Match Details Card
                        val order = result.orderWithItems.order
                        val carpet = result.carpetItem

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("کد سفارش:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(order.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مشتری:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                if (carpet != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("نوع و ابعاد فرش:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${carpet.carpetType} (${carpet.lengthMeter}×${carpet.widthMeter} متر)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("خدمات درخواست شده:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(carpet.requestedServicesJson, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onConfirm(result) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تأیید و ثبت نهایی اسکن در سیستم", fontWeight = FontWeight.Bold)
                        }
                    }

                    is ScanVerificationResult.Mismatch -> {
                        // MISMATCH RED WARNING BADGE
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "⚠️ هشدار خطای عدم تطابق فرش!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "فرش اسکن شده متعلق به سفارش جاری نیست!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // High contrast warning box comparing target vs scanned
                        val targetOrder = result.targetOrderWithItems?.order
                        val actualOrder = result.actualOrderWithItems?.order
                        val actualCarpet = result.actualCarpetItem

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF5350)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🔴 فرش اسکن شده مربوط به:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "مشتری: ${actualOrder?.customerName ?: "نامشخص"} (کد: ${actualOrder?.id ?: "نامشخص"})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "آدرس: ${actualOrder?.address ?: "---"}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                if (actualCarpet != null) {
                                    Text(
                                        text = "مشخصات: ${actualCarpet.carpetType} - ${actualCarpet.lengthMeter}×${actualCarpet.widthMeter} م",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEF9A9A))

                                Text(
                                    text = "🔵 سفارش جاری شما در این مقصد:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CleanBluePrimary
                                )
                                Text(
                                    text = "مشتری: ${targetOrder?.customerName ?: "نامشخص"} (کد: ${targetOrder?.id ?: "نامشخص"})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E1),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "توصیه سیستم: این فرش متعلق به این مشتری نیست. از تحویل یا بارگیری اشتباه خودداری کنید.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF795548),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("اسکن مجدد")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onReportDispatch(
                                        "هشدار جابجایی فرش: فرش اسکن شده (${result.scannedCode}) متعلق به ${actualOrder?.customerName} است اما در مقصد ${targetOrder?.customerName} اسکن شد!"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("گزارش به مرکز", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ScanVerificationResult.NotFound -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF57C00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "بارکد یا QR کد ثبت نشده!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57C00)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = result.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("متوجه شدم - اسکن مجدد")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live CameraX Preview with ML Kit Barcode Analyzer
 */
@OptIn(ExperimentalGetImage::class)
@Composable
internal fun RealCameraPreviewView(
    isFlashlightOn: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTimestamp by remember { mutableStateOf(0L) }

    LaunchedEffect(isFlashlightOn, camera) {
        try {
            camera?.cameraControl?.enableTorch(isFlashlightOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            try {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val now = System.currentTimeMillis()
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank()) {
                                                if (rawValue != lastScannedCode || (now - lastScannedTimestamp) > 3000) {
                                                    lastScannedCode = rawValue
                                                    lastScannedTimestamp = now
                                                    onBarcodeDetected(rawValue)
                                                }
                                                break
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } catch (e: Exception) {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

