package com.example.ui.openorders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.ui.printer.ReceiptPreviewDialog
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosTableBillPending
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenOrdersScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit,
    onSelectOrder: (Long) -> Unit,
    onNavigateToBilling: (Long) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredOrders = uiState.activeOrders.filter { order ->
        when (selectedFilter) {
            "DINE_IN" -> order.orderType == OrderType.DINE_IN
            "TAKEAWAY" -> order.orderType == OrderType.TAKEAWAY
            "BILL_PENDING" -> order.status == OrderStatus.BILL_PENDING
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Active Orders (${uiState.activeOrders.size})", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("ALL", "All Active (${uiState.activeOrders.size})"),
                    Pair("DINE_IN", "Dine-In (${uiState.activeOrders.count { it.orderType == OrderType.DINE_IN }})"),
                    Pair("TAKEAWAY", "Takeaway (${uiState.activeOrders.count { it.orderType == OrderType.TAKEAWAY }})"),
                    Pair("BILL_PENDING", "Bill Pending (${uiState.activeOrders.count { it.status == OrderStatus.BILL_PENDING }})")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredOrders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "No orders",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active orders found in this category", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredOrders, key = { it.orderId }) { order ->
                        ActiveOrderCard(
                            order = order,
                            onOpenOrder = { onSelectOrder(order.orderId) },
                            onPrintBill = {
                                viewModel.loadOrderDetails(order.orderId)
                                viewModel.markBillPendingAndPrint()
                            },
                            onProceedToBilling = {
                                viewModel.loadOrderDetails(order.orderId)
                                onNavigateToBilling(order.orderId)
                            }
                        )
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
                uiState.currentOrderDetails?.order?.orderId?.let { orderId ->
                    viewModel.reprintBill(orderId)
                }
            }
        )
    }
}

@Composable
fun ActiveOrderCard(
    order: Order,
    onOpenOrder: () -> Unit,
    onPrintBill: () -> Unit,
    onProceedToBilling: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.tableName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (order.status == OrderStatus.BILL_PENDING) PosTableBillPending.copy(alpha = 0.15f) else PosAmberPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.status.name,
                            color = if (order.status == OrderStatus.BILL_PENDING) PosTableBillPending else PosAmberPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref: ${order.orderNumber} • Opened at ${DateUtils.formatTime(order.createdAt)} • Staff: ${order.operatorId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyHelper.format(order.grandTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPrintBill,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print Bill", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bill", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onOpenOrder,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onProceedToBilling,
                        colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = "Pay", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
