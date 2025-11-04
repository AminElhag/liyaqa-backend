package com.liyaqa.backend.payment.config

import com.liyaqa.backend.payment.gateway.CaptureMethod
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Payment configuration properties.
 *
 * Centralizes all payment-related configuration including:
 * - Stripe settings
 * - PayPal settings (when added)
 * - Default payment settings
 * - Feature flags
 *
 * Configuration is externalized via application.yaml and environment variables
 * to support different environments (dev, staging, production).
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
class PaymentConfig {

    val stripe = StripeConfig()
    val default = DefaultPaymentConfig()

    /**
     * Stripe-specific configuration.
     */
    class StripeConfig {
        /**
         * Stripe secret API key.
         * Required for server-side API calls.
         * Should be kept secure and never exposed to clients.
         */
        lateinit var apiKey: String

        /**
         * Stripe webhook signing secret.
         * Used to verify webhook signatures for security.
         */
        lateinit var webhookSecret: String

        /**
         * Stripe publishable key.
         * Safe to expose to clients for frontend integration.
         * Used by Stripe.js and mobile SDKs.
         */
        lateinit var publishableKey: String
    }

    /**
     * Default payment settings.
     */
    class DefaultPaymentConfig {
        /**
         * Default currency for payments.
         * ISO 4217 currency code (USD, EUR, GBP, etc.)
         */
        var currency: String = "USD"

        /**
         * Default capture method for payments.
         * AUTOMATIC - Capture immediately after authorization
         * MANUAL - Requires explicit capture call
         */
        var captureMethod: CaptureMethod = CaptureMethod.AUTOMATIC
    }
}

/**
 * Payment feature flags and settings.
 *
 * Allows enabling/disabling payment features per environment
 * or for gradual rollout.
 */
@Configuration
@ConfigurationProperties(prefix = "payment.features")
class PaymentFeatureConfig {

    /**
     * Enable/disable payment processing globally.
     * Useful for maintenance or testing environments.
     */
    var enabled: Boolean = true

    /**
     * Enable/disable refund processing.
     */
    var refundsEnabled: Boolean = true

    /**
     * Enable/disable partial refunds.
     * If false, only full refunds are allowed.
     */
    var partialRefundsEnabled: Boolean = true

    /**
     * Enable/disable webhook processing.
     */
    var webhooksEnabled: Boolean = true

    /**
     * Require 3D Secure authentication for payments.
     */
    var require3DSecure: Boolean = false

    /**
     * Enable automatic retry for failed payments.
     */
    var autoRetryEnabled: Boolean = false
}
