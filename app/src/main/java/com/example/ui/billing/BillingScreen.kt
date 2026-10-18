package com.example.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.hardware.payment.CardPaymentResult
import com.example.hardware.payment.TerminalStep
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
fun BillingScreen(
    orderId: Long,
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit,
    onPaymentCompleted: () -> Unit
) {
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }

    var selectedPaymentTab by remember { mutableIntStateOf(0) } // 0: Cash, 1: Card
    var showCashConfirmDialog by remember { mutableStateOf(false) }

    val orderDetails = uiState.currentOrderDetails
    val order = orderDetails?.order
    val items = orderDetails?.items ?: emptyList()
    val grandTotal = order?.grandTotal ?: BigDecimal.ZERO

    val cashReceivedAmount = CurrencyHelper.toMoney(uiState.cashReceivedInput)
    val changeDue = CurrencyHelper.calculateChange(cashReceivedAmount, grandTotal)
    val isCashAmountSufficient = cashReceivedAmount >= grandTotal && grandTotal > BigDecimal.ZERO

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Checkout & Payment — ${order?.tableName ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Order: ${order?.orderNumber ?: ""} • Total Due: ${CurrencyHelper.format(grandTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !uiState.isCardProcessing,
                        modifier = Modifier.testTag("billing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Order",
                            tint = Color.White
                        )
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
            // LEFT COLUMN: Complete Order Verification (45% width)
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Order Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = order?.orderType?.name ?: "DINE_IN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Items list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items, key = { it.orderItemId }) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.quantity}x ${item.productName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (item.specialInstructions.isNotBlank()) {
                                            Text(
                                                text = "Note: ${item.specialInstructions}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = CurrencyHelper.format(item.lineTotal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Financial totals breakdown
                        if (order != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                                    Text(CurrencyHelper.format(order.subtotal), style = MaterialTheme.typography.bodySmall)
                                }

                                if (order.discountAmount > BigDecimal.ZERO) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount (${order.discountPercent}%)", style = MaterialTheme.typography.bodySmall, color = PosSuccess)
                                        Text("-${CurrencyHelper.format(order.discountAmount)}", style = MaterialTheme.typography.bodySmall, color = PosSuccess)
                                    }
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Grand Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyHelper.format(order.grandTotal),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Payment Processing Engine (55% width)
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Payment Method Selector Tabs
                TabRow(
                    selectedTabIndex = selectedPaymentTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedPaymentTab]),
                            color = PosAmberPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedPaymentTab == 0,
                        onClick = { if (!uiState.isCardProcessing) selectedPaymentTab = 0 },
                        enabled = !uiState.isCardProcessing,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = "Cash")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CASH PAYMENT", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("cash_tab")
                    )

                    Tab(
                        selected = selectedPaymentTab == 1,
                        onClick = { selectedPaymentTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = "Card")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INTEGRATED CARD POS", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("card_tab")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                if (selectedPaymentTab == 0) {
                    // CASH PAYMENT FLOW
                    CashPaymentPanel(
                        grandTotal = grandTotal,
                        cashInput = uiState.cashReceivedInput,
                        changeDue = changeDue,
                        isSufficient = isCashAmountSufficient,
                        onCashChanged = { viewModel.setCashReceived(it) },
                        onQuickCash = { amount ->
                            viewModel.setCashReceived(amount.toPlainString())
                        },
                        onConfirmClicked = {
                            showCashConfirmDialog = true
                        }
                    )
                } else {
                    // CARD PAYMENT FLOW
                    CardPaymentPanel(
                        grandTotal = grandTotal,
                        isProcessing = uiState.isCardProcessing,
                        terminalStep = uiState.terminalStep,
                        cardResult = uiState.lastCardResult,
                        attemptReference = uiState.currentPaymentReference,
                        onStartPayment = {
                            viewModel.startCardPayment {
                                onPaymentCompleted()
                            }
                        },
                        onReconcile = {
                            viewModel.reconcileCurrentCardPayment {
                                onPaymentCompleted()
                            }
                        }
                    )
                }
            }
        }
    }

    // Cash Confirmation Dialog
    if (showCashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCashConfirmDialog = false },
            title = { Text("Confirm Cash Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Due: ${CurrencyHelper.format(grandTotal)}", fontWeight = FontWeight.Bold)
                    Text("Cash Received: ${CurrencyHelper.format(cashReceivedAmount)}", fontWeight = FontWeight.Bold)
                    Text("Change to Return: ${CurrencyHelper.format(changeDue)}", color = PosSuccess, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Has the cash been verified and placed into the register?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCashConfirmDialog = false
                        viewModel.confirmCashPayment {
                            onPaymentCompleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                    modifier = Modifier.testTag("confirm_cash_collection_button")
                ) {
                    Text("Yes, Collect & Print Receipt")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCashConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Receipt Preview Dialog
    uiState.printPreviewContent?.let { previewText ->
        ReceiptPreviewDialog(
            formattedText = previewText,
            printerStatus = uiState.printerStatus,
            onDismiss = { viewModel.clearPrintPreview() },
            onPrintAgain = {
                viewModel.reprintReceipt(orderId)
            }
        )
    }
}

@Composable
fun CashPaymentPanel(
    grandTotal: BigDecimal,
    cashInput: String,
    changeDue: BigDecimal,
    isSufficient: Boolean,
    onCashChanged: (String) -> Unit,
    onQuickCash: (BigDecimal) -> Unit,
    onConfirmClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Cash Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick cash buttons
                Text("Quick Denominations:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onQuickCash(grandTotal) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_cash_exact"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("Exact", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }

                    listOf(20, 50, 100).forEach { denom ->
                        val denomBd = BigDecimal(denom)
                        Button(
                            onClick = { onQuickCash(denomBd) },
                            enabled = denomBd >= grandTotal,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_cash_$denom"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text("$$denom", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cash Received Input Field
                OutlinedTextField(
                    value = cashInput,
                    onValueChange = onCashChanged,
                    label = { Text("Cash Received ($)") },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_received_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Change Calculation Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSufficient) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSufficient) PosSuccess else Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isSufficient) "Change Due to Customer" else "Insufficient Amount",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSufficient) PosSuccess else Color(0xFFDC2626)
                            )
                            if (!isSufficient) {
                                Text(
                                    text = "Cash entered is less than total due",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        Text(
                            text = CurrencyHelper.format(changeDue),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSufficient) PosSuccess else Color(0xFFDC2626),
                            modifier = Modifier.testTag("change_due_display")
                        )
                    }
                }
            }

            // Confirm Cash Button
            Button(
                onClick = onConfirmClicked,
                enabled = isSufficient,
                colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_cash_payment_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Confirm Cash")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Cash & Print Receipt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CardPaymentPanel(
    grandTotal: BigDecimal,
    isProcessing: Boolean,
    terminalStep: TerminalStep,
    cardResult: CardPaymentResult?,
    attemptReference: String?,
    onStartPayment: () -> Unit,
    onReconcile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Integrated Card Terminal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = "PAX POSLink / EMV",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Terminal status and live feedback
                if (isProcessing) {
                    TerminalProcessingBox(step = terminalStep)
                } else if (cardResult != null) {
                    TerminalResultBox(result = cardResult, onReconcile = onReconcile)
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Card Terminal Ready",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Terminal Ready for Authorization",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tapping 'Send to Terminal' transmits the exact amount to the hardware SDK without exposing PAN/PIN to the app.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security notice compliant with payment safety rules
                Text(
                    text = "🔒 Payment Safety: PCI-DSS compliant. No cardholder PINs or CVVs are retained. Idempotency key attached to prevent duplicate billing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Charge Button
            Button(
                onClick = onStartPayment,
                enabled = !isProcessing && grandTotal > BigDecimal.ZERO,
                colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("send_to_card_terminal_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Communicating with Terminal...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = "Charge Card")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Send Charge ${CurrencyHelper.format(grandTotal)} to Terminal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalProcessingBox(step: TerminalStep) {
    val stepText = when (step) {
        TerminalStep.IDLE -> "Initializing terminal session..."
        TerminalStep.CONNECTING_TERMINAL -> "Establishing secure handshake with POS terminal..."
        TerminalStep.WAITING_FOR_CARD_INSERTION_OR_TAP -> "PLEASE INSERT, TAP, OR SWIPE CARD ON TERMINAL"
        TerminalStep.READING_CHIP_OR_CONTACTLESS -> "Reading EMV chip / NFC contactless data..."
        TerminalStep.AUTHORIZING_WITH_HOST -> "Authorizing transaction with merchant banking network..."
        TerminalStep.COMPLETED -> "Transaction completed."
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEFF6FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stepText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1E40AF)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Do not remove card or power off terminal",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
fun TerminalResultBox(
    result: CardPaymentResult,
    onReconcile: () -> Unit
) {
    when (result) {
        is CardPaymentResult.Approved -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFECFDF5),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosSuccess),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Approved", tint = PosSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAYMENT APPROVED", fontWeight = FontWeight.Bold, color = PosSuccess)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Auth Code: ${result.authCode}")
                    Text("Card: ${result.cardBrand} ${result.maskedPan}")
                    Text("Ref: ${result.reference}")
                }
            }
        }

        is CardPaymentResult.Declined -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Declined", tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRANSACTION DECLINED", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reason: ${result.message}", style = MaterialTheme.typography.bodyMedium)
                    Text("Please ask customer for alternative payment method.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        is CardPaymentResult.Timeout, is CardPaymentResult.CommunicationError, is CardPaymentResult.Unknown -> {
            val msg = when (result) {
                is CardPaymentResult.Timeout -> result.message
                is CardPaymentResult.CommunicationError -> result.message
                is CardPaymentResult.Unknown -> result.message
                else -> ""
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosAmberPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Uncertain", tint = PosAmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STATUS UNCERTAIN / INQUIRY REQUIRED", fontWeight = FontWeight.Bold, color = PosAmberPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onReconcile,
                        colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                        modifier = Modifier.testTag("reconcile_card_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reconcile")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconcile Status with Gateway")
                    }
                }
            }
        }

        is CardPaymentResult.Cancelled -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Transaction Cancelled", fontWeight = FontWeight.Bold)
                    Text(result.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
