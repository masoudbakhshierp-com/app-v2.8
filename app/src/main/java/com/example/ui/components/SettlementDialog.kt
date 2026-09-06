package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OrderWithItems
import com.example.ui.theme.CleanBluePrimary
import com.example.ui.theme.CleanTealAccent
import com.example.ui.theme.CleanTealContainer
import com.example.utils.FarsiUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettlementDialog(
    orderWithItems: OrderWithItems,
    onDismiss: () -> Unit,
    onConfirmSettlement: (paidAmount: Long, discountAmount: Long, paymentMethod: String, printReceipt: Boolean) -> Unit,
    onSignatureCaptured: (orderId: String, signatureData: String) -> Unit = { _, _ -> }
) {
    val order = orderWithItems.order
    var discountInput by remember { mutableStateOf(order.discountAmount.toString()) }
    val totalAmount = order.totalAmount

    var selectedMethod by remember { mutableStateOf("POS") } // POS, CASH, CARD_TO_CARD, CREDIT
    var shouldPrint by remember { mutableStateOf(true) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var isSignatureCaptured by remember { mutableStateOf(order.customerSignatureUrl.isNotBlank()) }

    val discount = discountInput.toLongOrNull() ?: 0L
    val finalPayable = (totalAmount - discount).coerceAtLeast(0L)

    if (showSignatureDialog) {
        CustomerSignatureDialog(
            customerName = order.customerName,
            orderId = order.id,
            onDismiss = { showSignatureDialog = false },
            onConfirmSignature = { sigData ->
                isSignatureCaptured = true
                showSignatureDialog = false
                onSignatureCaptured(order.id, sigData)
            }
        )
    }

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
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسویه حساب مالی سفارش ${order.id}", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "نام مشتری: ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "تلفن: ${order.customerPhone}", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مبلغ کل فاکتور:")
                            Text(FarsiUtils.formatPrice(totalAmount), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discount field
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("مبلغ تخفیف (تومان) - با تایید مدیر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payable box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("خالص مبلغ قابل دریافتی:", fontWeight = FontWeight.Bold)
                    Text(
                        text = FarsiUtils.formatPrice(finalPayable),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method Selector
                Text("روش پرداخت دریافتی در محل:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == "POS",
                        onClick = { selectedMethod = "POS" },
                        label = { Text("کارتخوان (POS)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMethod == "CASH",
                        onClick = { selectedMethod = "CASH" },
                        label = { Text("نقدی", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.8f)
                    )
                    FilterChip(
                        selected = selectedMethod == "CARD_TO_CARD",
                        onClick = { selectedMethod = "CARD_TO_CARD" },
                        label = { Text("کارت‌به‌کارت", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Digital signature button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSignatureCaptured) CleanTealContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSignatureCaptured) Icons.Default.Check else Icons.Default.Draw,
                                contentDescription = null,
                                tint = if (isSignatureCaptured) CleanTealAccent else CleanBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignatureCaptured) "امضای دیجیتال مشتری ثبت شد ✓" else "امضای دیجیتال تحویل مشتری",
                                fontSize = 13.sp,
                                fontWeight = if (isSignatureCaptured) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        OutlinedButton(
                            onClick = { showSignatureDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (isSignatureCaptured) "تغییر امضا" else "دریافت امضا", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Print Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = shouldPrint,
                        onCheckedChange = { shouldPrint = it }
                    )
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("چاپ خودکار رسید تسویه توسط پرینتر حرارتی بلوتوثی", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        onConfirmSettlement(finalPayable, discount, selectedMethod, shouldPrint)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت نهایی تسویه و پایان ماموریت", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
