package com.example.hardware.printer

import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal

data class PrinterStatus(
    val isConnected: Boolean = true,
    val hasPaper: Boolean = true,
    val modelName: String = "Internal Thermal 80mm ESC/POS",
    val batteryPercent: Int = 95,
    val message: String = "Ready"
)

data class KitchenItem(
    val name: String,
    val quantity: Int,
    val specialInstructions: String = ""
)

data class KitchenTicket(
    val orderNumber: String,
    val tableName: String,
    val orderType: String,
    val items: List<KitchenItem>,
    val timestamp: Long,
    val operatorId: String,
    val notes: String = ""
)

data class BillItem(
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal
)

data class CustomerBill(
    val restaurantName: String,
    val restaurantAddress: String,
    val phone: String,
    val taxId: String,
    val orderNumber: String,
    val tableName: String,
    val orderType: String,
    val items: List<BillItem>,
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val taxRatePercent: BigDecimal,
    val taxAmount: BigDecimal,
    val serviceChargeRatePercent: BigDecimal,
    val serviceChargeAmount: BigDecimal,
    val grandTotal: BigDecimal,
    val timestamp: Long,
    val operatorId: String,
    val header: String,
    val footer: String
)

data class PaymentReceipt(
    val restaurantName: String,
    val restaurantAddress: String,
    val phone: String,
    val orderNumber: String,
    val tableName: String,
    val paymentReference: String,
    val paymentMethod: String,
    val amountDue: BigDecimal,
    val amountPaid: BigDecimal,
    val changeDue: BigDecimal,
    val cardBrand: String? = null,
    val maskedPan: String? = null,
    val authCode: String? = null,
    val terminalId: String? = null,
    val paymentStatus: String,
    val timestamp: Long,
    val operatorId: String,
    val footer: String
)

sealed class PrintResult {
    data class Success(val formattedTicket: String) : PrintResult()
    data class Error(val reason: String) : PrintResult()
}

/**
 * Hardware Abstraction for POS Receipt and Kitchen Printers.
 * Real deployments map this interface to ESC/POS thermal printers via:
 * - Sunmi Printer Service (AIDL)
 * - Epson ePOS SDK
 * - Star Micronics CloudPRNT / StarIO
 * - Bluetooth/USB POS 80mm ESC/POS protocol
 */
interface PosPrinterService {
    val statusFlow: StateFlow<PrinterStatus>
    val lastPrintedDocument: StateFlow<String?>

    suspend fun getStatus(): PrinterStatus
    suspend fun printKitchenTicket(ticket: KitchenTicket): PrintResult
    suspend fun printCustomerBill(bill: CustomerBill): PrintResult
    suspend fun printPaymentReceipt(receipt: PaymentReceipt): PrintResult
    suspend fun setPaperStatus(hasPaper: Boolean)
    suspend fun setConnectionStatus(isConnected: Boolean)
}

class MockPosPrinterService : PosPrinterService {
    private val _statusFlow = MutableStateFlow(PrinterStatus())
    override val statusFlow: StateFlow<PrinterStatus> = _statusFlow.asStateFlow()

    private val _lastPrintedDocument = MutableStateFlow<String?>(null)
    override val lastPrintedDocument: StateFlow<String?> = _lastPrintedDocument.asStateFlow()

    var delayMs: Long = 1000L

    override suspend fun getStatus(): PrinterStatus = _statusFlow.value

    override suspend fun setPaperStatus(hasPaper: Boolean) {
        _statusFlow.value = _statusFlow.value.copy(
            hasPaper = hasPaper,
            message = if (!hasPaper) "Out of paper (Roll empty)" else "Ready"
        )
    }

    override suspend fun setConnectionStatus(isConnected: Boolean) {
        _statusFlow.value = _statusFlow.value.copy(
            isConnected = isConnected,
            message = if (!isConnected) "Printer disconnected / offline" else "Ready"
        )
    }

