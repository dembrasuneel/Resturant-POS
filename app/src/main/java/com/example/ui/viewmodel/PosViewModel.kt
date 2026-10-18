package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.CardSimulationResult
import com.example.data.model.Operator
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.repository.OrderWithDetails
import com.example.data.repository.PosRepository
import com.example.hardware.payment.CardPaymentResult
import com.example.hardware.payment.TerminalStep
import com.example.hardware.printer.PrinterStatus
import com.example.hardware.printer.PrintResult
import com.example.util.CurrencyHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PosUiState(
    val tables: List<RestaurantTable> = emptyList(),
    val activeOrders: List<Order> = emptyList(),
    val completedOrders: List<Order> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val currentOrderDetails: OrderWithDetails? = null,
    val settings: RestaurantSettings = RestaurantSettings(),
    val currentOperator: Operator = Operator("CASHIER-1", "Sarah Jenkins", "CASHIER"),
    val cashReceivedInput: String = "",
    val isCardProcessing: Boolean = false,
    val terminalStep: TerminalStep = TerminalStep.IDLE,
    val lastCardResult: CardPaymentResult? = null,
    val currentPaymentReference: String? = null,
    val printPreviewContent: String? = null,
    val printerStatus: PrinterStatus = PrinterStatus(),
    val userNotification: String? = null,
    val errorNotification: String? = null,
    val recentAuditLogs: List<AuditLog> = emptyList()
)

