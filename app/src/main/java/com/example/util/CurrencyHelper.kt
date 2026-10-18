package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object CurrencyHelper {
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

    fun toMoney(value: Double): BigDecimal {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    }

    fun toMoney(value: String): BigDecimal {
        return try {
            BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            ZERO
        }
    }

    fun format(amount: BigDecimal?, currencySymbol: String = "$"): String {
        val safe = (amount ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%s%.2f", currencySymbol, safe)
    }

    fun calculateLineTotal(unitPrice: BigDecimal, quantity: Int): BigDecimal {
        val qty = BigDecimal(quantity)
        return unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP)
    }

    data class BillCalculation(
        val subtotal: BigDecimal,
        val discountAmount: BigDecimal,
        val taxableAmount: BigDecimal,
        val taxAmount: BigDecimal,
        val serviceChargeAmount: BigDecimal,
        val grandTotal: BigDecimal
    )

    fun calculateBill(
        subtotal: BigDecimal,
        discountPercent: BigDecimal = BigDecimal.ZERO,
        flatDiscount: BigDecimal = BigDecimal.ZERO,
        taxRatePercent: BigDecimal = BigDecimal("8.25"),
        serviceChargeRatePercent: BigDecimal = BigDecimal("10.00")
    ): BillCalculation {
        val safeSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP)

        // Calculate discount
        var discount = ZERO
        if (discountPercent > BigDecimal.ZERO) {
            val pctFactor = discountPercent.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
            discount = safeSubtotal.multiply(pctFactor).setScale(2, RoundingMode.HALF_UP)
        } else if (flatDiscount > BigDecimal.ZERO) {
            discount = flatDiscount.setScale(2, RoundingMode.HALF_UP)
        }

        // Cap discount so it does not exceed subtotal
        if (discount > safeSubtotal) {
            discount = safeSubtotal
        }

        val afterDiscount = safeSubtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP)

        // Tax calculation on post-discount amount
        val taxFactor = taxRatePercent.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val tax = afterDiscount.multiply(taxFactor).setScale(2, RoundingMode.HALF_UP)

        // Service charge on post-discount amount
        val svcFactor = serviceChargeRatePercent.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val serviceCharge = afterDiscount.multiply(svcFactor).setScale(2, RoundingMode.HALF_UP)

        // Grand total
        val grandTotal = afterDiscount.add(tax).add(serviceCharge).setScale(2, RoundingMode.HALF_UP)

        return BillCalculation(
            subtotal = safeSubtotal,
            discountAmount = discount,
            taxableAmount = afterDiscount,
            taxAmount = tax,
            serviceChargeAmount = serviceCharge,
            grandTotal = grandTotal
        )
    }

    fun calculateChange(amountPaid: BigDecimal, grandTotal: BigDecimal): BigDecimal {
        val change = amountPaid.subtract(grandTotal).setScale(2, RoundingMode.HALF_UP)
        return if (change < BigDecimal.ZERO) ZERO else change
    }
}