    override suspend fun printKitchenTicket(ticket: KitchenTicket): PrintResult {
        val status = _statusFlow.value
        if (!status.isConnected) return PrintResult.Error("Kitchen Printer is disconnected")
        if (!status.hasPaper) return PrintResult.Error("Kitchen Printer is out of paper")

        delay(delayMs)

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("       KITCHEN ORDER TICKET     ")
        sb.appendLine("================================")
        sb.appendLine("Order: ${ticket.orderNumber}")
        sb.appendLine("Table: ${ticket.tableName} (${ticket.orderType})")
        sb.appendLine("Time : ${DateUtils.formatDateTime(ticket.timestamp)}")
        sb.appendLine("Staff: ${ticket.operatorId}")
        if (ticket.notes.isNotBlank()) {
            sb.appendLine("Notes: ${ticket.notes}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("QTY  ITEM")
        sb.appendLine("--------------------------------")
        ticket.items.forEach { item ->
            sb.appendLine(String.format("%-4d %-26s", item.quantity, item.name))
            if (item.specialInstructions.isNotBlank()) {
                sb.appendLine("     * INSTRUCTION: ${item.specialInstructions}")
            }
        }
        sb.appendLine("================================")
        sb.appendLine("         *** END TICKET ***     ")
        sb.appendLine("================================")

        val output = sb.toString()
        _lastPrintedDocument.value = output
        return PrintResult.Success(output)
    }

    override suspend fun printCustomerBill(bill: CustomerBill): PrintResult {
        val status = _statusFlow.value
        if (!status.isConnected) return PrintResult.Error("Printer is disconnected")
        if (!status.hasPaper) return PrintResult.Error("Printer out of paper")

        delay(delayMs)

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(bill.restaurantName.center(32))
        sb.appendLine(bill.restaurantAddress.center(32))
        sb.appendLine("Tel: ${bill.phone}".center(32))
        sb.appendLine("Tax ID: ${bill.taxId}".center(32))
        sb.appendLine("================================")
        sb.appendLine("Order #: ${bill.orderNumber}")
        sb.appendLine("Table  : ${bill.tableName} (${bill.orderType})")
        sb.appendLine("Date   : ${DateUtils.formatDateTime(bill.timestamp)}")
        sb.appendLine("Cashier: ${bill.operatorId}")
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-16s %3s %9s", "ITEM", "QTY", "TOTAL"))
        sb.appendLine("--------------------------------")
        bill.items.forEach { item ->
            val nameTruncated = if (item.name.length > 16) item.name.substring(0, 14) + ".." else item.name
            sb.appendLine(
                String.format(
                    "%-16s %3d %9s",
                    nameTruncated,
                    item.quantity,
                    CurrencyHelper.format(item.lineTotal)
                )
            )
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-20s %10s", "Subtotal:", CurrencyHelper.format(bill.subtotal)))
        if (bill.discountAmount > BigDecimal.ZERO) {
            sb.appendLine(String.format("%-20s %10s", "Discount:", "-${CurrencyHelper.format(bill.discountAmount)}"))
        }
        sb.appendLine(
            String.format(
                "%-20s %10s",
                "Tax (${bill.taxRatePercent}%):",
                CurrencyHelper.format(bill.taxAmount)
            )
        )
        sb.appendLine(
            String.format(
                "%-20s %10s",
                "Service (${bill.serviceChargeRatePercent}%):",
                CurrencyHelper.format(bill.serviceChargeAmount)
            )
        )
        sb.appendLine("================================")
        sb.appendLine(String.format("%-18s %12s", "TOTAL DUE:", CurrencyHelper.format(bill.grandTotal)))
        sb.appendLine("================================")
        sb.appendLine(bill.footer.center(32))
        sb.appendLine()

        val output = sb.toString()
        _lastPrintedDocument.value = output
        return PrintResult.Success(output)
    }

    override suspend fun printPaymentReceipt(receipt: PaymentReceipt): PrintResult {
        val status = _statusFlow.value
        if (!status.isConnected) return PrintResult.Error("Printer is disconnected")
        if (!status.hasPaper) return PrintResult.Error("Printer out of paper")

        delay(delayMs)

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(receipt.restaurantName.center(32))
        sb.appendLine(receipt.restaurantAddress.center(32))
        sb.appendLine("Tel: ${receipt.phone}".center(32))
        sb.appendLine("================================")
        sb.appendLine("       PAYMENT RECEIPT          ")
        sb.appendLine("================================")
        sb.appendLine("Order Ref : ${receipt.orderNumber}")
        sb.appendLine("Table     : ${receipt.tableName}")
        sb.appendLine("Date/Time : ${DateUtils.formatDateTime(receipt.timestamp)}")
        sb.appendLine("Txn Ref   : ${receipt.paymentReference}")
        sb.appendLine("Method    : ${receipt.paymentMethod}")
        if (receipt.paymentMethod == "CARD") {
            receipt.cardBrand?.let { sb.appendLine("Card Type : $it") }
            receipt.maskedPan?.let { sb.appendLine("Card PAN  : $it") }
            receipt.authCode?.let { sb.appendLine("Auth Code : $it") }
            receipt.terminalId?.let { sb.appendLine("Terminal  : $it") }
        }
        sb.appendLine("Status    : ${receipt.paymentStatus}")
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format("%-20s %10s", "Amount Due:", CurrencyHelper.format(receipt.amountDue)))
        sb.appendLine(String.format("%-20s %10s", "Amount Paid:", CurrencyHelper.format(receipt.amountPaid)))
        if (receipt.paymentMethod == "CASH") {
            sb.appendLine(String.format("%-20s %10s", "Change Given:", CurrencyHelper.format(receipt.changeDue)))
        }
        sb.appendLine("================================")
        sb.appendLine("       PAYMENT COMPLETED        ")
        sb.appendLine("================================")
        sb.appendLine(receipt.footer.center(32))
        sb.appendLine("Cashier: ${receipt.operatorId}")
        sb.appendLine()

        val output = sb.toString()
        _lastPrintedDocument.value = output
        return PrintResult.Success(output)
    }

    private fun String.center(width: Int): String {
        if (this.length >= width) return this.take(width)
        val pad = (width - this.length) / 2
        return " ".repeat(pad) + this + " ".repeat(width - this.length - pad)
    }
}
