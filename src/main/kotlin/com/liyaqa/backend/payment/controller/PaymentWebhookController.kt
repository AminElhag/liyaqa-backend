package com.liyaqa.backend.payment.controller

import com.liyaqa.backend.payment.data.PaymentTransactionRepository
import com.liyaqa.backend.payment.domain.PaymentTransactionStatus
import com.liyaqa.backend.payment.gateway.PaymentGateway
import com.liyaqa.backend.payment.gateway.PaymentGatewayType
import com.liyaqa.backend.payment.gateway.PaymentWebhookEventType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Controller for handling payment gateway webhooks.
 *
 * Receives real-time notifications from payment gateways (Stripe, PayPal, etc.)
 * about payment events and updates transaction status accordingly.
 *
 * Security:
 * - Verifies webhook signatures to ensure authenticity
 * - Only processes events from configured gateways
 * - Logs all webhook events for audit trail
 *
 * Endpoints:
 * - POST /api/webhooks/payments/stripe - Stripe webhook endpoint
 * - POST /api/webhooks/payments/paypal - PayPal webhook endpoint
 * - Additional gateway endpoints can be added as needed
 */
@RestController
@RequestMapping("/api/webhooks/payments")
class PaymentWebhookController(
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val paymentGateways: List<PaymentGateway>,
    @Value("\${payment.stripe.webhook-secret}")
    private val stripeWebhookSecret: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Handle Stripe webhook events.
     *
     * Stripe sends webhook events for various payment lifecycle events.
     * We verify the signature and update our transaction records accordingly.
     */
    @PostMapping("/stripe")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String
    ): ResponseEntity<String> {
        logger.info("Received Stripe webhook")

        val gateway = getGateway(PaymentGatewayType.STRIPE)
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Stripe gateway not configured")

        // Verify webhook signature for security
        if (!gateway.verifyWebhookSignature(payload, signature, stripeWebhookSecret)) {
            logger.warn("Stripe webhook signature verification failed")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid signature")
        }

        // Parse the webhook event
        val event = try {
            gateway.parseWebhookEvent(payload)
        } catch (e: Exception) {
            logger.error("Failed to parse Stripe webhook event", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to parse webhook")
        }

        // Process the event
        processWebhookEvent(event, PaymentGatewayType.STRIPE)

        return ResponseEntity.ok("Webhook processed")
    }

    /**
     * Handle PayPal webhook events.
     *
     * Similar to Stripe, but using PayPal's signature verification.
     * To be implemented when PayPal gateway is added.
     */
    @PostMapping("/paypal")
    fun handlePayPalWebhook(
        @RequestBody payload: String,
        @RequestHeader("PAYPAL-TRANSMISSION-SIG") signature: String,
        @RequestHeader("PAYPAL-TRANSMISSION-ID") transmissionId: String,
        @RequestHeader("PAYPAL-TRANSMISSION-TIME") transmissionTime: String,
        @RequestHeader("PAYPAL-CERT-URL") certUrl: String
    ): ResponseEntity<String> {
        logger.info("Received PayPal webhook")

        // PayPal webhook handling to be implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("PayPal webhooks not yet implemented")
    }

    /**
     * Process a webhook event and update transaction status.
     */
    private fun processWebhookEvent(event: com.liyaqa.backend.payment.gateway.PaymentWebhookEvent, gatewayType: PaymentGatewayType) {
        logger.info("Processing webhook event: ${event.eventType} for payment ${event.paymentIntentId}")

        try {
            // Find the transaction by gateway payment ID
            val transaction = paymentTransactionRepository.findByGatewayAndGatewayPaymentId(
                gateway = gatewayType,
                gatewayPaymentId = event.paymentIntentId
            )

            if (transaction == null) {
                logger.warn("Transaction not found for gateway payment ID: ${event.paymentIntentId}")
                return
            }

            // Update transaction based on event type
            when (event.eventType) {
                PaymentWebhookEventType.PAYMENT_INTENT_CREATED -> {
                    // Payment intent created, transaction should already exist
                    logger.info("Payment intent created: ${event.paymentIntentId}")
                }

                PaymentWebhookEventType.PAYMENT_INTENT_SUCCEEDED,
                PaymentWebhookEventType.PAYMENT_CAPTURED -> {
                    // Payment succeeded - mark as completed
                    if (transaction.status != PaymentTransactionStatus.COMPLETED) {
                        transaction.status = PaymentTransactionStatus.COMPLETED
                        transaction.capturedAt = Instant.now()
                        transaction.statusMessage = "Payment completed via webhook"
                        paymentTransactionRepository.save(transaction)
                        logger.info("Payment completed via webhook: ${transaction.transactionNumber}")
                    }
                }

                PaymentWebhookEventType.PAYMENT_INTENT_FAILED -> {
                    // Payment failed
                    if (transaction.status != PaymentTransactionStatus.FAILED) {
                        transaction.status = PaymentTransactionStatus.FAILED
                        transaction.failedAt = Instant.now()
                        transaction.statusMessage = "Payment failed via webhook"
                        transaction.errorMessage = event.status
                        paymentTransactionRepository.save(transaction)
                        logger.info("Payment failed via webhook: ${transaction.transactionNumber}")
                    }
                }

                PaymentWebhookEventType.PAYMENT_INTENT_CANCELED -> {
                    // Payment canceled
                    if (transaction.status != PaymentTransactionStatus.CANCELED) {
                        transaction.status = PaymentTransactionStatus.CANCELED
                        transaction.statusMessage = "Payment canceled via webhook"
                        paymentTransactionRepository.save(transaction)
                        logger.info("Payment canceled via webhook: ${transaction.transactionNumber}")
                    }
                }

                PaymentWebhookEventType.REFUND_CREATED,
                PaymentWebhookEventType.REFUND_SUCCEEDED -> {
                    // Refund succeeded
                    // The refund transaction should already be created by our service
                    // This webhook confirms the refund
                    logger.info("Refund succeeded via webhook for payment: ${transaction.transactionNumber}")
                }

                PaymentWebhookEventType.REFUND_FAILED -> {
                    // Refund failed
                    logger.warn("Refund failed via webhook for payment: ${transaction.transactionNumber}")
                }

                PaymentWebhookEventType.UNKNOWN -> {
                    logger.warn("Unknown webhook event type received: ${event.status}")
                }
            }

        } catch (e: Exception) {
            logger.error("Error processing webhook event: ${event.eventType}", e)
            // Don't throw exception - we want to return 200 to gateway to prevent retries
        }
    }

    /**
     * Get gateway by type.
     */
    private fun getGateway(type: PaymentGatewayType): PaymentGateway? {
        return paymentGateways.find { it.getGatewayType() == type }
    }
}
