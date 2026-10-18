package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    BILL_PENDING
}

enum class OrderType {
    DINE_IN,
    TAKEAWAY
}

enum class OrderStatus {
    OPEN,
    BILL_PENDING,
    PAYMENT_PROCESSING,
    PAID,
    CLOSED,
    PAYMENT_UNKNOWN
}

enum class PaymentMethod {
    CASH,
    CARD
}

enum class PaymentStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED,
    TIMEOUT,
    UNKNOWN
}

enum class CardSimulationResult {
    ALWAYS_APPROVE,
    RANDOM,
    ALWAYS_DECLINE,
    SIMULATE_TIMEOUT,
    SIMULATE_UNKNOWN
}

@Entity(tableName = "restaurant_tables")
data class RestaurantTable(
    @PrimaryKey(autoGenerate = true) val tableId: Long = 0,
    val tableNumber: Int,
    val name: String,
    val capacity: Int,
    val status: TableStatus = TableStatus.AVAILABLE,
    val currentOrderId: Long? = null,
    val activeAmount: BigDecimal = BigDecimal.ZERO
)

@Entity(tableName = "product_categories")
data class ProductCategory(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0,
    val name: String,
    val iconName: String = "restaurant",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = ProductCategory::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val productId: Long = 0,
    val categoryId: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val isAvailable: Boolean = true,
    val isCombo: Boolean = false,
    val comboItemsDescription: String = "",
    val imageUrl: String = ""
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val orderId: Long = 0,
    val orderNumber: String,
    val tableId: Long? = null,
    val tableName: String,
    val orderType: OrderType = OrderType.DINE_IN,
    val status: OrderStatus = OrderStatus.OPEN,
    val operatorId: String = "CASHIER-1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val discountReason: String = "",
    val taxRatePercent: BigDecimal = BigDecimal("8.25"),
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val serviceChargeRatePercent: BigDecimal = BigDecimal("10.00"),
    val serviceChargeAmount: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val notes: String = ""
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val orderItemId: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val lineTotal: BigDecimal,
    val specialInstructions: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["paymentReference"], unique = true)]
)
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val orderId: Long,
    val paymentReference: String,
    val paymentMethod: PaymentMethod,
    val amountDue: BigDecimal,
    val amountPaid: BigDecimal,
    val changeDue: BigDecimal = BigDecimal.ZERO,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val cardBrand: String? = null,
    val maskedPan: String? = null,
    val authCode: String? = null,
    val terminalId: String? = "TERM-POS-01",
    val sdkResponseCode: String? = null,
    val sdkResponseMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey val operatorId: String,
    val name: String,
    val role: String, // ADMIN, CASHIER, MANAGER
    val pin: String = "1234"
)

@Entity(tableName = "restaurant_settings")
data class RestaurantSettings(
    @PrimaryKey val id: Int = 1,
    val restaurantName: String = "Bistro & Grill Gourmet",
    val address: String = "742 Evergreen Terrace, Suite 100",
    val phone: String = "+1 (555) 019-2834",
    val taxId: String = "TX-8932014-B",
    val currencySymbol: String = "$",
    val defaultTaxPercent: BigDecimal = BigDecimal("8.25"),
    val defaultServiceChargePercent: BigDecimal = BigDecimal("10.00"),
    val receiptHeader: String = "Welcome to Bistro & Grill Gourmet!",
    val receiptFooter: String = "Thank you for dining with us! Please come again.",
    val autoPrintKitchen: Boolean = true,
    val autoPrintReceipt: Boolean = true,
    val mockPrinterDelayMs: Long = 1200L,
    val mockPrinterPaperOut: Boolean = false,
    val mockPrinterDisconnected: Boolean = false,
    val cardTerminalDelayMs: Long = 1800L,
    val cardSimulationResult: CardSimulationResult = CardSimulationResult.ALWAYS_APPROVE
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorId: String,
    val action: String,
    val details: String,
    val orderId: Long? = null
)
