package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AuditLog
import com.example.data.model.Operator
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable

@Database(
    entities = [
        RestaurantTable::class,
        ProductCategory::class,
        Product::class,
        Order::class,
        OrderItem::class,
        PaymentTransaction::class,
        Operator::class,
        RestaurantSettings::class,
        AuditLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tableDao(): TableDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao
    abstract fun operatorDao(): OperatorDao
    abstract fun settingsDao(): SettingsDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
