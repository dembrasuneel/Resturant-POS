package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.CardSimulationResult
import com.example.data.model.Operator
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.hardware.payment.CardPaymentRequest
import com.example.hardware.payment.CardPaymentResult
import com.example.hardware.payment.CardPaymentService
import com.example.hardware.payment.MockCardPaymentService
import com.example.hardware.printer.BillItem
import com.example.hardware.printer.CustomerBill
import com.example.hardware.printer.KitchenItem
import com.example.hardware.printer.KitchenTicket
import com.example.hardware.printer.MockPosPrinterService
import com.example.hardware.printer.PaymentReceipt
import com.example.hardware.printer.PosPrinterService
import com.example.hardware.printer.PrintResult
import com.example.util.CurrencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderWithDetails(
    val order: Order,
    val items: List<OrderItem>,
    val table: RestaurantTable?
)

class PosRepository(
    private val database: AppDatabase,
    val printerService: PosPrinterService,
    val cardPaymentService: CardPaymentService
) {
    private val tableDao = database.tableDao()
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val orderDao = database.orderDao()
    private val orderItemDao = database.orderItemDao()
    private val paymentTransactionDao = database.paymentTransactionDao()
    private val operatorDao = database.operatorDao()
    private val settingsDao = database.settingsDao()
    private val auditLogDao = database.auditLogDao()

    val tablesFlow: Flow<List<RestaurantTable>> = tableDao.getAllTablesFlow()
    val categoriesFlow: Flow<List<ProductCategory>> = categoryDao.getAllCategoriesFlow()
    val productsFlow: Flow<List<Product>> = productDao.getAllProductsFlow()
    val activeOrdersFlow: Flow<List<Order>> = orderDao.getActiveOrdersFlow()
    val completedOrdersFlow: Flow<List<Order>> = orderDao.getCompletedOrdersFlow()
    val settingsFlow: Flow<RestaurantSettings?> = settingsDao.getSettingsFlow()
    val recentAuditLogsFlow: Flow<List<AuditLog>> = auditLogDao.getRecentLogsFlow()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        // 1. Settings
        val currentSettings = settingsDao.getSettings()
        if (currentSettings == null) {
            settingsDao.insertOrUpdateSettings(RestaurantSettings())
        }

        // 2. Operators
        val currentOperators = operatorDao.getAllOperatorsFlow().firstOrNull() ?: emptyList()
        if (currentOperators.isEmpty()) {
            operatorDao.insertOperators(
                listOf(
                    Operator("CASHIER-1", "Sarah Jenkins", "CASHIER", "1111"),
                    Operator("CASHIER-2", "David Vance", "CASHIER", "2222"),
                    Operator("MGR-10", "Elena Rostova", "MANAGER", "9999")
                )
            )
        }

        // 3. Tables
        val existingTables = tableDao.getAllTables()
        if (existingTables.isEmpty()) {
            val tables = listOf(
                RestaurantTable(tableNumber = 1, name = "Table 1", capacity = 2),
                RestaurantTable(tableNumber = 2, name = "Table 2", capacity = 2),
                RestaurantTable(tableNumber = 3, name = "Table 3", capacity = 4),
                RestaurantTable(tableNumber = 4, name = "Table 4", capacity = 4),
                RestaurantTable(tableNumber = 5, name = "Table 5", capacity = 4),
                RestaurantTable(tableNumber = 6, name = "Table 6", capacity = 6),
                RestaurantTable(tableNumber = 7, name = "Table 7", capacity = 6),
                RestaurantTable(tableNumber = 8, name = "Table 8", capacity = 8),
                RestaurantTable(tableNumber = 9, name = "Bar Stool 1", capacity = 1),
                RestaurantTable(tableNumber = 10, name = "Bar Stool 2", capacity = 1),
                RestaurantTable(tableNumber = 11, name = "Patio 1", capacity = 4),
                RestaurantTable(tableNumber = 12, name = "Patio 2", capacity = 4)
            )
            tableDao.insertTables(tables)
        }

        // 4. Categories & Menu Items
        val existingCategories = categoryDao.getAllCategories()
        if (existingCategories.isEmpty()) {
            val catCombos = categoryDao.insertCategory(
                ProductCategory(name = "Value Combos", iconName = "lunch_dining", sortOrder = 1)
            )
            val catBurgers = categoryDao.insertCategory(
                ProductCategory(name = "Burgers & Sandwiches", iconName = "fastfood", sortOrder = 2)
            )
            val catPizzas = categoryDao.insertCategory(
                ProductCategory(name = "Artisan Pizzas", iconName = "local_pizza", sortOrder = 3)
            )
            val catSides = categoryDao.insertCategory(
                ProductCategory(name = "Sides & Starters", iconName = "tapas", sortOrder = 4)
            )
            val catDrinks = categoryDao.insertCategory(
                ProductCategory(name = "Beverages & Cocktails", iconName = "local_bar", sortOrder = 5)
            )
            val catDesserts = categoryDao.insertCategory(
                ProductCategory(name = "Desserts", iconName = "cake", sortOrder = 6)
            )

            val products = listOf(
                // Combos
                Product(
                    categoryId = catCombos,
                    name = "Bistro Deluxe Combo",
                    description = "Double Angus Burger, Crispy Seasoned Fries & Fountain Drink",
                    price = BigDecimal("16.95"),
                    isCombo = true,
                    comboItemsDescription = "Double Angus Burger + Large Truffle Fries + 24oz Drink"
                ),
                Product(
                    categoryId = catCombos,
                    name = "Pizza Party Combo",
                    description = "12\" Pepperoni Pizza, 6 Buffalo Wings & 2 Craft Sodas",
                    price = BigDecimal("24.50"),
                    isCombo = true,
                    comboItemsDescription = "12\" Pizza + 6 Wings + 2 Fountain Sodas"
                ),
                Product(
                    categoryId = catCombos,
                    name = "Lunch Express Sandwich Combo",
                    description = "Smoked Turkey Club, Cup of Tomato Bisque & Iced Tea",
                    price = BigDecimal("14.25"),
                    isCombo = true,
                    comboItemsDescription = "Turkey Club + Cup of Soup + 16oz Tea"
                ),
                Product(
                    categoryId = catCombos,
                    name = "Family Feast Platter",
                    description = "2 Classic Burgers, 1 Cheese Pizza, Onion Rings, Large Fries & 4 Drinks",
                    price = BigDecimal("39.99"),
                    isCombo = true,
                    comboItemsDescription = "2 Burgers + 1 Medium Pizza + Rings + Fries + 4 Drinks"
                ),

                // Burgers
                Product(
                    categoryId = catBurgers,
                    name = "Prime Angus Cheeseburger",
                    description = "Brioche bun, aged cheddar, lettuce, heirloom tomato, secret bistro sauce",
                    price = BigDecimal("12.50")
                ),
                Product(
                    categoryId = catBurgers,
                    name = "Smokey Bacon BBQ Burger",
                    description = "Applewood smoked bacon, crispy onion straws, house BBQ sauce",
                    price = BigDecimal("13.95")
                ),
                Product(
                    categoryId = catBurgers,
                    name = "Crispy Buttermilk Chicken Sandwich",
                    description = "Hand-breaded chicken breast, spicy coleslaw, dill pickles, garlic aioli",
                    price = BigDecimal("11.75")
                ),
                Product(
                    categoryId = catBurgers,
                    name = "Portobello Truffle Veggie Burger",
                    description = "Grilled balsamic portobello cap, avocado, roasted peppers, truffle mayo",
                    price = BigDecimal("12.25")
                ),

                // Pizzas
                Product(
                    categoryId = catPizzas,
                    name = "Margherita Gourmet Pizza",
                    description = "San Marzano tomatoes, fresh mozzarella, extra virgin olive oil, fresh basil",
                    price = BigDecimal("14.50")
                ),
                Product(
                    categoryId = catPizzas,
                    name = "Rustic Pepperoni & Hot Honey",
                    description = "Artisanal spicy pepperoni, mozzarella, chili honey drizzle",
                    price = BigDecimal("16.50")
                ),
                Product(
                    categoryId = catPizzas,
                    name = "Wild Truffle & Mushroom Pizza",
                    description = "Roasted wild mushrooms, fontina, fresh thyme, white truffle crema",
                    price = BigDecimal("17.95")
                ),

                // Sides
                Product(
                    categoryId = catSides,
                    name = "Parmesan Truffle Fries",
                    description = "Hand-cut Idaho potatoes, white truffle oil, freshly grated parmesan",
                    price = BigDecimal("6.50")
                ),
                Product(
                    categoryId = catSides,
                    name = "Beer Battered Onion Rings",
                    description = "Colossal sweet onions with smoky chipotle dip",
                    price = BigDecimal("5.95")
                ),
                Product(
                    categoryId = catSides,
                    name = "Buffalo Chicken Wings (6 pcs)",
                    description = "Crispy wings tossed in classic spicy buffalo glaze with blue cheese dip",
                    price = BigDecimal("8.95")
                ),
                Product(
                    categoryId = catSides,
                    name = "Caesar Salad Cup",
                    description = "Crisp romaine, sourdough croutons, shaved parmesan",
                    price = BigDecimal("5.25")
                ),

                // Beverages
                Product(
                    categoryId = catDrinks,
                    name = "Fountain Soda (Refillable)",
                    description = "Coca-Cola, Diet Coke, Sprite, Fanta, Root Beer",
                    price = BigDecimal("3.25")
                ),
                Product(
                    categoryId = catDrinks,
                    name = "House Brewed Hibiscus Iced Tea",
                    description = "Refreshing organic floral herbal tea sweetened with cane sugar",
                    price = BigDecimal("3.75")
                ),
                Product(
                    categoryId = catDrinks,
                    name = "Craft Lemonade Cooler",
                    description = "Freshly squeezed Meyer lemons, crushed mint",
                    price = BigDecimal("4.25")
                ),
                Product(
                    categoryId = catDrinks,
                    name = "Espresso / Cappuccino",
                    description = "Double shot freshly pulled Italian roast with steamed whole milk",
                    price = BigDecimal("4.50")
                ),

                // Desserts
                Product(
                    categoryId = catDesserts,
                    name = "Molten Chocolate Lava Cake",
                    description = "Rich dark chocolate with warm liquid center & vanilla bean gelato",
                    price = BigDecimal("7.50")
                ),
                Product(
                    categoryId = catDesserts,
                    name = "Classic New York Cheesecake",
                    description = "Creamy Philadelphia cheesecake with fresh strawberry coulis",
                    price = BigDecimal("6.95")
                ),
                Product(
                    categoryId = catDesserts,
                    name = "Warm Apple Crisp",
                    description = "Cinnamon spiced Granny Smith apples with oat crumble & salted caramel",
                    price = BigDecimal("6.50")
                )
            )
            productDao.insertProducts(products)
        }
    }

    suspend fun startOrderForTable(
        tableId: Long,
        operatorId: String = "CASHIER-1"
    ): Long = withContext(Dispatchers.IO) {
        val table = tableDao.getTableById(tableId) ?: throw IllegalArgumentException("Table not found")

        // Check if table already has an active order
        val existingOrder = orderDao.getActiveOrderByTableId(tableId)
        if (existingOrder != null) {
            return@withContext existingOrder.orderId
        }

        val settings = settingsDao.getSettings() ?: RestaurantSettings()
        val orderNum = generateOrderNumber(OrderType.DINE_IN)

        val newOrder = Order(
            orderNumber = orderNum,
            tableId = table.tableId,
            tableName = table.name,
            orderType = OrderType.DINE_IN,
            status = OrderStatus.OPEN,
            operatorId = operatorId,
            taxRatePercent = settings.defaultTaxPercent,
            serviceChargeRatePercent = settings.defaultServiceChargePercent
        )

        val newOrderId = orderDao.insertOrder(newOrder)

        // Update table to OCCUPIED
        tableDao.updateTableStatus(
            tableId = table.tableId,
            status = TableStatus.OCCUPIED,
            currentOrderId = newOrderId,
            activeAmount = BigDecimal.ZERO
        )

        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "CREATE_ORDER",
                details = "Opened new dine-in order $orderNum for ${table.name}",
                orderId = newOrderId
            )
        )

        newOrderId
    }

    suspend fun startTakeawayOrder(
        operatorId: String = "CASHIER-1",
        customerName: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettings() ?: RestaurantSettings()
        val orderNum = generateOrderNumber(OrderType.TAKEAWAY)
        val nameLabel = if (customerName.isNotBlank()) "Takeaway: $customerName" else "Takeaway #$orderNum"

        val newOrder = Order(
            orderNumber = orderNum,
            tableId = null,
            tableName = nameLabel,
            orderType = OrderType.TAKEAWAY,
            status = OrderStatus.OPEN,
            operatorId = operatorId,
            taxRatePercent = settings.defaultTaxPercent,
            serviceChargeRatePercent = BigDecimal.ZERO // Takeaways typically don't charge dine-in service fee
        )

        val newOrderId = orderDao.insertOrder(newOrder)

        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "CREATE_TAKEAWAY",
                details = "Opened new takeaway order $orderNum ($nameLabel)",
                orderId = newOrderId
            )
        )

        newOrderId
    }

    suspend fun addItemToOrder(
        orderId: Long,
        product: Product,
        quantity: Int = 1,
        specialInstructions: String = "",
        operatorId: String = "CASHIER-1"
    ) = withContext(Dispatchers.IO) {
        val existingItems = orderItemDao.getItemsForOrder(orderId)
        val matchingItem = existingItems.firstOrNull {
            it.productId == product.productId && it.specialInstructions == specialInstructions
        }

        if (matchingItem != null) {
            val newQty = matchingItem.quantity + quantity
            val newLineTotal = CurrencyHelper.calculateLineTotal(product.price, newQty)
            orderItemDao.updateOrderItem(
                matchingItem.copy(
                    quantity = newQty,
                    lineTotal = newLineTotal
                )
            )
        } else {
            val lineTotal = CurrencyHelper.calculateLineTotal(product.price, quantity)
            orderItemDao.insertOrderItem(
                OrderItem(
                    orderId = orderId,
                    productId = product.productId,
                    productName = product.name,
                    unitPrice = product.price,
                    quantity = quantity,
                    lineTotal = lineTotal,
                    specialInstructions = specialInstructions
                )
            )
        }

        recalculateOrderTotals(orderId)

        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "ADD_ITEM",
                details = "Added $quantity x ${product.name} to order #$orderId",
                orderId = orderId
            )
        )
    }

    suspend fun updateItemQuantity(
        orderItemId: Long,
        newQuantity: Int,
        operatorId: String = "CASHIER-1"
    ) = withContext(Dispatchers.IO) {
        val item = orderItemDao.getItemById(orderItemId) ?: return@withContext
        if (newQuantity <= 0) {
            orderItemDao.deleteOrderItem(orderItemId)
            auditLogDao.insertLog(
                AuditLog(
                    operatorId = operatorId,
                    action = "REMOVE_ITEM",
                    details = "Removed ${item.productName} from order #${item.orderId}",
                    orderId = item.orderId
                )
            )
        } else {
            val updatedLineTotal = CurrencyHelper.calculateLineTotal(item.unitPrice, newQuantity)
            orderItemDao.updateOrderItem(
                item.copy(
                    quantity = newQuantity,
                    lineTotal = updatedLineTotal
                )
            )
            auditLogDao.insertLog(
                AuditLog(
                    operatorId = operatorId,
                    action = "UPDATE_QTY",
                    details = "Updated ${item.productName} qty to $newQuantity in order #${item.orderId}",
                    orderId = item.orderId
                )
            )
        }
        recalculateOrderTotals(item.orderId)
    }

    suspend fun updateSpecialInstructions(
        orderItemId: Long,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val item = orderItemDao.getItemById(orderItemId) ?: return@withContext
        orderItemDao.updateOrderItem(item.copy(specialInstructions = notes))
    }

    suspend fun applyDiscount(
        orderId: Long,
        percent: BigDecimal,
        reason: String,
        operatorId: String = "MGR-10"
    ) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        val updated = order.copy(
            discountPercent = percent,
            discountReason = reason
        )
        orderDao.updateOrder(updated)
        recalculateOrderTotals(orderId)

        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "APPLY_DISCOUNT",
                details = "Applied $percent% discount ($reason) to order #$orderId",
                orderId = orderId
            )
        )
    }

    suspend fun recalculateOrderTotals(orderId: Long) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        val items = orderItemDao.getItemsForOrder(orderId)

        var subtotal = BigDecimal.ZERO
        for (item in items) {
            subtotal = subtotal.add(item.lineTotal)
        }

        val calculation = CurrencyHelper.calculateBill(
            subtotal = subtotal,
            discountPercent = order.discountPercent,
            flatDiscount = order.discountAmount,
            taxRatePercent = order.taxRatePercent,
            serviceChargeRatePercent = order.serviceChargeRatePercent
        )

        val updatedOrder = order.copy(
            subtotal = calculation.subtotal,
            discountAmount = calculation.discountAmount,
            taxAmount = calculation.taxAmount,
            serviceChargeAmount = calculation.serviceChargeAmount,
            grandTotal = calculation.grandTotal,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.updateOrder(updatedOrder)

        // Update active amount on table
        order.tableId?.let { tableId ->
            val table = tableDao.getTableById(tableId)
            if (table != null) {
                tableDao.updateTableStatus(
                    tableId = tableId,
                    status = if (updatedOrder.status == OrderStatus.BILL_PENDING) TableStatus.BILL_PENDING else TableStatus.OCCUPIED,
                    currentOrderId = orderId,
                    activeAmount = calculation.grandTotal
                )
            }
        }
    }

    suspend fun markBillPending(orderId: Long, operatorId: String = "CASHIER-1") = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        orderDao.updateOrderStatus(orderId, OrderStatus.BILL_PENDING)
        order.tableId?.let { tableId ->
            tableDao.updateTableStatus(
                tableId = tableId,
                status = TableStatus.BILL_PENDING,
                currentOrderId = orderId,
                activeAmount = order.grandTotal
            )
        }
        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "BILL_PENDING",
                details = "Presented bill for order #${order.orderNumber}",
                orderId = orderId
            )
        )
    }

    suspend fun completeCashPayment(
        orderId: Long,
        amountReceived: BigDecimal,
        operatorId: String = "CASHIER-1"
    ): PaymentTransaction = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: throw IllegalStateException("Order not found")
        if (amountReceived < order.grandTotal) {
            throw IllegalArgumentException("Received cash amount (${CurrencyHelper.format(amountReceived)}) is less than amount due (${CurrencyHelper.format(order.grandTotal)})")
        }

        val changeDue = CurrencyHelper.calculateChange(amountReceived, order.grandTotal)
        val reference = MockCardPaymentService.generatePaymentReference().replace("TXN-", "TXN-CASH-")

        val transaction = PaymentTransaction(
            orderId = orderId,
            paymentReference = reference,
            paymentMethod = PaymentMethod.CASH,
            amountDue = order.grandTotal,
            amountPaid = amountReceived,
            changeDue = changeDue,
            status = PaymentStatus.APPROVED,
            terminalId = "CASH-REGISTER-01",
            completedAt = System.currentTimeMillis()
        )
        val txnId = paymentTransactionDao.insertTransaction(transaction)

        // Update Order to PAID and CLOSED
        orderDao.updateOrderStatus(orderId, OrderStatus.PAID)
        orderDao.updateOrderStatus(orderId, OrderStatus.CLOSED)

        // Release Table
        order.tableId?.let { tableId ->
            tableDao.updateTableStatus(
                tableId = tableId,
                status = TableStatus.AVAILABLE,
                currentOrderId = null,
                activeAmount = BigDecimal.ZERO
            )
        }

        auditLogDao.insertLog(
            AuditLog(
                operatorId = operatorId,
                action = "PAYMENT_CASH_COMPLETE",
                details = "Order #${order.orderNumber} paid via cash: Received ${CurrencyHelper.format(amountReceived)}, Change ${CurrencyHelper.format(changeDue)}",
                orderId = orderId
            )
        )

        transaction.copy(transactionId = txnId)
    }

    suspend fun processCardPayment(
        orderId: Long,
        operatorId: String = "CASHIER-1"
    ): Pair<PaymentTransaction, CardPaymentResult> = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: throw IllegalStateException("Order not found")

        // Lock order status to PAYMENT_PROCESSING
        orderDao.updateOrderStatus(orderId, OrderStatus.PAYMENT_PROCESSING)

        val reference = MockCardPaymentService.generatePaymentReference().replace("TXN-", "TXN-CARD-")

        val initialTxn = PaymentTransaction(
            orderId = orderId,
            paymentReference = reference,
            paymentMethod = PaymentMethod.CARD,
            amountDue = order.grandTotal,
            amountPaid = order.grandTotal,
            changeDue = BigDecimal.ZERO,
            status = PaymentStatus.PENDING,
            terminalId = "TERM-POS-01"
        )
        val txnId = paymentTransactionDao.insertTransaction(initialTxn)

        val request = CardPaymentRequest(
            paymentReference = reference,
            orderId = orderId,
            orderNumber = order.orderNumber,
            amount = order.grandTotal,
            currencyCode = "USD",
            operatorId = operatorId,
            terminalId = "TERM-POS-01"
        )

        val result = cardPaymentService.processPayment(request)

        val finalTxn = when (result) {
            is CardPaymentResult.Approved -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.APPROVED,
                    cardBrand = result.cardBrand,
                    maskedPan = result.maskedPan,
                    authCode = result.authCode,
                    terminalId = result.terminalId,
                    sdkResponseCode = "00",
                    sdkResponseMessage = "APPROVED",
                    completedAt = result.timestamp
                )
                paymentTransactionDao.updateTransaction(updated)

                // Close order & release table ONLY upon verified approval
                orderDao.updateOrderStatus(orderId, OrderStatus.PAID)
                orderDao.updateOrderStatus(orderId, OrderStatus.CLOSED)

                order.tableId?.let { tableId ->
                    tableDao.updateTableStatus(
                        tableId = tableId,
                        status = TableStatus.AVAILABLE,
                        currentOrderId = null,
                        activeAmount = BigDecimal.ZERO
                    )
                }

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_CARD_APPROVED",
                        details = "Card payment approved for ${CurrencyHelper.format(order.grandTotal)} (Auth: ${result.authCode}, Ref: $reference)",
                        orderId = orderId
                    )
                )

                updated
            }

            is CardPaymentResult.Declined -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.DECLINED,
                    sdkResponseCode = result.errorCode,
                    sdkResponseMessage = result.message,
                    completedAt = System.currentTimeMillis()
                )
                paymentTransactionDao.updateTransaction(updated)
                orderDao.updateOrderStatus(orderId, OrderStatus.BILL_PENDING) // Revert to Bill Pending

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_CARD_DECLINED",
                        details = "Card payment declined: ${result.message}",
                        orderId = orderId
                    )
                )
                updated
            }

            is CardPaymentResult.Cancelled -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.CANCELLED,
                    sdkResponseMessage = result.message,
                    completedAt = System.currentTimeMillis()
                )
                paymentTransactionDao.updateTransaction(updated)
                orderDao.updateOrderStatus(orderId, OrderStatus.BILL_PENDING)

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_CARD_CANCELLED",
                        details = "Payment cancelled on terminal by user",
                        orderId = orderId
                    )
                )
                updated
            }

            is CardPaymentResult.Timeout -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.TIMEOUT,
                    sdkResponseMessage = result.message,
                    completedAt = System.currentTimeMillis()
                )
                paymentTransactionDao.updateTransaction(updated)
                orderDao.updateOrderStatus(orderId, OrderStatus.BILL_PENDING)

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_CARD_TIMEOUT",
                        details = "Terminal timeout waiting for payment card",
                        orderId = orderId
                    )
                )
                updated
            }

            is CardPaymentResult.CommunicationError -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.UNKNOWN,
                    sdkResponseMessage = result.message,
                    completedAt = System.currentTimeMillis()
                )
                paymentTransactionDao.updateTransaction(updated)
                orderDao.updateOrderStatus(orderId, OrderStatus.PAYMENT_UNKNOWN)

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_COMM_ERROR",
                        details = "Lost communication with terminal for order #$orderId",
                        orderId = orderId
                    )
                )
                updated
            }

            is CardPaymentResult.Unknown -> {
                val updated = initialTxn.copy(
                    transactionId = txnId,
                    status = PaymentStatus.UNKNOWN,
                    sdkResponseMessage = result.message,
                    completedAt = System.currentTimeMillis()
                )
                paymentTransactionDao.updateTransaction(updated)
                orderDao.updateOrderStatus(orderId, OrderStatus.PAYMENT_UNKNOWN)

                auditLogDao.insertLog(
                    AuditLog(
                        operatorId = operatorId,
                        action = "PAYMENT_CARD_UNKNOWN",
                        details = "Payment uncertain, terminal inquiry required for reference $reference",
                        orderId = orderId
                    )
                )
                updated
            }
        }

        Pair(finalTxn, result)
    }

    suspend fun reconcileCardPayment(
        orderId: Long,
        paymentReference: String,
        operatorId: String = "MGR-10"
    ): CardPaymentResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: throw IllegalStateException("Order not found")
        val result = cardPaymentService.reconcileTransaction(paymentReference)

        if (result is CardPaymentResult.Approved) {
            paymentTransactionDao.updateStatusByReference(
                reference = paymentReference,
                status = PaymentStatus.APPROVED,
                completedAt = result.timestamp,
                code = "RECON_OK",
                message = "Reconciled successfully"
            )

            orderDao.updateOrderStatus(orderId, OrderStatus.PAID)
            orderDao.updateOrderStatus(orderId, OrderStatus.CLOSED)

            order.tableId?.let { tableId ->
                tableDao.updateTableStatus(
                    tableId = tableId,
                    status = TableStatus.AVAILABLE,
                    currentOrderId = null,
                    activeAmount = BigDecimal.ZERO
                )
            }

            auditLogDao.insertLog(
                AuditLog(
                    operatorId = operatorId,
                    action = "PAYMENT_RECONCILED_SUCCESS",
                    details = "Reconciled reference $paymentReference with gateway -> PAID",
                    orderId = orderId
                )
            )
        }

        result
    }

    suspend fun printKitchenTicket(orderId: Long): PrintResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext PrintResult.Error("Order not found")
        val items = orderItemDao.getItemsForOrder(orderId)
        if (items.isEmpty()) return@withContext PrintResult.Error("Order has no items to send to kitchen")

        val ticket = KitchenTicket(
            orderNumber = order.orderNumber,
            tableName = order.tableName,
            orderType = order.orderType.name,
            items = items.map {
                KitchenItem(
                    name = it.productName,
                    quantity = it.quantity,
                    specialInstructions = it.specialInstructions
                )
            },
            timestamp = System.currentTimeMillis(),
            operatorId = order.operatorId,
            notes = order.notes
        )

        val result = printerService.printKitchenTicket(ticket)
        if (result is PrintResult.Success) {
            auditLogDao.insertLog(
                AuditLog(
                    operatorId = order.operatorId,
                    action = "PRINT_KITCHEN_TICKET",
                    details = "Printed kitchen ticket for ${order.orderNumber}",
                    orderId = orderId
                )
            )
        }
        result
    }

    suspend fun printCustomerBill(orderId: Long): PrintResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext PrintResult.Error("Order not found")
        val items = orderItemDao.getItemsForOrder(orderId)
        val settings = settingsDao.getSettings() ?: RestaurantSettings()

        val bill = CustomerBill(
            restaurantName = settings.restaurantName,
            restaurantAddress = settings.address,
            phone = settings.phone,
            taxId = settings.taxId,
            orderNumber = order.orderNumber,
            tableName = order.tableName,
            orderType = order.orderType.name,
            items = items.map {
                BillItem(
                    name = it.productName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = it.lineTotal
                )
            },
            subtotal = order.subtotal,
            discountAmount = order.discountAmount,
            taxableAmount = order.subtotal.subtract(order.discountAmount),
            taxRatePercent = order.taxRatePercent,
            taxAmount = order.taxAmount,
            serviceChargeRatePercent = order.serviceChargeRatePercent,
            serviceChargeAmount = order.serviceChargeAmount,
            grandTotal = order.grandTotal,
            timestamp = System.currentTimeMillis(),
            operatorId = order.operatorId,
            header = settings.receiptHeader,
            footer = settings.receiptFooter
        )

        val result = printerService.printCustomerBill(bill)
        if (result is PrintResult.Success) {
            auditLogDao.insertLog(
                AuditLog(
                    operatorId = order.operatorId,
                    action = "PRINT_CUSTOMER_BILL",
                    details = "Printed bill for ${order.orderNumber}",
                    orderId = orderId
                )
            )
        }
        result
    }

    suspend fun printPaymentReceipt(orderId: Long): PrintResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext PrintResult.Error("Order not found")
        val settings = settingsDao.getSettings() ?: RestaurantSettings()
        val txns = paymentTransactionDao.getTransactionsForOrder(orderId)
        val lastApprovedTxn = txns.firstOrNull { it.status == PaymentStatus.APPROVED }
            ?: return@withContext PrintResult.Error("No approved payment transaction found for this order")

        val receipt = PaymentReceipt(
            restaurantName = settings.restaurantName,
            restaurantAddress = settings.address,
            phone = settings.phone,
            orderNumber = order.orderNumber,
            tableName = order.tableName,
            paymentReference = lastApprovedTxn.paymentReference,
            paymentMethod = lastApprovedTxn.paymentMethod.name,
            amountDue = lastApprovedTxn.amountDue,
            amountPaid = lastApprovedTxn.amountPaid,
            changeDue = lastApprovedTxn.changeDue,
            cardBrand = lastApprovedTxn.cardBrand,
            maskedPan = lastApprovedTxn.maskedPan,
            authCode = lastApprovedTxn.authCode,
            terminalId = lastApprovedTxn.terminalId,
            paymentStatus = lastApprovedTxn.status.name,
            timestamp = lastApprovedTxn.completedAt ?: System.currentTimeMillis(),
            operatorId = order.operatorId,
            footer = settings.receiptFooter
        )

        val result = printerService.printPaymentReceipt(receipt)
        if (result is PrintResult.Success) {
            auditLogDao.insertLog(
                AuditLog(
                    operatorId = order.operatorId,
                    action = "PRINT_PAYMENT_RECEIPT",
                    details = "Printed receipt for ${order.orderNumber} (Txn: ${lastApprovedTxn.paymentReference})",
                    orderId = orderId
                )
            )
        }
        result
    }

    suspend fun getOrderWithDetails(orderId: Long): OrderWithDetails? = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext null
        val items = orderItemDao.getItemsForOrder(orderId)
        val table = order.tableId?.let { tableDao.getTableById(it) }
        OrderWithDetails(order, items, table)
    }

    suspend fun updateSettings(settings: RestaurantSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(settings)
        (printerService as? MockPosPrinterService)?.let {
            it.delayMs = settings.mockPrinterDelayMs
            it.setPaperStatus(!settings.mockPrinterPaperOut)
            it.setConnectionStatus(!settings.mockPrinterDisconnected)
        }
        (cardPaymentService as? MockCardPaymentService)?.let {
            it.processingDelayMs = settings.cardTerminalDelayMs
            it.setSimulationMode(settings.cardSimulationResult)
        }
    }

    suspend fun addProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(productId: Long) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(productId)
    }

    suspend fun getAllApprovedTransactions(): List<PaymentTransaction> = withContext(Dispatchers.IO) {
        paymentTransactionDao.getAllApprovedTransactions()
    }

    private fun generateOrderNumber(type: OrderType): String {
        val datePart = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val randomPart = (1000 + (Math.random() * 8999).toInt()).toString()
        val prefix = if (type == OrderType.DINE_IN) "DIN" else "TAK"
        return "$prefix-$datePart-$randomPart"
    }
}
