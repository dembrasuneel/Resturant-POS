package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.CardSimulationResult
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.TableStatus
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String {
        return value?.toPlainString() ?: "0.00"
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal {
        return if (value.isNullOrBlank()) BigDecimal.ZERO else BigDecimal(value)
    }

    @TypeConverter
    fun fromTableStatus(value: TableStatus): String = value.name

    @TypeConverter
    fun toTableStatus(value: String): TableStatus = try {
        TableStatus.valueOf(value)
    } catch (_: Exception) {
        TableStatus.AVAILABLE
    }

    @TypeConverter
    fun fromOrderType(value: OrderType): String = value.name

    @TypeConverter
    fun toOrderType(value: String): OrderType = try {
        OrderType.valueOf(value)
    } catch (_: Exception) {
        OrderType.DINE_IN
    }

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = try {
        OrderStatus.valueOf(value)
    } catch (_: Exception) {
        OrderStatus.OPEN
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (_: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (_: Exception) {
        PaymentStatus.PENDING
    }

    @TypeConverter
    fun fromCardSimulation(value: CardSimulationResult): String = value.name

    @TypeConverter
    fun toCardSimulation(value: String): CardSimulationResult = try {
        CardSimulationResult.valueOf(value)
    } catch (_: Exception) {
        CardSimulationResult.ALWAYS_APPROVE
    }
}
