package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanPurpleAccent
import com.example.ui.theme.CleanPurpleContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReturnToCleanWarehouseDialog(
    orderId: String,
    customerName: String,
    currentRackCode: String,
    onDismiss: () -> Unit,
    onConfirm: (cleanRackCode: String, reason: String) -> Unit
) {
    var cleanRackInput by remember { mutableStateOf(if (currentRackCode.isNotBlank() && currentRackCode.startsWith("T-")) currentRackCode else "T-101") }
    var reasonInput by remember { mutableStateOf("عدم حضور مشتری در محل تحویل") }

    val quickCleanRacks = listOf("T-101", "T-102", "T-105", "T-110", "C-01", "C-02", "C-05")
    val quickReasons = listOf("عدم حضور مشتری در محل", "مشتری پاسخگو نبود", "درخواست تغییر زمان تحویل", "نقص در آدرس تحویل")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warehouse, contentDescription = null, tint = CleanPurpleAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("برگشت به قفسه تمیز انبار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CleanPurpleContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "سفارش $orderId - مشتری: $customerName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CleanPurpleAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "فرش‌های این فاکتور به علت عدم تحویل، به انبار تمیز منتقل شده و مجدداً توسط پنل برنامه‌ریزی می‌گردند.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = cleanRackInput,
                    onValueChange = { cleanRackInput = it.uppercase() },
                    label = { Text("کد قفسه تمیز انبار (مثلاً T-101)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "انتخاب قفسه تمیز پیشنهادی انبار:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCleanRacks.forEach { code ->
                        FilterChip(
                            selected = cleanRackInput == code,
                            onClick = { cleanRackInput = code },
                            label = { Text(code, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("علت عدم تحویل در روز جاری") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickReasons.forEach { reason ->
                        SuggestionChip(
                            onClick = { reasonInput = reason },
                            label = { Text(reason, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cleanRackInput.isNotBlank()) {
                        onConfirm(cleanRackInput, reasonInput)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CleanPurpleAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ثبت برگشت به قفسه تمیز", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("انصراف")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
