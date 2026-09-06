package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.BluetoothPrinterDevice

@Composable
fun PrinterDeviceDialog(
    connectedPrinter: BluetoothPrinterDevice?,
    availablePrinters: List<BluetoothPrinterDevice>,
    onScan: () -> Unit,
    onConnect: (BluetoothPrinterDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مدیریت پرینتر حرارتی بلوتوثی", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onScan) {
                    Icon(Icons.Default.Refresh, contentDescription = "جستجو")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (connectedPrinter != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("پرینتر متصل فعلی:", fontSize = 12.sp)
                                Text(connectedPrinter.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(connectedPrinter.address, fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = onDisconnect, shape = RoundedCornerShape(8.dp)) {
                                Text("قطع اتصال")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("دستگاه‌های حرارتی یافت‌شده / جفت‌شده:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (availablePrinters.isEmpty()) {
                    Text(
                        "هیچ پرینتر بلوتوثی جفت‌شده (Paired) پیدا نشد. اول از تنظیمات " +
                            "بلوتوث گوشی، پرینتر را روشن و Pair کنید، بعد اینجا دکمه‌ی " +
                            "بروزرسانی را بزنید.",
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(availablePrinters) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onConnect(device) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (connectedPrinter?.address == device.address)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(device.name, fontWeight = FontWeight.Bold)
                                        Text(device.address, fontSize = 11.sp)
                                    }
                                    if (connectedPrinter?.address == device.address) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("اتصال", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("بستن")
            }
        }
    )
}
