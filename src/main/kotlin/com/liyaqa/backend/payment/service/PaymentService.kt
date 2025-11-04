package com.liyaqa.backend.payment.service

import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import com.liyaqa.backend.payment.data.PaymentTransactionRepository
import com.liyaqa.backend.payment.domain.PaymentTransaction
import com.liyaqa.backend.payment.domain.PaymentTransactionStatus
import com.liyaqa.backend.payment.domain.PaymentTransactionType
import com.liyaqa.backend.payment.gateway.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Payment service orchestrating payment operations across different gateways.
 *
 * This service provides a gateway-agnostic API for payment operations.
 * It handles transaction tracking, gateway selection, and business logic
 * while delegating actual payment processing to gateway implementations.
 *
 * Key features:
 * - Gateway selection based on configuration
 * - Transaction tracking and audit trail
 * - Idempotency support
 * - Refund validation
 * - Error handling and logging
 */
@Service
@Transactional
class PaymentService(
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val paymentGateways: List<PaymentGateway>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a payment intent for a booking.
     *
     * This creates a payment intent with the configured gateway and tracks
     * the transaction in our database. The client secret can be used by
     * the frontend to complete the payment.
     *
     * @param booking The booking to create payment for
     * @param member The member making the payment
     * @param captureMethod Whether to capture immediately or authorize only
     * @return Payment intent response with client secret
     */
    fun createBookingPayment(
        booking: Booking,
        member: Member,
        captureMethod: CaptureMethod = CaptureMethod.AUTOMATIC
    ): CreatePaymentResult {
        logger.info("Creating payment for booking ${booking.id} for member ${member.id}")

        // Get the appropriate gateway (default to first available, could be configured)
        val gateway = getDefaultGateway()

        // Generate unique transaction number
        val transactionNumber = generateTransactionNumber()

        // Generate idempotency key to prevent duplicate charges
        val idempotencyKey = generateIdempotencyKey(booking.id.toString(), member.id.toString())

        // Check for existing transaction with same idempotency key
        val existingTransaction = paymentTransactionRepository
            .findByTransactionNumber(transactionNumber)

        if (existingTransaction != null && existingTransaction.status != PaymentTransactionStatus.FAILED) {
            logger.warn("Payment already exists for booking ${booking.id}")
            return CreatePaymentResult(
                success = false,
                errorMessage = "Payment already initiated for this booking"
            )
        }

        // Create payment intent with gateway
        val request = CreatePaymentIntentRequest(
            amount = booking.finalPrice,
            currency = "USD",
            customerEmail = member.email,
            description = "Court booking at ${booking.court.name} - ${booking.startTime}",
            metadata = mapOf(
                "booking_id" to booking.id.toString(),
                "member_id" to member.id.toString(),
                "branch_id" to booking.branch.id.toString(),
                "facility_id" to booking.facility.id.toString()
            ),
            captureMethod = captureMethod
        )

        return when (val result = gateway.createPaymentIntent(request)) {
            is PaymentResult.Success -> {
                val response = result.data

                // Create transaction record
                val transaction = PaymentTransaction(
                    booking = booking,
                    membership = null,
                    member = member,
                    branch = booking.branch,
                    facility = booking.facility,
                    transactionNumber = transactionNumber,
                    transactionType = PaymentTransactionType.PAYMENT,
                    gateway = gateway.getGatewayType(),
                    gatewayPaymentId = response.paymentIntentId,
                    gatewayClientSecret = response.clientSecret,
                    amount = booking.finalPrice,
                    currency = request.currency,
                    status = mapIntentStatusToTransactionStatus(response.status),
                    description = request.description,
                    customerEmail = member.email,
                    idempotencyKey = idempotencyKey
                )
                transaction.tenantId = booking.tenantId

                paymentTransactionRepository.save(transaction)

                logger.info("Payment intent created: ${response.paymentIntentId} for booking ${booking.id}")

                CreatePaymentResult(
                    success = true,
                    transactionId = transaction.id,
                    transactionNumber = transaction.transactionNumber,
                    paymentIntentId = response.paymentIntentId,
                    clientSecret = response.clientSecret,
                    amount = booking.finalPrice,
                    currency = request.currency
                )
            }
            is PaymentResult.Failure -> {
                logger.error("Failed to create payment intent: ${result.error.message}")

                // Still create a failed transaction record for audit trail
                val transaction = PaymentTransaction(
                    booking = booking,
                    membership = null,
                    member = member,
                    branch = booking.branch,
                    facility = booking.facility,
                    transactionNumber = transactionNumber,
                    transactionType = PaymentTransactionType.PAYMENT,
                    gateway = gateway.getGatewayType(),
                    gatewayPaymentId = "",
                    amount = booking.finalPrice,
                    currency = request.currency,
                    status = PaymentTransactionStatus.FAILED,
                    description = request.description,
                    customerEmail = member.email,
                    errorCode = result.error.code,
                    errorMessage = result.error.message,
                    idempotencyKey = idempotencyKey
                )
                transaction.tenantId = booking.tenantId
                transaction.failedAt = Instant.now()

                paymentTransactionRepository.save(transaction)

                CreatePaymentResult(
                    success = false,
                    errorMessage = result.error.message,
                    errorCode = result.error.code
                )
            }
        }
    }

    /**
     * Capture a previously authorized payment.
     *
     * Use this when using MANUAL capture method. This finalizes the payment
     * and transfers funds.
     *
     * @param transactionId Our internal transaction ID
     * @return Result indicating success or failure
     */
    fun capturePayment(transactionId: UUID): CapturePaymentResult {
        val transaction = paymentTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("Transaction not found: $transactionId") }

        if (transaction.status != PaymentTransactionStatus.AUTHORIZED) {
            logger.warn("Cannot capture payment ${transaction.transactionNumber} with status ${transaction.status}")
            return CapturePaymentResult(
                success = false,
                errorMessage = "Payment cannot be captured in current status: ${transaction.status}"
            )
        }

        val gateway = getGatewayForTransaction(transaction)

        return when (val result = gateway.capturePayment(transaction.gatewayPaymentId)) {
            is PaymentResult.Success -> {
                val response = result.data

                transaction.markCaptured(
                    paymentMethod = response.paymentMethod,
                    brand = response.brand,
                    last4 = response.last4,
                    receiptUrl = response.receiptUrl
                )

                paymentTransactionRepository.save(transaction)

                logger.info("Payment captured: ${transaction.transactionNumber}")

                CapturePaymentResult(
                    success = true,
                    transactionNumber = transaction.transactionNumber,
                    amount = transaction.amount,
                    receiptUrl = transaction.receiptUrl
                )
            }
            is PaymentResult.Failure -> {
                logger.error("Failed to capture payment ${transaction.transactionNumber}: ${result.error.message}")

                transaction.markFailed(
                    errorCode = result.error.code,
                    errorMessage = result.error.message,
                    declineCode = result.error.declineCode
                )

                paymentTransactionRepository.save(transaction)

                CapturePaymentResult(
                    success = false,
                    errorMessage = result.error.message,
                    errorCode = result.error.code
                )
            }
        }
    }

    /**
     * Cancel a payment intent before it's captured.
     *
     * @param transactionId Our internal transaction ID
     * @return Result indicating success or failure
     */
    fun cancelPaymentIntent(transactionId: UUID): CancelPaymentResult {
        val transaction = paymentTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("Transaction not found: $transactionId") }

        if (transaction.status !in listOf(
                PaymentTransactionStatus.PENDING,
                PaymentTransactionStatus.AUTHORIZED
            )) {
            return CancelPaymentResult(
                success = false,
                errorMessage = "Payment cannot be canceled in current status: ${transaction.status}"
            )
        }

        val gateway = getGatewayForTransaction(transaction)

        return when (val result = gateway.cancelPaymentIntent(transaction.gatewayPaymentId)) {
            is PaymentResult.Success -> {
                transaction.status = PaymentTransactionStatus.CANCELED
                paymentTransactionRepository.save(transaction)

                logger.info("Payment canceled: ${transaction.transactionNumber}")

                CancelPaymentResult(success = true)
            }
            is PaymentResult.Failure -> {
                logger.error("Failed to cancel payment ${transaction.transactionNumber}: ${result.error.message}")

                CancelPaymentResult(
                    success = false,
                    errorMessage = result.error.message,
                    errorCode = result.error.code
                )
            }
        }
    }

    /**
     * Process a refund for a completed payment.
     *
     * Validates the refund amount and processes through the payment gateway.
     * Supports partial refunds.
     *
     * @param transactionId Transaction to refund
     * @param refundAmount Amount to refund (must be <= remaining refundable amount)
     * @param reason Reason for refund
     * @return Result with refund details
     */
    fun refundPayment(
        transactionId: UUID,
        refundAmount: BigDecimal,
        reason: RefundReason
    ): RefundPaymentResult {
        val transaction = paymentTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("Transaction not found: $transactionId") }

        // Validate refund
        if (!transaction.canBeRefunded()) {
            return RefundPaymentResult(
                success = false,
                errorMessage = "Transaction cannot be refunded. Status: ${transaction.status}"
            )
        }

        val remainingRefundable = transaction.getRemainingRefundableAmount()
        if (refundAmount > remainingRefundable) {
            return RefundPaymentResult(
                success = false,
                errorMessage = "Refund amount ($refundAmount) exceeds remaining refundable amount ($remainingRefundable)"
            )
        }

        if (refundAmount <= BigDecimal.ZERO) {
            return RefundPaymentResult(
                success = false,
                errorMessage = "Refund amount must be greater than zero"
            )
        }

        val gateway = getGatewayForTransaction(transaction)

        val request = RefundRequest(
            paymentIntentId = transaction.gatewayPaymentId,
            amount = refundAmount,
            reason = reason,
            metadata = mapOf(
                "original_transaction_id" to transaction.id.toString(),
                "original_transaction_number" to transaction.transactionNumber,
                "booking_id" to (transaction.booking?.id?.toString() ?: "")
            )
        )

        return when (val result = gateway.refundPayment(request)) {
            is PaymentResult.Success -> {
                val response = result.data

                // Update original transaction
                transaction.markRefunded(refundAmount, reason.name)
                paymentTransactionRepository.save(transaction)

                // Create refund transaction record
                val refundTransaction = PaymentTransaction(
                    booking = transaction.booking,
                    membership = transaction.membership,
                    member = transaction.member,
                    branch = transaction.branch,
                    facility = transaction.facility,
                    transactionNumber = generateTransactionNumber(),
                    transactionType = PaymentTransactionType.REFUND,
                    gateway = transaction.gateway,
                    gatewayPaymentId = response.refundId,
                    amount = refundAmount,
                    currency = transaction.currency,
                    status = mapRefundStatusToTransactionStatus(response.status),
                    description = "Refund for ${transaction.transactionNumber}",
                    customerEmail = transaction.customerEmail,
                    refundReason = reason.name,
                    parentTransaction = transaction
                )
                refundTransaction.tenantId = transaction.tenantId
                refundTransaction.refundedAt = Instant.now()

                paymentTransactionRepository.save(refundTransaction)

                logger.info("Refund processed: $refundAmount for transaction ${transaction.transactionNumber}")

                RefundPaymentResult(
                    success = true,
                    refundTransactionId = refundTransaction.id,
                    refundTransactionNumber = refundTransaction.transactionNumber,
                    refundAmount = refundAmount,
                    remainingRefundable = transaction.getRemainingRefundableAmount()
                )
            }
            is PaymentResult.Failure -> {
                logger.error("Failed to process refund for ${transaction.transactionNumber}: ${result.error.message}")

                RefundPaymentResult(
                    success = false,
                    errorMessage = result.error.message,
                    errorCode = result.error.code
                )
            }
        }
    }

    /**
     * Get payment transaction details.
     */
    fun getTransaction(transactionId: UUID): PaymentTransaction? {
        return paymentTransactionRepository.findById(transactionId).orElse(null)
    }

    /**
     * Get payment transaction by transaction number.
     */
    fun getTransactionByNumber(transactionNumber: String): PaymentTransaction? {
        return paymentTransactionRepository.findByTransactionNumber(transactionNumber)
    }

    /**
     * Get all transactions for a booking.
     */
    fun getBookingTransactions(bookingId: UUID): List<PaymentTransaction> {
        return paymentTransactionRepository.findByBookingId(bookingId)
    }

    /**
     * Get revenue for a branch in a date range.
     */
    fun getBranchRevenue(branchId: UUID, startDate: java.time.LocalDate, endDate: java.time.LocalDate): BigDecimal {
        return paymentTransactionRepository.getRevenueByBranchAndDateRange(branchId, startDate, endDate)
    }

    /**
     * Get refunds for a branch in a date range.
     */
    fun getBranchRefunds(branchId: UUID, startDate: java.time.LocalDate, endDate: java.time.LocalDate): BigDecimal {
        return paymentTransactionRepository.getRefundsByBranchAndDateRange(branchId, startDate, endDate)
    }

    // Helper methods

    /**
     * Get the default payment gateway.
     * In a real application, this could be configured per facility/branch.
     */
    private fun getDefaultGateway(): PaymentGateway {
        return paymentGateways.firstOrNull()
            ?: throw IllegalStateException("No payment gateways configured")
    }

    /**
     * Get the gateway that was used for a transaction.
     */
    private fun getGatewayForTransaction(transaction: PaymentTransaction): PaymentGateway {
        return paymentGateways.find { it.getGatewayType() == transaction.gateway }
            ?: throw IllegalStateException("Gateway ${transaction.gateway} not available")
    }

    /**
     * Generate unique transaction number.
     */
    private fun generateTransactionNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "TXN-$timestamp-$random"
    }

    /**
     * Generate idempotency key for payment.
     */
    private fun generateIdempotencyKey(bookingId: String, memberId: String): String {
        return "booking-$bookingId-member-$memberId-${System.currentTimeMillis()}"
    }

    /**
     * Map gateway payment intent status to our internal transaction status.
     */
    private fun mapIntentStatusToTransactionStatus(status: PaymentIntentStatus): PaymentTransactionStatus {
        return when (status) {
            PaymentIntentStatus.CREATED,
            PaymentIntentStatus.REQUIRES_PAYMENT_METHOD,
            PaymentIntentStatus.REQUIRES_CONFIRMATION,
            PaymentIntentStatus.REQUIRES_ACTION -> PaymentTransactionStatus.PENDING
            PaymentIntentStatus.PROCESSING -> PaymentTransactionStatus.PROCESSING
            PaymentIntentStatus.REQUIRES_CAPTURE -> PaymentTransactionStatus.AUTHORIZED
            PaymentIntentStatus.SUCCEEDED -> PaymentTransactionStatus.COMPLETED
            PaymentIntentStatus.CANCELED -> PaymentTransactionStatus.CANCELED
            PaymentIntentStatus.FAILED -> PaymentTransactionStatus.FAILED
        }
    }

    /**
     * Map gateway refund status to our internal transaction status.
     */
    private fun mapRefundStatusToTransactionStatus(status: RefundStatus): PaymentTransactionStatus {
        return when (status) {
            RefundStatus.PENDING -> PaymentTransactionStatus.PROCESSING
            RefundStatus.SUCCEEDED -> PaymentTransactionStatus.REFUNDED
            RefundStatus.FAILED -> PaymentTransactionStatus.FAILED
            RefundStatus.CANCELED -> PaymentTransactionStatus.CANCELED
        }
    }
}

/**
 * Result classes for payment operations.
 */

data class CreatePaymentResult(
    val success: Boolean,
    val transactionId: UUID? = null,
    val transactionNumber: String? = null,
    val paymentIntentId: String? = null,
    val clientSecret: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

data class CapturePaymentResult(
    val success: Boolean,
    val transactionNumber: String? = null,
    val amount: BigDecimal? = null,
    val receiptUrl: String? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

data class CancelPaymentResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

data class RefundPaymentResult(
    val success: Boolean,
    val refundTransactionId: UUID? = null,
    val refundTransactionNumber: String? = null,
    val refundAmount: BigDecimal? = null,
    val remainingRefundable: BigDecimal? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null
)
