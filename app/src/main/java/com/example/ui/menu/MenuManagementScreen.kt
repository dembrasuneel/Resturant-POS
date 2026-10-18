package com.example.ui.menu

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.Product
import com.example.ui.theme.PosAmberPrimary
import com.example.ui.theme.PosNavyDark
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyHelper
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateBack: () -> Unit
) {
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val filteredProducts = uiState.products.filter {
        selectedCatId == null || it.categoryId == selectedCatId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Menu & Pricing Management", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val defaultCat = uiState.categories.firstOrNull()?.categoryId ?: 1L
                            editingProduct = Product(
                                productId = 0L,
                                categoryId = defaultCat,
                                name = "",
                                description = "",
                                price = BigDecimal("9.99"),
                                isCombo = false
                            )
                            isCreatingNew = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosAmberPrimary),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("add_product_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Product")
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
            // Category selector chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCatId == null,
                        onClick = { selectedCatId = null },
                        label = { Text("All Categories (${uiState.products.size})") }
                    )
                }
                items(uiState.categories, key = { it.categoryId }) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.categoryId,
                        onClick = { selectedCatId = cat.categoryId },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Products list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.productId }) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    if (prod.isCombo) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PosAmberPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "COMBO",
                                                color = PosAmberPrimary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    prod.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (prod.isCombo && prod.comboItemsDescription.isNotBlank()) {
                                    Text(
                                        "Includes: ${prod.comboItemsDescription}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    CurrencyHelper.format(prod.price),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PosAmberPrimary
                                )

                                IconButton(
                                    onClick = {
                                        editingProduct = prod
                                        isCreatingNew = false
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(prod.productId)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    editingProduct?.let { product ->
        var nameInput by remember { mutableStateOf(product.name) }
        var descInput by remember { mutableStateOf(product.description) }
        var priceInput by remember { mutableStateOf(product.price.toPlainString()) }
        var isComboInput by remember { mutableStateOf(product.isCombo) }
        var comboIncludesInput by remember { mutableStateOf(product.comboItemsDescription) }
        var categoryIdInput by remember { mutableStateOf(product.categoryId) }

        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text(if (isCreatingNew) "Add New Product / Combo" else "Edit Menu Product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Price ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_price_input")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isComboInput,
                            onCheckedChange = { isComboInput = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("This is a Combo Deal")
                    }

                    if (isComboInput) {
                        OutlinedTextField(
                            value = comboIncludesInput,
                            onValueChange = { comboIncludesInput = it },
                            label = { Text("Combo Items (e.g. Burger + Fries + Drink)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = CurrencyHelper.toMoney(priceInput)
                        val updated = product.copy(
                            name = nameInput.trim(),
                            description = descInput.trim(),
                            price = parsedPrice,
                            isCombo = isComboInput,
                            comboItemsDescription = comboIncludesInput.trim(),
                            categoryId = categoryIdInput
                        )
                        viewModel.saveProduct(updated)
                        editingProduct = null
                    },
                    modifier = Modifier.testTag("save_product_button")
                ) {
                    Text("Save to Menu")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
