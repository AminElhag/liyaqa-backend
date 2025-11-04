package com.liyaqa.backend.shared.notification.service.channel

import com.liyaqa.backend.shared.notification.domain.Notification
import com.liyaqa.backend.shared.notification.domain.NotificationChannel

/**
 * Interface for notification channel providers.
 *
 * Design Pattern: Strategy Pattern
 * Each channel (email, SMS, push, etc.) implements this interface
 * to provide consistent notification delivery abstraction.
 *
 * Implementations should:
 * - Handle channel-specific delivery logic
 * - Integrate with third-party providers (SendGrid, Twilio, Firebase, etc.)
 * - Provide delivery confirmation
 * - Handle retries and failures gracefully
 * - Log delivery metrics
 */
interface NotificationChannelProvider {

    /**
     * The channel this provider handles.
     */
    fun getChannel(): NotificationChannel

    /**
     * Send notification through this channel.
     *
     * @param notification The notification to send
     * @return Delivery result with success status and provider-specific data
     */
    suspend fun send(notification: Notification): ChannelDeliveryResult

    /**
     * Check if this provider is available and configured.
     */
    fun isAvailable(): Boolean

    /**
     * Validate notification before sending.
     * Returns null if valid, error message if invalid.
     */
    fun validate(notification: Notification): String?
}

/**
 * Result of channel delivery attempt.
 */
data class ChannelDeliveryResult(
    val success: Boolean,
    val providerMessageId: String? = null,
    val errorMessage: String? = null,
    val providerResponse: String? = null,
    val shouldRetry: Boolean = false
) {
    companion object {
        fun success(providerMessageId: String?, providerResponse: String? = null) =
            ChannelDeliveryResult(
                success = true,
                providerMessageId = providerMessageId,
                providerResponse = providerResponse
            )

        fun failure(errorMessage: String, shouldRetry: Boolean = true) =
            ChannelDeliveryResult(
                success = false,
                errorMessage = errorMessage,
                shouldRetry = shouldRetry
            )

        fun permanentFailure(errorMessage: String) =
            ChannelDeliveryResult(
                success = false,
                errorMessage = errorMessage,
                shouldRetry = false
            )
    }
}
