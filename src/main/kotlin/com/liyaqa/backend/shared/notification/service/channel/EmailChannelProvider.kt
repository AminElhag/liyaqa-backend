package com.liyaqa.backend.shared.notification.service.channel

import com.liyaqa.backend.internal.shared.config.EmailService
import com.liyaqa.backend.shared.notification.domain.Notification
import com.liyaqa.backend.shared.notification.domain.NotificationChannel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

/**
 * Email channel provider using existing EmailService.
 *
 * Integration Strategy:
 * - Leverages existing SMTP configuration and EmailService
 * - Adds notification tracking and metrics
 * - Provides consistent interface with other channels
 * - Supports HTML and plain text content
 */
@Component
class EmailChannelProvider(
    private val emailService: EmailService
) : NotificationChannelProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getChannel(): NotificationChannel = NotificationChannel.EMAIL

    override suspend fun send(notification: Notification): ChannelDeliveryResult {
        try {
            // Validate before sending
            validate(notification)?.let {
                return ChannelDeliveryResult.permanentFailure(it)
            }

            val to = notification.recipientEmail
                ?: return ChannelDeliveryResult.permanentFailure("No email address provided")

            val subject = notification.subject
                ?: return ChannelDeliveryResult.permanentFailure("No subject provided")

            val htmlContent = notification.htmlContent ?: notification.message
            val plainText = notification.message

            // Send email using existing service
            emailService.sendEmail(
                to = to,
                subject = subject,
                htmlContent = htmlContent,
                plainText = plainText
            )

            // Generate pseudo message ID for tracking
            val messageId = "email-${UUID.randomUUID()}"

            logger.info(
                "Email notification sent successfully: id={}, recipient={}, type={}",
                notification.id,
                to,
                notification.type
            )

            return ChannelDeliveryResult.success(
                providerMessageId = messageId,
                providerResponse = "Sent via EmailService"
            )

        } catch (ex: Exception) {
            logger.error(
                "Failed to send email notification: id={}, error={}",
                notification.id,
                ex.message,
                ex
            )

            return ChannelDeliveryResult.failure(
                errorMessage = "Email delivery failed: ${ex.message}",
                shouldRetry = true
            )
        }
    }

    override fun isAvailable(): Boolean {
        // Email service is always available (though might be disabled in config)
        return true
    }

    override fun validate(notification: Notification): String? {
        if (notification.recipientEmail.isNullOrBlank()) {
            return "Email address is required"
        }

        if (!isValidEmail(notification.recipientEmail!!)) {
            return "Invalid email address format"
        }

        if (notification.subject.isNullOrBlank()) {
            return "Subject is required for email notifications"
        }

        if (notification.message.isBlank()) {
            return "Message body is required"
        }

        return null
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}
