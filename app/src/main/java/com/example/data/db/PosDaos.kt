package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLog
import com.example.data.model.Operator
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface TableDao {
    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    fun getAllTablesFlow(): Flow<List<RestaurantTable>>

    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    suspend fun getAllTables(): List<RestaurantTable>

    @Query("SELECT * FROM restaurant_tables WHERE tableId = :tableId LIMIT 1")
    suspend fun getTableById(tableId: Long): RestaurantTable?

    @Query("SELECT * FROM restaurant_tables WHERE tableNumber = :tableNumber LIMIT 1")
    suspend fun getTableByNumber(tableNumber: Int): RestaurantTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RestaurantTable): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<RestaurantTable>)

    @Update
    suspend fun updateTable(table: RestaurantTable)

    @Query("UPDATE restaurant_tables SET status = :status, currentOrderId = :currentOrderId, activeAmount = :activeAmount WHERE tableId = :tableId")
    suspend fun updateTableStatus(tableId: Long, status: TableStatus, currentOrderId: Long?, activeAmount: BigDecimal)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM product_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategoriesFlow(): Flow<List<ProductCategory>>

    @Query("SELECT * FROM product_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategories(): List<ProductCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ProductCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ProductCategory>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProductsByCategoryFlow(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProductsFlow(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE productId = :productId")
    suspend fun deleteProduct(productId: Long)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status IN ('OPEN', 'BILL_PENDING', 'PAYMENT_PROCESSING') ORDER BY updatedAt DESC")
    fun getActiveOrdersFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status IN ('PAID', 'CLOSED') ORDER BY updatedAt DESC")
    fun getCompletedOrdersFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: Long): Flow<Order?>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): Order?

    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status IN ('OPEN', 'BILL_PENDING', 'PAYMENT_PROCESSING') LIMIT 1")
    suspend fun getActiveOrderByTableId(tableId: Long): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
}

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY addedAt ASC")
    fun getItemsForOrderFlow(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY addedAt ASC")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItem>

    @Query("SELECT * FROM order_items WHERE orderItemId = :orderItemId LIMIT 1")
    suspend fun getItemById(orderItemId: Long): OrderItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Update
    suspend fun updateOrderItem(item: OrderItem)

    @Query("DELETE FROM order_items WHERE orderItemId = :orderItemId")
    suspend fun deleteOrderItem(orderItemId: Long)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteAllItemsForOrder(orderId: Long)
}

@Dao
interface PaymentTransactionDao {
    @Query("SELECT * FROM payment_transactions WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun getTransactionsForOrderFlow(orderId: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE orderId = :orderId ORDER BY createdAt DESC")
    suspend fun getTransactionsForOrder(orderId: Long): List<PaymentTransaction>

    @Query("SELECT * FROM payment_transactions WHERE paymentReference = :reference LIMIT 1")
    suspend fun getTransactionByReference(reference: String): PaymentTransaction?

    @Query("SELECT * FROM payment_transactions WHERE status = 'APPROVED' ORDER BY createdAt DESC")
    fun getAllApprovedTransactionsFlow(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE status = 'APPROVED' ORDER BY createdAt DESC")
    suspend fun getAllApprovedTransactions(): List<PaymentTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PaymentTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: PaymentTransaction)

    @Query("UPDATE payment_transactions SET status = :status, completedAt = :completedAt, sdkResponseCode = :code, sdkResponseMessage = :message WHERE paymentReference = :reference")
    suspend fun updateStatusByReference(reference: String, status: PaymentStatus, completedAt: Long, code: String?, message: String?)
}

@Dao
interface OperatorDao {
    @Query("SELECT * FROM operators")
    fun getAllOperatorsFlow(): Flow<List<Operator>>

    @Query("SELECT * FROM operators WHERE operatorId = :operatorId LIMIT 1")
    suspend fun getOperatorById(operatorId: String): Operator?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: Operator)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperators(operators: List<Operator>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM restaurant_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<RestaurantSettings?>

    @Query("SELECT * FROM restaurant_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): RestaurantSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: RestaurantSettings)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}
