package com.liyaqa.backend.shared.notification.service.channel

import com.liyaqa.backend.shared.notification.domain.Notification
import com.liyaqa.backend.shared.notification.domain.NotificationChannel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

/**
 * SMS channel provider.
 *
 * Integration Strategy:
 * - Ready for Twilio, AWS SNS, or other SMS providers
 * - Configurable provider selection
 * - Rate limiting awareness
 * - Cost tracking (SMS has per-message cost)
 * - Phone number validation
 * - International format support
 *
 * TODO: Integrate with actual SMS provider (Twilio recommended)
 * For now, logs SMS that would be sent for development/testing.
 */
@Component
class SmsChannelProvider(
    @Value("\${liyaqa.notification.sms.enabled:false}")
    private val smsEnabled: Boolean,

    @Value("\${liyaqa.notification.sms.provider:twilio}")
    private val provider: String
) : NotificationChannelProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getChannel(): NotificationChannel = NotificationChannel.SMS

    override suspend fun send(notification: Notification): ChannelDeliveryResult {
        if (!isAvailable()) {
            return ChannelDeliveryResult.permanentFailure("SMS channel is not enabled")
        }

        try {
            // Validate before sending
            validate(notification)?.let {
                return ChannelDeliveryResult.permanentFailure(it)
            }

            val phoneNumber = notification.recipientPhone
                ?: return ChannelDeliveryResult.permanentFailure("No phone number provided")

            val message = notification.message

            // TODO: Integrate with actual SMS provider
            // For now, log what would be sent
            logger.info(
                "SMS notification (mock): id={}, to={}, message={}",
                notification.id,
                phoneNumber,
                message.take(50)
            )

            // Simulate provider message ID
            val messageId = "sms-${UUID.randomUUID()}"

            return ChannelDeliveryResult.success(
                providerMessageId = messageId,
                providerResponse = "Mock SMS sent (integration pending)"
            )

        } catch (ex: Exception) {
            logger.error(
                "Failed to send SMS notification: id={}, error={}",
                notification.id,
                ex.message,
                ex
            )

            return ChannelDeliveryResult.failure(
                errorMessage = "SMS delivery failed: ${ex.message}",
                shouldRetry = true
            )
        }
    }

    override fun isAvailable(): Boolean = smsEnabled

    override fun validate(notification: Notification): String? {
        if (notification.recipientPhone.isNullOrBlank()) {
            return "Phone number is required"
        }

        if (!isValidPhoneNumber(notification.recipientPhone!!)) {
            return "Invalid phone number format"
        }

        if (notification.message.isBlank()) {
            return "Message is required"
        }

        // SMS length limits (160 chars for single SMS, 153 for concatenated)
        if (notification.message.length > 1600) {
            return "Message exceeds SMS length limit (1600 characters)"
        }

        return null
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic validation - should start with + and contain only digits
        // In production, use a library like libphonenumber for proper validation
        val phoneRegex = "^\\+?[1-9]\\d{1,14}$".toRegex()
        return phone.replace(Regex("[\\s()-]"), "").matches(phoneRegex)
    }
}

/**
 * SMS Provider Integration Notes:
 *
 * Twilio Integration Example:
 * ```kotlin
 * val twilio = Twilio.init(accountSid, authToken)
 * val message = Message.creator(
 *     PhoneNumber(to),
 *     PhoneNumber(from),
 *     messageBody
 * ).create()
 * return message.sid
 * ```
 *
 * AWS SNS Integration Example:
 * ```kotlin
 * val snsClient = SnsClient.builder().build()
 * val request = PublishRequest.builder()
 *     .phoneNumber(to)
 *     .message(messageBody)
 *     .build()
 * val result = snsClient.publish(request)
 * return result.messageId()
 * ```
 */
