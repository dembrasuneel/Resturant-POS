package com.example

import com.example.data.model.CardSimulationResult
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.TableStatus
import com.example.hardware.payment.CardPaymentRequest
import com.example.hardware.payment.CardPaymentResult
import com.example.hardware.payment.MockCardPaymentService
import com.example.hardware.printer.MockPosPrinterService
import com.example.hardware.printer.PrintResult
import com.example.util.CurrencyHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PosBusinessLogicTest {

    @Test
    fun testCurrencyHelperLineTotal() {
        val unitPrice = BigDecimal("12.50")
        val qty = 3
        val lineTotal = CurrencyHelper.calculateLineTotal(unitPrice, qty)
        assertEquals(BigDecimal("37.50"), lineTotal)
    }

    @Test
    fun testBillCalculationStandard() {
        // Subtotal = $100.00, Tax = 8.25%, Service = 10.00%
        val subtotal = BigDecimal("100.00")
        val calc = CurrencyHelper.calculateBill(
            subtotal = subtotal,
            discountPercent = BigDecimal.ZERO,
            taxRatePercent = BigDecimal("8.25"),
            serviceChargeRatePercent = BigDecimal("10.00")
        )

        assertEquals(BigDecimal("100.00"), calc.subtotal)
        assertEquals(BigDecimal("0.00"), calc.discountAmount)
        assertEquals(BigDecimal("100.00"), calc.taxableAmount)
        assertEquals(BigDecimal("8.25"), calc.taxAmount)
        assertEquals(BigDecimal("10.00"), calc.serviceChargeAmount)
        // Grand Total = 100.00 + 8.25 + 10.00 = 118.25
        assertEquals(BigDecimal("118.25"), calc.grandTotal)
    }

    @Test
    fun testBillCalculationWithPercentageDiscount() {
        // Subtotal = $50.00, 10% Discount = $5.00, Taxable = $45.00
        // Tax 8.25% on 45.00 = 3.7125 -> 3.71
        // Service 10% on 45.00 = 4.50
        // Total = 45.00 + 3.71 + 4.50 = 53.21
        val subtotal = BigDecimal("50.00")
        val calc = CurrencyHelper.calculateBill(
            subtotal = subtotal,
            discountPercent = BigDecimal("10.00"),
            taxRatePercent = BigDecimal("8.25"),
            serviceChargeRatePercent = BigDecimal("10.00")
        )

        assertEquals(BigDecimal("5.00"), calc.discountAmount)
        assertEquals(BigDecimal("45.00"), calc.taxableAmount)
        assertEquals(BigDecimal("3.71"), calc.taxAmount)
        assertEquals(BigDecimal("4.50"), calc.serviceChargeAmount)
        assertEquals(BigDecimal("53.21"), calc.grandTotal)
    }

    @Test
    fun testCashChangeCalculation() {
        val grandTotal = BigDecimal("43.25")
        val cashReceived = BigDecimal("50.00")
        val change = CurrencyHelper.calculateChange(cashReceived, grandTotal)
        assertEquals(BigDecimal("6.75"), change)

        // Exact cash
        val exactChange = CurrencyHelper.calculateChange(BigDecimal("43.25"), grandTotal)
        assertEquals(BigDecimal("0.00"), exactChange)

        // Insufficient cash gives 0.00 change
        val underpaidChange = CurrencyHelper.calculateChange(BigDecimal("40.00"), grandTotal)
        assertEquals(BigDecimal("0.00"), underpaidChange)
    }

    @Test
    fun testCardPaymentTerminalFlow() = runTest {
        val cardService = MockCardPaymentService()
        cardService.processingDelayMs = 10L // Fast for testing
        cardService.setSimulationMode(CardSimulationResult.ALWAYS_APPROVE)

        val request = CardPaymentRequest(
            paymentReference = "TXN-TEST-1001",
            orderId = 1L,
            orderNumber = "DIN-261018-001",
            amount = BigDecimal("55.50"),
            operatorId = "CASHIER-1"
        )

        val result = cardService.processPayment(request)
        assertTrue(result is CardPaymentResult.Approved)

        val approved = result as CardPaymentResult.Approved
        assertEquals("TXN-TEST-1001", approved.reference)
        assertNotNull(approved.authCode)

        // Test Idempotency: Re-submitting the same reference returns identical result
        val secondResult = cardService.processPayment(request)
        assertEquals(result, secondResult)
    }

    @Test
    fun testCardPaymentDeclineSimulation() = runTest {
        val cardService = MockCardPaymentService()
        cardService.processingDelayMs = 10L
        cardService.setSimulationMode(CardSimulationResult.ALWAYS_DECLINE)

        val request = CardPaymentRequest(
            paymentReference = "TXN-TEST-1002",
            orderId = 2L,
            orderNumber = "DIN-261018-002",
            amount = BigDecimal("30.00"),
            operatorId = "CASHIER-1"
        )

        val result = cardService.processPayment(request)
        assertTrue(result is CardPaymentResult.Declined)
    }

    @Test
    fun testCardPaymentReconciliation() = runTest {
        val cardService = MockCardPaymentService()
        cardService.processingDelayMs = 10L
        cardService.setSimulationMode(CardSimulationResult.SIMULATE_UNKNOWN)

        val ref = "TXN-TEST-UNKNOWN-1"
        val request = CardPaymentRequest(
            paymentReference = ref,
            orderId = 3L,
            orderNumber = "DIN-261018-003",
            amount = BigDecimal("40.00"),
            operatorId = "CASHIER-1"
        )

        val initialResult = cardService.processPayment(request)
        assertTrue(initialResult is CardPaymentResult.Unknown)

        // Reconcile transaction
        val reconciledResult = cardService.reconcileTransaction(ref)
        assertTrue(reconciledResult is CardPaymentResult.Approved)
    }

    @Test
    fun testPrinterServiceFaultHandling() = runTest {
        val printer = MockPosPrinterService()
        printer.delayMs = 10L

        // Normal status
        assertTrue(printer.getStatus().isConnected)
        assertTrue(printer.getStatus().hasPaper)

        // Paper out simulation
        printer.setPaperStatus(false)
        val billResult = printer.printCustomerBill(
            com.example.hardware.printer.CustomerBill(
                restaurantName = "Test Bistro",
                restaurantAddress = "123 Main St",
                phone = "555-1234",
                taxId = "TX-123",
                orderNumber = "ORD-1",
                tableName = "Table 1",
                orderType = "DINE_IN",
                items = emptyList(),
                subtotal = BigDecimal("10.00"),
                discountAmount = BigDecimal.ZERO,
                taxableAmount = BigDecimal("10.00"),
                taxRatePercent = BigDecimal("8.25"),
                taxAmount = BigDecimal("0.83"),
                serviceChargeRatePercent = BigDecimal("10.00"),
                serviceChargeAmount = BigDecimal("1.00"),
                grandTotal = BigDecimal("11.83"),
                timestamp = System.currentTimeMillis(),
                operatorId = "CASHIER-1",
                header = "Welcome",
                footer = "Thank you"
            )
        )
        assertTrue(billResult is PrintResult.Error)
        assertEquals("Printer out of paper", (billResult as PrintResult.Error).reason)
    }
}
