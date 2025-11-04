package com.liyaqa.backend.payment.gateway

import java.math.BigDecimal

/**
 * Payment Gateway abstraction interface.
 *
 * This interface defines the contract that all payment gateway implementations must follow.
 * It uses the Strategy pattern to allow easy swapping of payment providers (Stripe, PayPal, etc.)
 * without changing business logic.
 *
 * Design Philosophy:
 * - Gateway-agnostic: Business logic doesn't depend on specific gateway implementation
 * - Testable: Easy to mock for testing
 * - Extensible: New gateways can be added by implementing this interface
 * - Secure: Sensitive operations return results, never throw business exceptions
 */
interface PaymentGateway {

    /**
     * Get the gateway type (STRIPE, PAYPAL, etc.)
     */
    fun getGatewayType(): PaymentGatewayType

    /**
     * Create a payment intent/session for a booking.
     *
     * This prepares the payment but doesn't charge the customer yet.
     * Returns a PaymentIntent with client_secret that can be used by frontend.
     *
     * @param request Payment intent creation request
     * @return Result containing PaymentIntentResponse or error
     */
    fun createPaymentIntent(request: CreatePaymentIntentRequest): PaymentResult<PaymentIntentResponse>

    /**
     * Capture/confirm a payment that was previously authorized.
     *
     * @param paymentIntentId Gateway-specific payment intent ID
     * @return Result containing PaymentResponse or error
     */
    fun capturePayment(paymentIntentId: String): PaymentResult<PaymentResponse>

    /**
     * Cancel a payment intent before capture.
     *
     * @param paymentIntentId Gateway-specific payment intent ID
     * @return Result indicating success or failure
     */
    fun cancelPaymentIntent(paymentIntentId: String): PaymentResult<Unit>

    /**
     * Refund a captured payment.
     *
     * @param request Refund request with payment ID and amount
     * @return Result containing RefundResponse or error
     */
    fun refundPayment(request: RefundRequest): PaymentResult<RefundResponse>

    /**
     * Get payment details from the gateway.
     *
     * @param paymentIntentId Gateway-specific payment intent ID
     * @return Result containing PaymentResponse or error
     */
    fun getPaymentDetails(paymentIntentId: String): PaymentResult<PaymentResponse>

    /**
     * Verify webhook signature to ensure request came from payment gateway.
     *
     * @param payload Raw webhook payload
     * @param signature Signature header from webhook
     * @param secret Webhook secret for verification
     * @return true if signature is valid
     */
    fun verifyWebhookSignature(payload: String, signature: String, secret: String): Boolean

    /**
     * Parse webhook payload into a standardized webhook event.
     *
     * @param payload Raw webhook payload from gateway
     * @return Parsed webhook event
     */
    fun parseWebhookEvent(payload: String): PaymentWebhookEvent
}

/**
 * Payment gateway types.
 */
enum class PaymentGatewayType {
    STRIPE,
    PAYPAL,
    SQUARE,
    BRAINTREE,
    TEST // For testing/development
}

/**
 * Request to create a payment intent.
 */
data class CreatePaymentIntentRequest(
    val amount: BigDecimal,
    val currency: String,
    val customerId: String? = null, // Gateway-specific customer ID
    val customerEmail: String,
    val description: String,
    val metadata: Map<String, String> = emptyMap(),
    val captureMethod: CaptureMethod = CaptureMethod.AUTOMATIC
)

/**
 * Payment capture method.
 */
enum class CaptureMethod {
    AUTOMATIC, // Charge immediately
    MANUAL     // Authorize first, capture later
}

/**
 * Payment intent response from gateway.
 */
data class PaymentIntentResponse(
    val paymentIntentId: String,        // Gateway-specific payment intent ID
    val clientSecret: String,            // Secret for frontend payment completion
    val status: PaymentIntentStatus,
    val amount: BigDecimal,
    val currency: String,
    val customerId: String?,
    val createdAt: Long                  // Unix timestamp
)

/**
 * Payment intent status.
 */
enum class PaymentIntentStatus {
    CREATED,
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CONFIRMATION,
    REQUIRES_ACTION,
    PROCESSING,
    REQUIRES_CAPTURE,
    SUCCEEDED,
    CANCELED,
    FAILED
}

/**
 * Payment response from gateway.
 */
data class PaymentResponse(
    val paymentId: String,               // Gateway-specific payment ID
    val paymentIntentId: String,
    val amount: BigDecimal,
    val currency: String,
    val status: PaymentStatus,
    val paymentMethod: String?,          // Card, bank transfer, etc.
    val last4: String?,                  // Last 4 digits of card
    val brand: String?,                  // Visa, Mastercard, etc.
    val receiptUrl: String?,
    val createdAt: Long,
    val capturedAt: Long?
)

/**
 * Payment status (our internal representation).
 */
enum class PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    FULLY_REFUNDED,
    FAILED,
    CANCELED
}

/**
 * Refund request.
 */
data class RefundRequest(
    val paymentIntentId: String,
    val amount: BigDecimal,              // Amount to refund (can be partial)
    val reason: RefundReason,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Refund reason codes.
 */
enum class RefundReason {
    REQUESTED_BY_CUSTOMER,
    DUPLICATE,
    FRAUDULENT,
    BOOKING_CANCELLED,
    FACILITY_CLOSED,
    OTHER
}

/**
 * Refund response from gateway.
 */
data class RefundResponse(
    val refundId: String,                // Gateway-specific refund ID
    val paymentIntentId: String,
    val amount: BigDecimal,
    val currency: String,
    val status: RefundStatus,
    val reason: RefundReason?,
    val createdAt: Long
)

/**
 * Refund status.
 */
enum class RefundStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELED
}

/**
 * Webhook event from payment gateway.
 */
data class PaymentWebhookEvent(
    val eventId: String,
    val eventType: PaymentWebhookEventType,
    val paymentIntentId: String,
    val paymentId: String?,
    val amount: BigDecimal?,
    val currency: String?,
    val status: String,
    val createdAt: Long,
    val rawPayload: String
)

/**
 * Webhook event types.
 */
enum class PaymentWebhookEventType {
    PAYMENT_INTENT_CREATED,
    PAYMENT_INTENT_SUCCEEDED,
    PAYMENT_INTENT_FAILED,
    PAYMENT_INTENT_CANCELED,
    PAYMENT_CAPTURED,
    REFUND_CREATED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,
    UNKNOWN
}

/**
 * Result wrapper for payment operations.
 *
 * Using Result pattern instead of exceptions for better error handling.
 */
sealed class PaymentResult<out T> {
    data class Success<T>(val data: T) : PaymentResult<T>()
    data class Failure(val error: PaymentError) : PaymentResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw PaymentException(error.message, error)
    }
}

/**
 * Payment error information.
 */
data class PaymentError(
    val code: String,
    val message: String,
    val type: PaymentErrorType,
    val declineCode: String? = null
)

/**
 * Payment error types.
 */
enum class PaymentErrorType {
    API_ERROR,              // Gateway API error
    AUTHENTICATION_ERROR,   // Invalid API key
    CARD_ERROR,             // Card declined, insufficient funds, etc.
    IDEMPOTENCY_ERROR,      // Duplicate request
    INVALID_REQUEST_ERROR,  // Invalid parameters
    RATE_LIMIT_ERROR,       // Too many requests
    NETWORK_ERROR,          // Connection failure
    UNKNOWN_ERROR
}

/**
 * Payment exception (only thrown when explicitly requested via getOrThrow).
 */
class PaymentException(
    message: String,
    val error: PaymentError,
    cause: Throwable? = null
) : RuntimeException(message, cause)
