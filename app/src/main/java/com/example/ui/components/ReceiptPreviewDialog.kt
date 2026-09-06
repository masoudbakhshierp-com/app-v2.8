package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.utils.PrinterManager

import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewDialog(
    title: String,
    orderWithItems: OrderWithItems,
    paymentMethodLabel: String = "پیش‌فاکتور اولیه",
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onPrintConfirm: () -> Unit
) {
    val order = orderWithItems.order
    val itemsSummary = orderWithItems.items.map {
        "${it.carpetType} (${it.lengthMeter}x${it.widthMeter}متر) - ${it.requestedServicesJson} - ${it.totalPrice} تومان"
    }

    val receiptText = PrinterManager.buildEscPosThermalReceiptText(
        title = title,
        orderId = order.id,
        customerName = order.customerName,
        customerPhone = order.customerPhone,
        address = order.address,
        carpetItemsSummary = if (itemsSummary.isEmpty()) listOf("هنوز فرشی ثبت نشده است") else itemsSummary,
        totalPrice = order.totalAmount,
        discount = order.discountAmount,
        netPayable = order.totalAmount - order.discountAmount,
        paymentMethod = paymentMethodLabel,
        rackCode = order.rackCode,
        includeTwoCopies = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = CleanPurpleAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("پیش‌نمایش چاپ ۲ نسخه‌ای (مشتری و راننده)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CleanPurpleContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "فاکتور حرارتی شامل ۲ نسخه (نسخه مشتری + نسخه راننده) به همراه QR کد پیگیری است.",
                        fontSize = 11.sp,
                        color = CleanPurpleAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated ESC/POS Receipt Paper
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)), // Thermal Paper tint
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val singleCopyCustomer = PrinterManager.buildEscPosThermalReceiptText(
                            title = title,
                            orderId = order.id,
                            customerName = order.customerName,
                            customerPhone = order.customerPhone,
                            address = order.address,
                            carpetItemsSummary = if (itemsSummary.isEmpty()) listOf("هنوز فرشی ثبت نشده است") else itemsSummary,
                            totalPrice = order.totalAmount,
                            discount = order.discountAmount,
                            netPayable = order.totalAmount - order.discountAmount,
                            paymentMethod = paymentMethodLabel,
                            rackCode = order.rackCode,
                            includeTwoCopies = false
                        )

                        // --- Copy 1: Customer Copy ---
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                QrCodeView(code = "ORD-${order.id}", size = 80.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("QR کد پیگیری: ORD-${order.id}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Text(
                            text = singleCopyCustomer,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Paper Cut Separator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                            Text(
                                "  ✂ محل برش کاغذ پرینتر (نسخه راننده)  ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Copy 2: Driver Copy ---
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                QrCodeView(code = "ORD-${order.id}", size = 80.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("QR کد پیگیری: ORD-${order.id}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        val singleCopyDriver = singleCopyCustomer.replace("نسخه تک برگ", "نسخه راننده")
                        Text(
                            text = singleCopyDriver,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Black,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPrintConfirm,
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال چاپ ۲ نسخه فاکتور...")
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("چاپ ۲ نسخه (نسخه مشتری و نسخه راننده)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
