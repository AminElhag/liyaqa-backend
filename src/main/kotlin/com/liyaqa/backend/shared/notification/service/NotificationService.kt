package com.liyaqa.backend.shared.notification.service

import com.liyaqa.backend.shared.notification.data.NotificationPreferenceRepository
import com.liyaqa.backend.shared.notification.data.NotificationRepository
import com.liyaqa.backend.shared.notification.data.NotificationTemplateRepository
import com.liyaqa.backend.shared.notification.domain.*
import com.liyaqa.backend.shared.notification.service.channel.NotificationChannelProvider
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.util.*

/**
 * Core notification orchestration service.
 *
 * Design Philosophy:
 * "Notifications turn transactions into relationships."
 *
 * This service is the heart of customer engagement, responsible for:
 * - Preference-aware notification routing
 * - Multi-channel delivery coordination
 * - Template rendering and personalization
 * - Delivery tracking and analytics
 * - Retry logic for failed notifications
 * - Scheduling and queueing
 *
 * Architecture:
 * - Channel-agnostic API for business logic
 * - Strategy pattern for channel providers
 * - Async processing for non-blocking operations
 * - Comprehensive audit trail
 * - Graceful degradation on failures
 */
@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val channelProviders: List<NotificationChannelProvider>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // === Core API ===

    /**
     * Send a notification using a template.
     *
     * This is the primary method for sending notifications.
     * It handles template rendering, preference checking, and delivery.
     */
    @Async
    fun sendFromTemplate(
        tenantId: String,
        recipientType: RecipientType,
        recipientId: UUID,
        templateCode: String,
        variables: Map<String, Any>,
        channel: NotificationChannel? = null,
        priority: NotificationPriority = NotificationPriority.MEDIUM,
        contextType: String? = null,
        contextId: UUID? = null,
        scheduledAt: Instant? = null
    ): UUID? {
        try {
            // Find template
            val template = findTemplate(templateCode, tenantId)
                ?: run {
                    logger.error("Template not found: code={}, tenant={}", templateCode, tenantId)
                    return null
                }

            // Determine channel
            val targetChannel = channel ?: template.channel

            // Get recipient preferences
            val preferences = getOrCreatePreferences(tenantId, recipientType, recipientId)

            // Check if should send based on preferences
            if (!preferences.shouldReceive(targetChannel, template.notificationType, priority)) {
                logger.info(
                    "Notification blocked by user preferences: recipient={}, channel={}, type={}",
                    recipientId,
                    targetChannel,
                    template.notificationType
                )
                return null
            }

            // Render template
            val subject = template.renderSubject(variables)
            val message = template.renderBody(variables)
            val htmlContent = template.renderHtml(variables)

            // Create notification
            val notification = Notification(
                recipientType = recipientType,
                recipientId = recipientId,
                recipientEmail = preferences.email,
                recipientPhone = preferences.phoneNumber,
                recipientName = variables["recipientName"]?.toString(),
                type = template.notificationType,
                channel = targetChannel,
                priority = priority,
                subject = subject,
                message = message,
                htmlContent = htmlContent,
                templateId = template.templateCode,
                templateVariables = variables.toString(),
                scheduledAt = scheduledAt,
                contextType = contextType,
                contextId = contextId,
                maxRetries = template.maxRetries
            )
            notification.tenantId = tenantId

            val saved = notificationRepository.save(notification)

            // If not scheduled, send immediately
            if (scheduledAt == null || scheduledAt.isBefore(Instant.now())) {
                GlobalScope.launch {
                    sendNotification(saved.id!!)
                }
            }

            logger.info(
                "Notification created: id={}, recipient={}, type={}, channel={}",
                saved.id,
                recipientId,
                template.notificationType,
                targetChannel
            )

            return saved.id

        } catch (ex: Exception) {
            logger.error("Failed to send notification from template: {}", ex.message, ex)
            return null
        }
    }

    /**
     * Send a custom notification without a template.
     */
    @Async
    fun send(request: SendNotificationRequest): UUID? {
        try {
            // Get recipient preferences
            val preferences = getOrCreatePreferences(
                request.tenantId,
                request.recipientType,
                request.recipientId
            )

            // Check preferences
            if (!preferences.shouldReceive(request.channel, request.type, request.priority)) {
                logger.info(
                    "Notification blocked by user preferences: recipient={}, channel={}, type={}",
                    request.recipientId,
                    request.channel,
                    request.type
                )
                return null
            }

            // Create notification
            val notification = Notification(
                recipientType = request.recipientType,
                recipientId = request.recipientId,
                recipientEmail = request.recipientEmail ?: preferences.email,
                recipientPhone = request.recipientPhone ?: preferences.phoneNumber,
                recipientName = request.recipientName,
                type = request.type,
                channel = request.channel,
                priority = request.priority,
                subject = request.subject,
                message = request.message,
                htmlContent = request.htmlContent,
                scheduledAt = request.scheduledAt,
                expiresAt = request.expiresAt,
                contextType = request.contextType,
                contextId = request.contextId,
                metadata = request.metadata
            )
            notification.tenantId = request.tenantId

            val saved = notificationRepository.save(notification)

            // If not scheduled, send immediately
            if (request.scheduledAt == null || request.scheduledAt.isBefore(Instant.now())) {
                GlobalScope.launch {
                    sendNotification(saved.id!!)
                }
            }

            return saved.id

        } catch (ex: Exception) {
            logger.error("Failed to send custom notification: {}", ex.message, ex)
            return null
        }
    }

    /**
     * Send notification immediately by ID.
     */
    suspend fun sendNotification(notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId).orElse(null)
            ?: run {
                logger.warn("Notification not found: {}", notificationId)
                return
            }

        // Check if should send
        if (!notification.shouldSend()) {
            logger.debug("Notification not ready to send: id={}, status={}", notificationId, notification.status)
            return
        }

        // Find channel provider
        val provider = channelProviders.firstOrNull { it.getChannel() == notification.channel }
            ?: run {
                logger.error("No provider found for channel: {}", notification.channel)
                notification.markAsFailed("No provider found for channel: ${notification.channel}")
                notificationRepository.save(notification)
                return
            }

        // Check provider availability
        if (!provider.isAvailable()) {
            logger.warn("Provider not available: channel={}", notification.channel)
            notification.markAsFailed("Provider not available: ${notification.channel}")
            notificationRepository.save(notification)
            return
        }

        // Send through channel
        try {
            val result = provider.send(notification)

            if (result.success) {
                notification.markAsSent(result.providerMessageId)
                notification.providerResponse = result.providerResponse
                notification.provider = provider.getChannel().name

                // For some channels, mark as delivered immediately
                if (notification.channel == NotificationChannel.IN_APP) {
                    notification.markAsDelivered()
                }

                logger.info(
                    "Notification sent successfully: id={}, channel={}, providerId={}",
                    notificationId,
                    notification.channel,
                    result.providerMessageId
                )
            } else {
                notification.markAsFailed(result.errorMessage ?: "Unknown error")
                notification.providerResponse = result.providerResponse

                if (result.shouldRetry) {
                    notification.incrementRetry()
                }

                logger.error(
                    "Notification delivery failed: id={}, channel={}, error={}, willRetry={}",
                    notificationId,
                    notification.channel,
                    result.errorMessage,
                    result.shouldRetry
                )
            }

            notificationRepository.save(notification)

        } catch (ex: Exception) {
            logger.error("Exception during notification delivery: id={}, error={}", notificationId, ex.message, ex)
            notification.markAsFailed("Delivery exception: ${ex.message}")
            notification.incrementRetry()
            notificationRepository.save(notification)
        }
    }

    // === Batch Operations ===

    /**
     * Send notification to multiple recipients (broadcast).
     */
    @Async
    suspend fun sendBatch(requests: List<SendNotificationRequest>): List<UUID> = coroutineScope {
        requests.map { request ->
            async {
                send(request)
            }
        }.awaitAll().filterNotNull()
    }

    // === Scheduled Processing ===

    /**
     * Process pending notifications every minute.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    suspend fun processPendingNotifications() {
        try {
            val pending = notificationRepository.findPendingNotifications(
                status = NotificationStatus.PENDING,
                now = Instant.now(),
                pageable = org.springframework.data.domain.PageRequest.of(0, 100)
            )

            logger.debug("Processing {} pending notifications", pending.content.size)

            for (notification in pending.content) {
                sendNotification(notification.id!!)
            }

        } catch (ex: Exception) {
            logger.error("Error processing pending notifications: {}", ex.message, ex)
        }
    }

    /**
     * Retry failed notifications every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    suspend fun retryFailedNotifications() {
        try {
            val failed = notificationRepository.findRetryableNotifications(
                now = Instant.now(),
                pageable = org.springframework.data.domain.PageRequest.of(0, 50)
            )

            logger.debug("Retrying {} failed notifications", failed.content.size)

            for (notification in failed.content) {
                if (notification.canRetry()) {
                    sendNotification(notification.id!!)
                }
            }

        } catch (ex: Exception) {
            logger.error("Error retrying failed notifications: {}", ex.message, ex)
        }
    }

    // === User Interactions ===

    /**
     * Mark notification as read.
     */
    fun markAsRead(notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId).orElse(null)
            ?: return

        notification.markAsRead()
        notificationRepository.save(notification)
    }

    /**
     * Mark notification as clicked.
     */
    fun markAsClicked(notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId).orElse(null)
            ?: return

        notification.markAsClicked()
        notificationRepository.save(notification)
    }

    // === Helper Methods ===

    private fun findTemplate(templateCode: String, tenantId: String): NotificationTemplate? {
        val templates = templateRepository.findByTemplateCodeWithFallback(templateCode, tenantId)
        return templates.firstOrNull()
    }

    private fun getOrCreatePreferences(
        tenantId: String,
        recipientType: RecipientType,
        recipientId: UUID
    ): NotificationPreference {
        return preferenceRepository.findByTenantIdAndUserTypeAndUserId(tenantId, recipientType, recipientId)
            ?: NotificationPreference(
                userType = recipientType,
                userId = recipientId
            ).also {
                it.tenantId = tenantId
                preferenceRepository.save(it)
            }
    }
}

/**
 * Request DTO for sending custom notifications.
 */
data class SendNotificationRequest(
    val tenantId: String,
    val recipientType: RecipientType,
    val recipientId: UUID,
    val recipientEmail: String? = null,
    val recipientPhone: String? = null,
    val recipientName: String? = null,
    val type: NotificationType,
    val channel: NotificationChannel,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val subject: String? = null,
    val message: String,
    val htmlContent: String? = null,
    val scheduledAt: Instant? = null,
    val expiresAt: Instant? = null,
    val contextType: String? = null,
    val contextId: UUID? = null,
    val metadata: String? = null
)
