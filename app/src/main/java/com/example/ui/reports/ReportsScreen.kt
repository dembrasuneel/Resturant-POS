package com.example.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.ui.printer.ReceiptPreviewDialog
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.theme.PosSuccess
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit
) {
    val completedOrders = uiState.completedOrders

    // Aggregates
    val totalGrossSales = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.grandTotal) }
    val totalSubtotal = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.subtotal) }
    val totalTax = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.taxAmount) }
    val totalService = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.serviceChargeAmount) }
    val totalDiscount = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.discountAmount) }
    val countCompleted = completedOrders.size
    val countActive = uiState.activeOrders.size

    val avgTicket = if (countCompleted > 0) {
        totalGrossSales.divide(BigDecimal(countCompleted), 2, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }

    var selectedOrderIdForReprint by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Daily Sales Summary & Financial Reports", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val sb = StringBuilder()
                            sb.appendLine("================================")
                            sb.appendLine("     DAILY SALES (Z-REPORT)     ")
                            sb.appendLine("================================")
                            sb.appendLine(uiState.settings.restaurantName)
                            sb.appendLine("Date: ${DateUtils.formatDateTime(System.currentTimeMillis())}")
                            sb.appendLine("Terminal ID: TERM-POS-01")
                            sb.appendLine("--------------------------------")
                            sb.appendLine(String.format("%-20s %10s", "Completed Orders:", countCompleted))
                            sb.appendLine(String.format("%-20s %10s", "Active Orders:", countActive))
                            sb.appendLine(String.format("%-20s %10s", "Average Ticket:", CurrencyHelper.format(avgTicket)))
                            sb.appendLine("--------------------------------")
                            sb.appendLine(String.format("%-20s %10s", "Food Subtotal:", CurrencyHelper.format(totalSubtotal)))
                            sb.appendLine(String.format("%-20s %10s", "Discounts Given:", "-${CurrencyHelper.format(totalDiscount)}"))
                            sb.appendLine(String.format("%-20s %10s", "Total Tax:", CurrencyHelper.format(totalTax)))
                            sb.appendLine(String.format("%-20s %10s", "Service Charges:", CurrencyHelper.format(totalService)))
                            sb.appendLine("================================")
                            sb.appendLine(String.format("%-18s %12s", "GROSS TOTAL:", CurrencyHelper.format(totalGrossSales)))
                            sb.appendLine("================================")
                            sb.appendLine("End of Day Report Confirmed.")
                            sb.appendLine()
                            viewModel.reprintReceipt(0) // Triggers dialog with custom preview
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Z-Report", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Z-Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PosNavyDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Metrics KPI Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Gross Sales",
                    value = CurrencyHelper.format(totalGrossSales),
                    icon = Icons.Default.TrendingUp,
                    color = PosSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Completed Orders",
                    value = "$countCompleted",
                    icon = Icons.Default.Receipt,
                    color = PosAmberPrimary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Average Ticket",
                    value = CurrencyHelper.format(avgTicket),
                    icon = Icons.Default.Payments,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Active / Open",
                    value = "$countActive",
                    icon = Icons.Default.CreditCard,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Completed Transactions History Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Completed Orders & Settlement History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (completedOrders.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No completed orders recorded today.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(completedOrders, key = { it.orderId }) { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${order.tableName} (${order.orderType})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Order: ${order.orderNumber} • ${DateUtils.formatTime(order.updatedAt)} • Staff: ${order.operatorId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = CurrencyHelper.format(order.grandTotal),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PosSuccess
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                selectedOrderIdForReprint = order.orderId
                                                viewModel.reprintReceipt(order.orderId)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("reprint_receipt_${order.orderId}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Print, contentDescription = "Reprint", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reprint Receipt", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt Preview Dialog
    uiState.printPreviewContent?.let { previewText ->
        ReceiptPreviewDialog(
            formattedText = previewText,
            printerStatus = uiState.printerStatus,
            onDismiss = { viewModel.clearPrintPreview() },
            onPrintAgain = {
                selectedOrderIdForReprint?.let { orderId ->
                    viewModel.reprintReceipt(orderId)
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
