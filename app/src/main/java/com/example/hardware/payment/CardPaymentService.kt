package com.example.hardware.payment

import com.example.data.model.CardSimulationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.util.UUID

data class CardPaymentRequest(
    val paymentReference: String,
    val orderId: Long,
    val orderNumber: String,
    val amount: BigDecimal,
    val currencyCode: String = "USD",
    val operatorId: String,
    val terminalId: String = "TERM-POS-01"
)

sealed class CardPaymentResult {
    data class Approved(
        val reference: String,
        val authCode: String,
        val cardBrand: String,
        val maskedPan: String,
        val terminalId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CardPaymentResult()

    data class Declined(
        val reference: String,
        val errorCode: String,
        val message: String
    ) : CardPaymentResult()

    data class Cancelled(
        val reference: String,
        val message: String = "Transaction cancelled by customer on terminal"
    ) : CardPaymentResult()

    data class Timeout(
        val reference: String,
        val message: String = "Payment terminal timeout waiting for card interaction"
    ) : CardPaymentResult()

    data class CommunicationError(
        val reference: String,
        val message: String = "Network / Serial communication lost with payment terminal"
    ) : CardPaymentResult()

    data class Unknown(
        val reference: String,
        val message: String = "Status uncertain. Terminal inquiry/reconciliation required before closing order."
    ) : CardPaymentResult()
}

enum class TerminalStep {
    IDLE,
    CONNECTING_TERMINAL,
    WAITING_FOR_CARD_INSERTION_OR_TAP,
    READING_CHIP_OR_CONTACTLESS,
    AUTHORIZING_WITH_HOST,
    COMPLETED
}

/**
 * Isolated Card Payment Terminal Hardware Interface.
 *
 * For Production Deployment:
 * Replace this implementation with the manufacturer's certified Android SDK:
 * - PAX POSLink SDK (A920 / A80 / E-series)
 * - Stripe Terminal Android SDK
 * - Square Reader SDK / Reader SDK for Android
 * - Castles Technology Saturn SDK
 * - Ingenico Tetra / Telium SDK via USB/Bluetooth
 * - Clover Connector Android SDK
 */
interface CardPaymentService {
    val stepFlow: StateFlow<TerminalStep>

    suspend fun processPayment(request: CardPaymentRequest): CardPaymentResult
    suspend fun cancelPayment(reference: String): Boolean
    suspend fun reconcileTransaction(reference: String): CardPaymentResult
    fun setSimulationMode(mode: CardSimulationResult)
}

class MockCardPaymentService : CardPaymentService {
    private val _stepFlow = MutableStateFlow(TerminalStep.IDLE)
    override val stepFlow: StateFlow<TerminalStep> = _stepFlow.asStateFlow()

    private var simulationMode: CardSimulationResult = CardSimulationResult.ALWAYS_APPROVE
    var processingDelayMs: Long = 1800L

    // Idempotency store to prevent duplicate charges with the same reference
    private val processedTransactions = mutableMapOf<String, CardPaymentResult>()

    override fun setSimulationMode(mode: CardSimulationResult) {
        this.simulationMode = mode
    }

    override suspend fun cancelPayment(reference: String): Boolean {
        if (_stepFlow.value != TerminalStep.IDLE && _stepFlow.value != TerminalStep.COMPLETED) {
            _stepFlow.value = TerminalStep.COMPLETED
            processedTransactions[reference] = CardPaymentResult.Cancelled(reference)
            return true
        }
        return false
    }

    override suspend fun processPayment(request: CardPaymentRequest): CardPaymentResult {
        // Idempotency check: if transaction was already processed with this reference, return recorded result
        processedTransactions[request.paymentReference]?.let { existingResult ->
            return existingResult
        }

        try {
            _stepFlow.value = TerminalStep.CONNECTING_TERMINAL
            delay(processingDelayMs / 3)

            _stepFlow.value = TerminalStep.WAITING_FOR_CARD_INSERTION_OR_TAP
            delay(processingDelayMs / 3)

            _stepFlow.value = TerminalStep.READING_CHIP_OR_CONTACTLESS
            delay(processingDelayMs / 3)

            _stepFlow.value = TerminalStep.AUTHORIZING_WITH_HOST
            delay(processingDelayMs / 2)

            val result = when (simulationMode) {
                CardSimulationResult.ALWAYS_APPROVE -> {
                    CardPaymentResult.Approved(
                        reference = request.paymentReference,
                        authCode = "AUTH" + (100000 + (Math.random() * 899999).toInt()),
                        cardBrand = "Visa",
                        maskedPan = "**** **** **** 4242",
                        terminalId = request.terminalId
                    )
                }
                CardSimulationResult.ALWAYS_DECLINE -> {
                    CardPaymentResult.Declined(
                        reference = request.paymentReference,
                        errorCode = "51",
                        message = "Declined: Insufficient Funds / Card issuer declined"
                    )
                }
                CardSimulationResult.SIMULATE_TIMEOUT -> {
                    CardPaymentResult.Timeout(
                        reference = request.paymentReference,
                        message = "Terminal timed out: Customer did not present card within 60s"
                    )
                }
                CardSimulationResult.SIMULATE_UNKNOWN -> {
                    CardPaymentResult.Unknown(
                        reference = request.paymentReference,
                        message = "Network packet lost during authorization. Gateway status unknown."
                    )
                }
                CardSimulationResult.RANDOM -> {
                    val rand = Math.random()
                    when {
                        rand < 0.75 -> CardPaymentResult.Approved(
                            reference = request.paymentReference,
                            authCode = "AUTH" + (100000 + (Math.random() * 899999).toInt()),
                            cardBrand = listOf("Mastercard", "Visa", "Amex").random(),
                            maskedPan = "**** **** **** " + (1000 + (Math.random() * 8999).toInt()),
                            terminalId = request.terminalId
                        )
                        rand < 0.90 -> CardPaymentResult.Declined(
                            reference = request.paymentReference,
                            errorCode = "05",
                            message = "Do Not Honor (Declined by Issuer)"
                        )
                        else -> CardPaymentResult.Timeout(
                            reference = request.paymentReference,
                            message = "Terminal connection timeout"
                        )
                    }
                }
            }

            processedTransactions[request.paymentReference] = result
            return result
        } finally {
            _stepFlow.value = TerminalStep.COMPLETED
        }
    }

    override suspend fun reconcileTransaction(reference: String): CardPaymentResult {
        // Query the gateway with the payment reference to resolve uncertain / unknown states safely
        delay(1000L)
        val existing = processedTransactions[reference]
        return if (existing is CardPaymentResult.Approved) {
            existing
        } else {
            // In a real gateway, this calls GET /v1/charges/{reference}
            // For mock recovery: if reference exists in unknown state, resolve to Approved or Cancelled safely
            val resolved = CardPaymentResult.Approved(
                reference = reference,
                authCode = "RECON" + (100000 + (Math.random() * 899999).toInt()),
                cardBrand = "Visa",
                maskedPan = "**** **** **** 1098",
                terminalId = "TERM-POS-01"
            )
            processedTransactions[reference] = resolved
            resolved
        }
    }

    companion object {
        fun generatePaymentReference(): String {
            return "TXN-" + System.currentTimeMillis().toString().takeLast(6) + "-" + UUID.randomUUID().toString().take(4).uppercase()
        }
    }
}
