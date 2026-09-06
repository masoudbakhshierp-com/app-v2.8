package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RackAssignmentDialog(
    orderId: String,
    currentRackCode: String,
    onDismiss: () -> Unit,
    onConfirm: (rackCode: String) -> Unit
) {
    var rackCodeInput by remember { mutableStateOf(if (currentRackCode.isNotBlank()) currentRackCode else "A-01") }
    val quickRacks = listOf("A-01", "A-02", "A-05", "B-01", "B-04", "B-10", "C-03", "D-12")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تخصیص قفسه انبار ورودی", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Text(
                    text = "فرش‌های سفارش $orderId به کارگاه منتقل شد. لطفاً شماره قفسه یا داربست انبار جهت نگهداری تا شروع شستشو را وارد یا انتخاب نمایید:",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = rackCodeInput,
                    onValueChange = { rackCodeInput = it.uppercase() },
                    label = { Text("کد / شماره قفسه (مثلاً A-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "انتخاب سریع قفسه‌های خالی پیشنهادی:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickRacks.forEach { code ->
                        FilterChip(
                            selected = rackCodeInput == code,
                            onClick = { rackCodeInput = code },
                            label = { Text(code, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rackCodeInput.isNotBlank()) {
                        onConfirm(rackCodeInput)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ثبت قفسه و ارسال به پنل وب")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("انصراف")
            }
        }
    )
}
