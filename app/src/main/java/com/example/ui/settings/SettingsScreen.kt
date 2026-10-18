package com.example.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import com.example.data.model.CardSimulationResult
import com.example.data.model.RestaurantSettings
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit
) {
    val initialSettings = uiState.settings

    var restaurantName by remember { mutableStateOf(initialSettings.restaurantName) }
    var address by remember { mutableStateOf(initialSettings.address) }
    var phone by remember { mutableStateOf(initialSettings.phone) }
    var taxId by remember { mutableStateOf(initialSettings.taxId) }
    var taxRate by remember { mutableStateOf(initialSettings.defaultTaxPercent.toPlainString()) }
    var serviceChargeRate by remember { mutableStateOf(initialSettings.defaultServiceChargePercent.toPlainString()) }
    var receiptHeader by remember { mutableStateOf(initialSettings.receiptHeader) }
    var receiptFooter by remember { mutableStateOf(initialSettings.receiptFooter) }

    var paperOutSim by remember { mutableStateOf(initialSettings.mockPrinterPaperOut) }
    var printerDisconnectSim by remember { mutableStateOf(initialSettings.mockPrinterDisconnected) }
    var cardSimResult by remember { mutableStateOf(initialSettings.cardSimulationResult) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("POS Terminal Settings & Hardware Config", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val newSettings = initialSettings.copy(
                                restaurantName = restaurantName.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                taxId = taxId.trim(),
                                defaultTaxPercent = CurrencyHelper.toMoney(taxRate),
                                defaultServiceChargePercent = CurrencyHelper.toMoney(serviceChargeRate),
                                receiptHeader = receiptHeader.trim(),
                                receiptFooter = receiptFooter.trim(),
                                mockPrinterPaperOut = paperOutSim,
                                mockPrinterDisconnected = printerDisconnectSim,
                                cardSimulationResult = cardSimResult
                            )
                            viewModel.updateSettings(newSettings)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Restaurant Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Store, contentDescription = "Store", tint = PosAmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurant Profile & Receipt Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Restaurant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_restaurant_name")
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text("Tax Registration ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { taxRate = it },
                            label = { Text("Sales Tax Rate (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("settings_tax_rate")
                        )

                        OutlinedTextField(
                            value = serviceChargeRate,
                            onValueChange = { serviceChargeRate = it },
                            label = { Text("Dine-In Service Charge (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("settings_service_rate")
                        )
                    }

                    OutlinedTextField(
                        value = receiptHeader,
                        onValueChange = { receiptHeader = it },
                        label = { Text("Receipt Header Greeting") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Receipt Footer Message") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 2: Hardware Testing & Error Emulation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CreditCard, contentDescription = "Terminal Hardware", tint = PosAmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Payment Terminal Emulation & Error Simulation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Test real-world terminal behaviors such as issuer decline, connection timeout, and uncertain status:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Pair(CardSimulationResult.ALWAYS_APPROVE, "Always Approve (Standard Success)"),
                            Pair(CardSimulationResult.RANDOM, "Random Real-world Mix (75% Approve, 15% Decline, 10% Timeout)"),
                            Pair(CardSimulationResult.ALWAYS_DECLINE, "Simulate Card Issuer Decline (Insufficient Funds)"),
                            Pair(CardSimulationResult.SIMULATE_TIMEOUT, "Simulate Customer Timeout (Card not tapped)"),
                            Pair(CardSimulationResult.SIMULATE_UNKNOWN, "Simulate Network Loss / Uncertain State (Test Reconciliation)")
                        ).forEach { (mode, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = cardSimResult == mode,
                                    onClick = { cardSimResult = mode }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Printer Hardware", tint = PosAmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thermal Receipt Printer Fault Testing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Simulate Printer Out of Paper", fontWeight = FontWeight.Medium)
                            Text("Throws paper empty error when attempting to print", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = paperOutSim,
                            onCheckedChange = { paperOutSim = it },
                            modifier = Modifier.testTag("toggle_paper_out")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Simulate Printer Disconnection", fontWeight = FontWeight.Medium)
                            Text("Throws printer offline / communication error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = printerDisconnectSim,
                            onCheckedChange = { printerDisconnectSim = it },
                            modifier = Modifier.testTag("toggle_printer_disconnect")
                        )
                    }
                }
            }

            // Section 3: Hardware Manufacturer Integration Architecture Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "SDK Info", tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hardware Integration Architecture Guide", fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                    }
                    Text(
                        "• Card Payment Terminal: This app separates payment orchestration via the 'CardPaymentService' interface. For production deployment, implement this interface using your manufacturer's certified SDK (e.g. PAX POSLink, Stripe Terminal Android SDK, Square Reader SDK, Castles Saturn, or Clover Connector).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• ESC/POS Receipt Printer: The 'PosPrinterService' interface decouples printing logic from hardware. Production printers (Sunmi internal AIDL service, Epson ePOS SDK, Star Micronics, or raw USB OTG/Bluetooth 80mm ESC/POS) plug directly into this interface.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
