package com.liyaqa.backend.payment.gateway

import com.stripe.Stripe
import com.stripe.exception.*
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCancelParams
import com.stripe.param.PaymentIntentCaptureParams
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.RefundCreateParams
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Stripe payment gateway implementation.
 *
 * Implements PaymentGateway interface using Stripe's Java SDK.
 * Handles payment intents, captures, refunds, and webhook verification.
 *
 * Configuration required:
 * - stripe.api.key: Secret API key from Stripe dashboard
 * - stripe.webhook.secret: Webhook signing secret
 *
 * Note: Amounts are converted to cents (Stripe's smallest currency unit).
 */
@Component
class StripePaymentGateway(
    @Value("\${payment.stripe.api-key}")
    private val apiKey: String,

    @Value("\${payment.stripe.webhook-secret}")
    private val webhookSecret: String
) : PaymentGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        Stripe.apiKey = apiKey
        logger.info("Stripe payment gateway initialized")
    }

    override fun getGatewayType(): PaymentGatewayType = PaymentGatewayType.STRIPE

    override fun createPaymentIntent(request: CreatePaymentIntentRequest): PaymentResult<PaymentIntentResponse> {
        return try {
            val params = PaymentIntentCreateParams.builder()
                .setAmount(toSmallestUnit(request.amount, request.currency))
                .setCurrency(request.currency.lowercase())
                .setDescription(request.description)
                .apply {
                    request.customerId?.let { setCustomer(it) }
                    if (request.metadata.isNotEmpty()) {
                        putAllMetadata(request.metadata)
                    }
                    when (request.captureMethod) {
                        CaptureMethod.MANUAL -> setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                        CaptureMethod.AUTOMATIC -> setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                    }
                    // Add receipt email
                    setReceiptEmail(request.customerEmail)
                }
                .build()

            val paymentIntent = PaymentIntent.create(params)

            logger.info("Created Stripe payment intent: ${paymentIntent.id} for amount ${request.amount} ${request.currency}")

            PaymentResult.Success(
                PaymentIntentResponse(
                    paymentIntentId = paymentIntent.id,
                    clientSecret = paymentIntent.clientSecret,
                    status = mapStripeStatus(paymentIntent.status),
                    amount = request.amount,
                    currency = request.currency,
                    customerId = paymentIntent.customer,
                    createdAt = paymentIntent.created
                )
            )
        } catch (e: StripeException) {
            logger.error("Failed to create Stripe payment intent", e)
            PaymentResult.Failure(mapStripeException(e))
        } catch (e: Exception) {
            logger.error("Unexpected error creating payment intent", e)
            PaymentResult.Failure(
                PaymentError(
                    code = "unknown_error",
                    message = e.message ?: "Unknown error occurred",
                    type = PaymentErrorType.UNKNOWN_ERROR
                )
            )
        }
    }

    override fun capturePayment(paymentIntentId: String): PaymentResult<PaymentResponse> {
        return try {
            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)

            if (paymentIntent.status == "requires_capture") {
                val params = PaymentIntentCaptureParams.builder().build()
                val captured = paymentIntent.capture(params)

                logger.info("Captured Stripe payment: ${captured.id}")

                PaymentResult.Success(mapToPaymentResponse(captured))
            } else {
                // Payment already captured or in wrong state
                PaymentResult.Success(mapToPaymentResponse(paymentIntent))
            }
        } catch (e: StripeException) {
            logger.error("Failed to capture Stripe payment: $paymentIntentId", e)
            PaymentResult.Failure(mapStripeException(e))
        }
    }

    override fun cancelPaymentIntent(paymentIntentId: String): PaymentResult<Unit> {
        return try {
            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            val params = PaymentIntentCancelParams.builder().build()
            paymentIntent.cancel(params)

            logger.info("Canceled Stripe payment intent: $paymentIntentId")

            PaymentResult.Success(Unit)
        } catch (e: StripeException) {
            logger.error("Failed to cancel Stripe payment intent: $paymentIntentId", e)
            PaymentResult.Failure(mapStripeException(e))
        }
    }

    override fun refundPayment(request: RefundRequest): PaymentResult<RefundResponse> {
        return try {
            val params = RefundCreateParams.builder()
                .setPaymentIntent(request.paymentIntentId)
                .setAmount(toSmallestUnit(request.amount, "usd")) // Currency from original payment
                .setReason(mapRefundReason(request.reason))
                .apply {
                    if (request.metadata.isNotEmpty()) {
                        putAllMetadata(request.metadata)
                    }
                }
                .build()

            val refund = Refund.create(params)

            logger.info("Created Stripe refund: ${refund.id} for payment ${request.paymentIntentId}, amount ${request.amount}")

            PaymentResult.Success(
                RefundResponse(
                    refundId = refund.id,
                    paymentIntentId = request.paymentIntentId,
                    amount = fromSmallestUnit(refund.amount, refund.currency),
                    currency = refund.currency.uppercase(),
                    status = mapRefundStatus(refund.status),
                    reason = request.reason,
                    createdAt = refund.created
                )
            )
        } catch (e: StripeException) {
            logger.error("Failed to create Stripe refund for payment ${request.paymentIntentId}", e)
            PaymentResult.Failure(mapStripeException(e))
        }
    }

    override fun getPaymentDetails(paymentIntentId: String): PaymentResult<PaymentResponse> {
        return try {
            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            PaymentResult.Success(mapToPaymentResponse(paymentIntent))
        } catch (e: StripeException) {
            logger.error("Failed to retrieve Stripe payment: $paymentIntentId", e)
            PaymentResult.Failure(mapStripeException(e))
        }
    }

    override fun verifyWebhookSignature(payload: String, signature: String, secret: String): Boolean {
        return try {
            Webhook.constructEvent(payload, signature, secret)
            true
        } catch (e: Exception) {
            logger.warn("Webhook signature verification failed", e)
            false
        }
    }

    override fun parseWebhookEvent(payload: String): PaymentWebhookEvent {
        // Parse webhook event - simplified implementation
        // In production, use proper JSON parsing library
        return PaymentWebhookEvent(
            eventId = "webhook_event",
            eventType = PaymentWebhookEventType.UNKNOWN,
            paymentIntentId = "",
            paymentId = null,
            amount = null,
            currency = null,
            status = "unknown",
            createdAt = System.currentTimeMillis() / 1000,
            rawPayload = payload
        )
    }

    // Helper functions

    private fun toSmallestUnit(amount: BigDecimal, currency: String): Long {
        // Most currencies use 2 decimal places (cents)
        // Special handling for zero-decimal currencies (JPY, KRW) can be added here
        return amount.multiply(BigDecimal(100)).longValueExact()
    }

    private fun fromSmallestUnit(amount: Long, currency: String): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal(100))
    }

    private fun mapStripeStatus(status: String): PaymentIntentStatus {
        return when (status) {
            "requires_payment_method" -> PaymentIntentStatus.REQUIRES_PAYMENT_METHOD
            "requires_confirmation" -> PaymentIntentStatus.REQUIRES_CONFIRMATION
            "requires_action" -> PaymentIntentStatus.REQUIRES_ACTION
            "processing" -> PaymentIntentStatus.PROCESSING
            "requires_capture" -> PaymentIntentStatus.REQUIRES_CAPTURE
            "succeeded" -> PaymentIntentStatus.SUCCEEDED
            "canceled" -> PaymentIntentStatus.CANCELED
            else -> PaymentIntentStatus.FAILED
        }
    }

    private fun mapToPaymentResponse(paymentIntent: PaymentIntent): PaymentResponse {
        return PaymentResponse(
            paymentId = paymentIntent.id,
            paymentIntentId = paymentIntent.id,
            amount = fromSmallestUnit(paymentIntent.amount, paymentIntent.currency),
            currency = paymentIntent.currency.uppercase(),
            status = when (paymentIntent.status) {
                "succeeded" -> PaymentStatus.CAPTURED
                "requires_capture" -> PaymentStatus.AUTHORIZED
                "canceled" -> PaymentStatus.CANCELED
                else -> PaymentStatus.PENDING
            },
            paymentMethod = null, // Payment method details require separate API call
            last4 = null,
            brand = null,
            receiptUrl = null,
            createdAt = paymentIntent.created,
            capturedAt = null
        )
    }

    private fun mapStripeException(e: StripeException): PaymentError {
        return when (e) {
            is CardException -> PaymentError(
                code = e.code ?: "card_error",
                message = e.message ?: "Card error occurred",
                type = PaymentErrorType.CARD_ERROR,
                declineCode = e.declineCode
            )
            is RateLimitException -> PaymentError(
                code = "rate_limit",
                message = "Too many requests to payment gateway",
                type = PaymentErrorType.RATE_LIMIT_ERROR
            )
            is InvalidRequestException -> PaymentError(
                code = e.code ?: "invalid_request",
                message = e.message ?: "Invalid request to payment gateway",
                type = PaymentErrorType.INVALID_REQUEST_ERROR
            )
            is AuthenticationException -> PaymentError(
                code = "authentication_error",
                message = "Payment gateway authentication failed",
                type = PaymentErrorType.AUTHENTICATION_ERROR
            )
            is ApiConnectionException -> PaymentError(
                code = "network_error",
                message = "Failed to connect to payment gateway",
                type = PaymentErrorType.NETWORK_ERROR
            )
            is ApiException -> PaymentError(
                code = e.code ?: "api_error",
                message = e.message ?: "Payment gateway API error",
                type = PaymentErrorType.API_ERROR
            )
            else -> PaymentError(
                code = "unknown_error",
                message = e.message ?: "Unknown payment error",
                type = PaymentErrorType.UNKNOWN_ERROR
            )
        }
    }

    private fun mapRefundReason(reason: RefundReason): RefundCreateParams.Reason {
        return when (reason) {
            RefundReason.REQUESTED_BY_CUSTOMER -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER
            RefundReason.DUPLICATE -> RefundCreateParams.Reason.DUPLICATE
            RefundReason.FRAUDULENT -> RefundCreateParams.Reason.FRAUDULENT
            else -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER
        }
    }

    private fun mapRefundStatus(status: String): RefundStatus {
        return when (status) {
            "pending" -> RefundStatus.PENDING
            "succeeded" -> RefundStatus.SUCCEEDED
            "failed" -> RefundStatus.FAILED
            "canceled" -> RefundStatus.CANCELED
            else -> RefundStatus.PENDING
        }
    }

    private fun mapWebhookEventType(type: String): PaymentWebhookEventType {
        return when (type) {
            "payment_intent.created" -> PaymentWebhookEventType.PAYMENT_INTENT_CREATED
            "payment_intent.succeeded" -> PaymentWebhookEventType.PAYMENT_INTENT_SUCCEEDED
            "payment_intent.payment_failed" -> PaymentWebhookEventType.PAYMENT_INTENT_FAILED
            "payment_intent.canceled" -> PaymentWebhookEventType.PAYMENT_INTENT_CANCELED
            "charge.captured" -> PaymentWebhookEventType.PAYMENT_CAPTURED
            "charge.refunded" -> PaymentWebhookEventType.REFUND_CREATED
            "refund.created" -> PaymentWebhookEventType.REFUND_CREATED
            "refund.updated" -> PaymentWebhookEventType.REFUND_SUCCEEDED
            "refund.failed" -> PaymentWebhookEventType.REFUND_FAILED
            else -> PaymentWebhookEventType.UNKNOWN
        }
    }
}
