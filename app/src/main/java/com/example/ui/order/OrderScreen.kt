package com.example.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.ui.printer.ReceiptPreviewDialog
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.theme.PosSuccess
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    orderId: Long,
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit,
    onNavigateToBilling: (Long) -> Unit
) {
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }

    var selectedItemForNotes by remember { mutableStateOf<OrderItem?>(null) }
    var notesText by remember { mutableStateOf("") }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountPercentInput by remember { mutableStateOf("10") }
    var discountReasonInput by remember { mutableStateOf("VIP Customer") }

    val orderDetails = uiState.currentOrderDetails
    val order = orderDetails?.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = order?.tableName ?: "Order #$orderId",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Order: ${order?.orderNumber ?: ""} • Status: ${order?.status ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("order_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.sendKitchenTicket() },
                        colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("send_kitchen_ticket_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Kitchen Ticket", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kitchen Ticket")
                    }

                    Button(
                        onClick = { viewModel.markBillPendingAndPrint() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("print_customer_bill_button")
                    ) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = "Customer Bill", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Bill")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PosNavyDark
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // LEFT PANE: Product Catalog & Categories (60% width)
            Column(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search products, combos, drinks...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryId == null && uiState.searchQuery.isEmpty(),
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Products") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosAmberPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(uiState.categories, key = { it.categoryId }) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.categoryId,
                            onClick = { viewModel.selectCategory(category.categoryId) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosAmberPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_${category.categoryId}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filtered Products
                val filteredProducts = uiState.products.filter { product ->
                    val matchesCategory = uiState.selectedCategoryId == null || product.categoryId == uiState.selectedCategoryId
                    val matchesQuery = uiState.searchQuery.isEmpty() ||
                            product.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            product.description.contains(uiState.searchQuery, ignoreCase = true)
                    matchesCategory && matchesQuery
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("products_catalog_grid")
                ) {
                    items(filteredProducts, key = { it.productId }) { product ->
                        ProductCard(
                            product = product,
                            onAddToCart = {
                                viewModel.addItemToCurrentOrder(product = product, quantity = 1)
                            }
                        )
                    }
                }
            }

            // RIGHT PANE: Current Order Cart & Billing Panel (40% width)
            Surface(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Cart Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Order Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${orderDetails?.items?.sumOf { it.quantity } ?: 0} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Items List
                    val items = orderDetails?.items ?: emptyList()
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = "Empty order",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No items added yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Select from menu to add items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("order_items_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items, key = { it.orderItemId }) { item ->
                                OrderItemRow(
                                    item = item,
                                    onIncrease = {
                                        viewModel.updateOrderItemQuantity(item.orderItemId, item.quantity + 1)
                                    },
                                    onDecrease = {
                                        viewModel.updateOrderItemQuantity(item.orderItemId, item.quantity - 1)
                                    },
                                    onAddNotes = {
                                        selectedItemForNotes = item
                                        notesText = item.specialInstructions
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Financial Calculations Breakdown
                    if (order != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                                Text(CurrencyHelper.format(order.subtotal), style = MaterialTheme.typography.bodyMedium)
                            }

                            // Discount Row with Edit button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showDiscountDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Discount,
                                        contentDescription = "Discount",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (order.discountPercent > BigDecimal.ZERO)
                                            "Discount (${order.discountPercent}% - ${order.discountReason})"
                                        else "Apply Discount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = if (order.discountAmount > BigDecimal.ZERO)
                                        "-${CurrencyHelper.format(order.discountAmount)}"
                                    else "$0.00",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (order.discountAmount > BigDecimal.ZERO) PosSuccess else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tax (${order.taxRatePercent}%)", style = MaterialTheme.typography.bodySmall)
                                Text(CurrencyHelper.format(order.taxAmount), style = MaterialTheme.typography.bodySmall)
                            }

                            if (order.serviceChargeRatePercent > BigDecimal.ZERO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Service Charge (${order.serviceChargeRatePercent}%)", style = MaterialTheme.typography.bodySmall)
                                    Text(CurrencyHelper.format(order.serviceChargeAmount), style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Grand Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Payable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyHelper.format(order.grandTotal),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons: Save & Exit vs Proceed to Billing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .weight(0.4f)
                                .height(54.dp)
                                .testTag("save_and_exit_button")
                        ) {
                            Text("Save & Exit")
                        }

                        Button(
                            onClick = {
                                if (order != null && order.grandTotal > BigDecimal.ZERO) {
                                    onNavigateToBilling(order.orderId)
                                }
                            },
                            enabled = order != null && (orderDetails?.items?.isNotEmpty() == true),
                            colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.6f)
                                .height(54.dp)
                                .testTag("proceed_to_billing_button")
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = "Billing")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed to Billing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Receipt Preview Modal
    uiState.printPreviewContent?.let { previewText ->
        ReceiptPreviewDialog(
            formattedText = previewText,
            printerStatus = uiState.printerStatus,
            onDismiss = { viewModel.clearPrintPreview() },
            onPrintAgain = {
                viewModel.reprintBill(orderId)
            }
        )
    }

    // Special Instructions Dialog
    selectedItemForNotes?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForNotes = null },
            title = { Text("Special Preparation Instructions") },
            text = {
                Column {
                    Text(
                        text = "Item: ${item.productName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("e.g. Extra spicy, dressing on side, no onions...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_instructions_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateItemInstructions(item.orderItemId, notesText.trim())
                        selectedItemForNotes = null
                    },
                    modifier = Modifier.testTag("save_instructions_button")
                ) {
                    Text("Save Instruction")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedItemForNotes = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Apply Discount Dialog
    if (showDiscountDialog) {
        val discountOptions = listOf(5, 10, 15, 20)
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Manager / Promo Discount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Preset Discount:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        discountOptions.forEach { pct ->
                            Button(
                                onClick = { discountPercentInput = pct.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (discountPercentInput == pct.toString()) PosAmberPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (discountPercentInput == pct.toString()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$pct%")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = discountPercentInput,
                        onValueChange = { discountPercentInput = it },
                        label = { Text("Discount Percentage (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = discountReasonInput,
                        onValueChange = { discountReasonInput = it },
                        label = { Text("Reason for Discount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = try {
                            BigDecimal(discountPercentInput.trim())
                        } catch (_: Exception) {
                            BigDecimal.ZERO
                        }
                        viewModel.applyOrderDiscount(pct, discountReasonInput.trim())
                        showDiscountDialog = false
                    },
                    modifier = Modifier.testTag("confirm_discount_button")
                ) {
                    Text("Apply Discount")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddToCart)
            .testTag("product_card_${product.productId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (product.isCombo) {
                    Surface(
                        color = PosAmberPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Combo",
                                tint = PosAmberPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COMBO MEAL",
                                color = PosAmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyHelper.format(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosAmberPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = PosAmberPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onAddToCart)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Order",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(
    item: OrderItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAddNotes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${CurrencyHelper.format(item.unitPrice)} each",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = CurrencyHelper.format(item.lineTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (item.specialInstructions.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PosAmberPrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${item.specialInstructions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PosAmberPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Special instruction button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onAddNotes)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit Notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.specialInstructions.isBlank()) "+ Instruction" else "Edit Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quantity controls (- / qty / +)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onDecrease)
                            .testTag("decrease_qty_${item.orderItemId}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = if (item.quantity == 1) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onIncrease)
                            .testTag("increase_qty_${item.orderItemId}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