class PosViewModel(
    private val repository: PosRepository
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _currentOrderDetails = MutableStateFlow<OrderWithDetails?>(null)
    private val _currentOperator = MutableStateFlow(Operator("CASHIER-1", "Sarah Jenkins", "CASHIER"))
    private val _cashReceivedInput = MutableStateFlow("")
    private val _isCardProcessing = MutableStateFlow(false)
    private val _lastCardResult = MutableStateFlow<CardPaymentResult?>(null)
    private val _currentPaymentReference = MutableStateFlow<String?>(null)
    private val _printPreviewContent = MutableStateFlow<String?>(null)
    private val _userNotification = MutableStateFlow<String?>(null)
    private val _errorNotification = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<PosUiState> = combine(
        repository.tablesFlow,
        repository.activeOrdersFlow,
        repository.completedOrdersFlow,
        repository.categoriesFlow,
        repository.productsFlow,
        _selectedCategoryId,
        _searchQuery,
        _currentOrderDetails,
        repository.settingsFlow,
        _currentOperator
    ) { args: Array<Any?> ->
        val tables = args[0] as List<RestaurantTable>
        val activeOrders = args[1] as List<Order>
        val completedOrders = args[2] as List<Order>
        val categories = args[3] as List<ProductCategory>
        val products = args[4] as List<Product>
        val selectedCat = args[5] as? Long
        val query = args[6] as String
        val orderDetails = args[7] as? OrderWithDetails
        val settings = args[8] as? RestaurantSettings
        val operator = args[9] as Operator

        PosUiState(
            tables = tables,
            activeOrders = activeOrders,
            completedOrders = completedOrders,
            categories = categories,
            products = products,
            selectedCategoryId = selectedCat ?: categories.firstOrNull()?.categoryId,
            searchQuery = query,
            currentOrderDetails = orderDetails,
            settings = settings ?: RestaurantSettings(),
            currentOperator = operator,
            cashReceivedInput = _cashReceivedInput.value,
            isCardProcessing = _isCardProcessing.value,
            terminalStep = repository.cardPaymentService.stepFlow.value,
            lastCardResult = _lastCardResult.value,
            currentPaymentReference = _currentPaymentReference.value,
            printPreviewContent = _printPreviewContent.value,
            printerStatus = repository.printerService.statusFlow.value,
            userNotification = _userNotification.value,
            errorNotification = _errorNotification.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState()
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }

        // Monitor terminal step progression
        viewModelScope.launch {
            repository.cardPaymentService.stepFlow.collect { step ->
                // Triggers uiState updates via combine if needed
            }
        }

        // Monitor printer status
        viewModelScope.launch {
            repository.printerService.statusFlow.collect { status ->
                // Triggers status updates
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openTableOrder(table: RestaurantTable, onNavToOrder: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val orderId = if (table.currentOrderId != null) {
                    table.currentOrderId
                } else {
                    repository.startOrderForTable(table.tableId, _currentOperator.value.operatorId)
                }
                loadOrderDetails(orderId)
                onNavToOrder(orderId)
            } catch (e: Exception) {
                _errorNotification.value = "Failed to open table order: ${e.message}"
            }
        }
    }

    fun startTakeawayOrder(customerName: String, onNavToOrder: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val orderId = repository.startTakeawayOrder(_currentOperator.value.operatorId, customerName)
                loadOrderDetails(orderId)
                onNavToOrder(orderId)
            } catch (e: Exception) {
                _errorNotification.value = "Failed to start takeaway: ${e.message}"
            }
        }
    }

    fun loadOrderDetails(orderId: Long) {
        viewModelScope.launch {
            val details = repository.getOrderWithDetails(orderId)
            _currentOrderDetails.value = details
            if (details != null) {
                // Pre-fill cash input with exact grand total for convenience
                _cashReceivedInput.value = details.order.grandTotal.toPlainString()
            }
        }
    }

    fun addItemToCurrentOrder(product: Product, quantity: Int = 1, specialInstructions: String = "") {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            try {
                repository.addItemToOrder(
                    orderId = current.order.orderId,
                    product = product,
                    quantity = quantity,
                    specialInstructions = specialInstructions,
                    operatorId = _currentOperator.value.operatorId
                )
                loadOrderDetails(current.order.orderId)
            } catch (e: Exception) {
                _errorNotification.value = "Could not add item: ${e.message}"
            }
        }
    }

    fun updateOrderItemQuantity(orderItemId: Long, newQuantity: Int) {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            try {
                repository.updateItemQuantity(orderItemId, newQuantity, _currentOperator.value.operatorId)
                loadOrderDetails(current.order.orderId)
            } catch (e: Exception) {
                _errorNotification.value = "Could not update item: ${e.message}"
            }
        }
    }

    fun updateItemInstructions(orderItemId: Long, instructions: String) {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            try {
                repository.updateSpecialInstructions(orderItemId, instructions)
                loadOrderDetails(current.order.orderId)
            } catch (e: Exception) {
                _errorNotification.value = "Could not update instructions: ${e.message}"
            }
        }
    }

    fun applyOrderDiscount(percent: BigDecimal, reason: String) {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            try {
                repository.applyDiscount(current.order.orderId, percent, reason, _currentOperator.value.operatorId)
                loadOrderDetails(current.order.orderId)
                _userNotification.value = "Applied $percent% discount ($reason)"
            } catch (e: Exception) {
                _errorNotification.value = "Could not apply discount: ${e.message}"
            }
        }
    }

    fun sendKitchenTicket(onSuccess: (() -> Unit)? = null) {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            when (val result = repository.printKitchenTicket(current.order.orderId)) {
                is PrintResult.Success -> {
                    _userNotification.value = "Kitchen order ticket sent to kitchen printer!"
                    _printPreviewContent.value = result.formattedTicket
                    onSuccess?.invoke()
                }
                is PrintResult.Error -> {
                    _errorNotification.value = "Kitchen print error: ${result.reason}"
                }
            }
        }
    }

    fun markBillPendingAndPrint(onSuccess: (() -> Unit)? = null) {
        val current = _currentOrderDetails.value ?: return
        viewModelScope.launch {
            repository.markBillPending(current.order.orderId, _currentOperator.value.operatorId)
            when (val result = repository.printCustomerBill(current.order.orderId)) {
                is PrintResult.Success -> {
                    _userNotification.value = "Customer bill printed!"
                    _printPreviewContent.value = result.formattedTicket
                    loadOrderDetails(current.order.orderId)
                    onSuccess?.invoke()
                }
                is PrintResult.Error -> {
                    _errorNotification.value = "Bill print error: ${result.reason}"
                    loadOrderDetails(current.order.orderId)
                }
            }
        }
    }

    fun setCashReceived(input: String) {
        _cashReceivedInput.value = input
    }

    fun confirmCashPayment(onPaymentSuccess: () -> Unit) {
        val current = _currentOrderDetails.value ?: return
        val amount = CurrencyHelper.toMoney(_cashReceivedInput.value)

        if (amount < current.order.grandTotal) {
            _errorNotification.value = "Amount received (${CurrencyHelper.format(amount)}) is less than total due (${CurrencyHelper.format(current.order.grandTotal)})"
            return
        }

        viewModelScope.launch {
            try {
                val txn = repository.completeCashPayment(
                    orderId = current.order.orderId,
                    amountReceived = amount,
                    operatorId = _currentOperator.value.operatorId
                )
                _userNotification.value = "Cash payment confirmed! Change due: ${CurrencyHelper.format(txn.changeDue)}"

                // Auto-print receipt if configured or preview it
                val printResult = repository.printPaymentReceipt(current.order.orderId)
                if (printResult is PrintResult.Success) {
                    _printPreviewContent.value = printResult.formattedTicket
                }

                loadOrderDetails(current.order.orderId)
                onPaymentSuccess()
            } catch (e: Exception) {
                _errorNotification.value = "Cash payment failed: ${e.message}"
            }
        }
    }

    fun startCardPayment(onPaymentSuccess: () -> Unit) {
        val current = _currentOrderDetails.value ?: return
        if (current.order.grandTotal <= BigDecimal.ZERO) {
            _errorNotification.value = "Cannot charge empty or $0.00 order"
            return
        }

        _isCardProcessing.value = true
        _lastCardResult.value = null

        viewModelScope.launch {
            try {
                val (txn, result) = repository.processCardPayment(
                    orderId = current.order.orderId,
                    operatorId = _currentOperator.value.operatorId
                )
                _isCardProcessing.value = false
                _lastCardResult.value = result
                _currentPaymentReference.value = txn.paymentReference

                when (result) {
                    is CardPaymentResult.Approved -> {
                        _userNotification.value = "Card payment approved! (Auth: ${result.authCode})"
                        val printResult = repository.printPaymentReceipt(current.order.orderId)
                        if (printResult is PrintResult.Success) {
                            _printPreviewContent.value = printResult.formattedTicket
                        }
                        loadOrderDetails(current.order.orderId)
                        onPaymentSuccess()
                    }
                    is CardPaymentResult.Declined -> {
                        _errorNotification.value = "Card payment declined: ${result.message}"
                        loadOrderDetails(current.order.orderId)
                    }
                    is CardPaymentResult.Cancelled -> {
                        _errorNotification.value = "Card payment was cancelled on terminal."
                        loadOrderDetails(current.order.orderId)
                    }
                    is CardPaymentResult.Timeout -> {
                        _errorNotification.value = "Payment terminal timed out. Please check terminal."
                        loadOrderDetails(current.order.orderId)
                    }
                    is CardPaymentResult.CommunicationError -> {
                        _errorNotification.value = "Communication error with card terminal! Reconcile before retrying."
                        loadOrderDetails(current.order.orderId)
                    }
                    is CardPaymentResult.Unknown -> {
                        _errorNotification.value = "Payment state unknown. Please perform Reconciliation Inquiry."
                        loadOrderDetails(current.order.orderId)
                    }
                }
            } catch (e: Exception) {
                _isCardProcessing.value = false
                _errorNotification.value = "Card terminal error: ${e.message}"
            }
        }
    }

    fun reconcileCurrentCardPayment(onPaymentSuccess: () -> Unit) {
        val current = _currentOrderDetails.value ?: return
        val ref = _currentPaymentReference.value ?: return

        viewModelScope.launch {
            try {
                _userNotification.value = "Querying payment gateway for reference $ref..."
                val result = repository.reconcileCardPayment(current.order.orderId, ref, _currentOperator.value.operatorId)
                _lastCardResult.value = result

                if (result is CardPaymentResult.Approved) {
                    _userNotification.value = "Reconciliation confirmed APPROVED! Order closed."
                    val printResult = repository.printPaymentReceipt(current.order.orderId)
                    if (printResult is PrintResult.Success) {
                        _printPreviewContent.value = printResult.formattedTicket
                    }
                    loadOrderDetails(current.order.orderId)
                    onPaymentSuccess()
                } else {
                    _errorNotification.value = "Reconciliation status: not approved"
                }
            } catch (e: Exception) {
                _errorNotification.value = "Reconciliation failed: ${e.message}"
            }
        }
    }

    fun reprintReceipt(orderId: Long) {
        viewModelScope.launch {
            when (val result = repository.printPaymentReceipt(orderId)) {
                is PrintResult.Success -> {
                    _printPreviewContent.value = result.formattedTicket
                    _userNotification.value = "Receipt sent to printer!"
                }
                is PrintResult.Error -> {
                    _errorNotification.value = "Print receipt error: ${result.reason}"
                }
            }
        }
    }

    fun reprintBill(orderId: Long) {
        viewModelScope.launch {
            when (val result = repository.printCustomerBill(orderId)) {
                is PrintResult.Success -> {
                    _printPreviewContent.value = result.formattedTicket
                    _userNotification.value = "Bill sent to printer!"
                }
                is PrintResult.Error -> {
                    _errorNotification.value = "Print bill error: ${result.reason}"
                }
            }
        }
    }

    fun clearPrintPreview() {
        _printPreviewContent.value = null
    }

    fun clearNotification() {
        _userNotification.value = null
        _errorNotification.value = null
    }

    fun updateSettings(newSettings: RestaurantSettings) {
        viewModelScope.launch {
            try {
                repository.updateSettings(newSettings)
                _userNotification.value = "Settings updated successfully"
            } catch (e: Exception) {
                _errorNotification.value = "Failed to update settings: ${e.message}"
            }
        }
    }

    fun switchOperator(operator: Operator) {
        _currentOperator.value = operator
        _userNotification.value = "Active operator switched to ${operator.name} (${operator.role})"
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            try {
                if (product.productId == 0L) {
                    repository.addProduct(product)
                    _userNotification.value = "Added product ${product.name}"
                } else {
                    repository.updateProduct(product)
                    _userNotification.value = "Updated product ${product.name}"
                }
            } catch (e: Exception) {
                _errorNotification.value = "Failed to save product: ${e.message}"
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                _userNotification.value = "Product removed from menu"
            } catch (e: Exception) {
                _errorNotification.value = "Failed to delete product: ${e.message}"
            }
        }
    }
}
