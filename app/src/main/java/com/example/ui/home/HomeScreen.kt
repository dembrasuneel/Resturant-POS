package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Operator
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.ui.printer.ReceiptPreviewDialog
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.theme.PosTableAvailable
import com.example.ui.theme.PosTableBillPending
import com.example.ui.theme.PosTableOccupied
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateToOrder: (Long) -> Unit,
    onNavigateToOpenOrders: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showTakeawayDialog by remember { mutableStateOf(false) }
    var takeawayCustomerName by remember { mutableStateOf("") }
    var showOperatorSwitchDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification, uiState.errorNotification) {
        val msg = uiState.userNotification ?: uiState.errorNotification
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PosAmberPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "BistroPOS Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.settings.restaurantName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${DateUtils.formatDate(System.currentTimeMillis())} • Terminal #01",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    // Operator Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clickable { showOperatorSwitchDialog = true }
                            .padding(end = 8.dp)
                            .testTag("operator_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Active Operator",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.currentOperator.name} (${uiState.currentOperator.role})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Printer Status Indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.printerStatus.isConnected && uiState.printerStatus.hasPaper)
                            Color(0xFF065F46) else Color(0xFF991B1B),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printer Status",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.printerStatus.isConnected && uiState.printerStatus.hasPaper) "Printer Ready" else "Printer Error",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PosNavyDark
                )
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
            // Main Action Bar (Touch-friendly POS buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showTakeawayDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("takeaway_order_button")
                ) {
                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = "Takeaway")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Takeaway Order", fontWeight = FontWeight.SemiBold)
                }

                BadgedBox(
                    badge = {
                        if (uiState.activeOrders.isNotEmpty()) {
                            Badge(
                                containerColor = PosAmberPrimary,
                                contentColor = Color.White
                            ) {
                                Text(uiState.activeOrders.size.toString())
                            }
                        }
                    }
                ) {
                    OutlinedButton(
                        onClick = onNavigateToOpenOrders,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_orders_button")
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Open Orders")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active Orders")
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToReports,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("sales_summary_button")
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = "Sales")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sales Summary")
                }

                OutlinedButton(
                    onClick = onNavigateToMenu,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("menu_management_button")
                ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Menu")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Menu Setup")
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terminal Settings")
                }
            }

            // Status Summary Chips
            val availableCount = uiState.tables.count { it.status == TableStatus.AVAILABLE }
            val occupiedCount = uiState.tables.count { it.status == TableStatus.OCCUPIED }
            val billPendingCount = uiState.tables.count { it.status == TableStatus.BILL_PENDING }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    label = "Available",
                    count = availableCount,
                    color = PosTableAvailable
                )
                StatusBadge(
                    label = "Occupied",
                    count = occupiedCount,
                    color = PosTableOccupied
                )
                StatusBadge(
                    label = "Bill Pending",
                    count = billPendingCount,
                    color = PosTableBillPending
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restaurant Tables Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tables_grid")
            ) {
                items(uiState.tables, key = { it.tableId }) { table ->
                    TableGridItem(
                        table = table,
                        onClick = {
                            viewModel.openTableOrder(table) { orderId ->
                                onNavigateToOrder(orderId)
                            }
                        }
                    )
                }
            }
        }
    }

    // Receipt Preview Dialog if present
    uiState.printPreviewContent?.let { previewText ->
        ReceiptPreviewDialog(
            formattedText = previewText,
            printerStatus = uiState.printerStatus,
            onDismiss = { viewModel.clearPrintPreview() },
            onPrintAgain = {
                uiState.currentOrderDetails?.order?.orderId?.let { orderId ->
                    viewModel.reprintReceipt(orderId)
                }
            }
        )
    }

    // Takeaway Dialog
    if (showTakeawayDialog) {
        AlertDialog(
            onDismissRequest = { showTakeawayDialog = false },
            title = { Text("Start Takeaway Order") },
            text = {
                Column {
                    Text(
                        "Enter optional customer name or phone for order tracking:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = takeawayCustomerName,
                        onValueChange = { takeawayCustomerName = it },
                        label = { Text("Customer Name / Call Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("takeaway_customer_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = takeawayCustomerName.trim()
                        showTakeawayDialog = false
                        takeawayCustomerName = ""
                        viewModel.startTakeawayOrder(name) { orderId ->
                            onNavigateToOrder(orderId)
                        }
                    },
                    modifier = Modifier.testTag("confirm_takeaway_button")
                ) {
                    Text("Start Takeaway")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTakeawayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Switch Operator Dialog
    if (showOperatorSwitchDialog) {
        val operators = listOf(
            Operator("CASHIER-1", "Sarah Jenkins", "CASHIER"),
            Operator("CASHIER-2", "David Vance", "CASHIER"),
            Operator("MGR-10", "Elena Rostova", "MANAGER")
        )
        AlertDialog(
            onDismissRequest = { showOperatorSwitchDialog = false },
            title = { Text("Switch Active Operator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    operators.forEach { op ->
                        val isSelected = op.operatorId == uiState.currentOperator.operatorId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchOperator(op)
                                    showOperatorSwitchDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(op.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Role: ${op.role} • ID: ${op.operatorId}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        "Active",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showOperatorSwitchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TableGridItem(
    table: RestaurantTable,
    onClick: () -> Unit
) {
    val (statusColor, statusText) = when (table.status) {
        TableStatus.AVAILABLE -> Pair(PosTableAvailable, "Available")
        TableStatus.OCCUPIED -> Pair(PosTableOccupied, "Occupied")
        TableStatus.BILL_PENDING -> Pair(PosTableBillPending, "Bill Pending")
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
            .testTag("table_card_${table.tableNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Table name & Capacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Capacity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${table.capacity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Middle: Active Bill Amount if occupied
            if (table.status != TableStatus.AVAILABLE && table.activeAmount > BigDecimal.ZERO) {
                Column {
                    Text(
                        text = "Current Bill",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyHelper.format(table.activeAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "Tap to start order",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom: Status Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
