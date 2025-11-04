package com.liyaqa.backend.shared.notification.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Core notification entity tracking all communications across all channels.
 *
 * Design Philosophy:
 * Notifications are the bridge between transactions and relationships.
 * Every notification is tracked for audit, analytics, and customer service.
 * This entity serves as:
 * - Audit trail for all customer communications
 * - Delivery status tracking for operational monitoring
 * - Analytics foundation for engagement metrics
 * - Customer service reference for support tickets
 *
 * Multi-tenancy: Notifications are scoped by tenant for data isolation
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notification_recipient", columnList = "recipient_type,recipient_id"),
        Index(name = "idx_notification_tenant", columnList = "tenant_id,created_at"),
        Index(name = "idx_notification_status", columnList = "status,created_at"),
        Index(name = "idx_notification_channel", columnList = "channel,status"),
        Index(name = "idx_notification_type", columnList = "type,created_at"),
        Index(name = "idx_notification_scheduled", columnList = "scheduled_at,status")
    ]
)
class Notification(

    // === Recipient ===

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 50)
    var recipientType: RecipientType,

    @Column(name = "recipient_id", nullable = false)
    var recipientId: UUID,

    @Column(name = "recipient_email", length = 255)
    var recipientEmail: String? = null,

    @Column(name = "recipient_phone", length = 50)
    var recipientPhone: String? = null,

    @Column(name = "recipient_name", length = 255)
    var recipientName: String? = null,

    // === Notification Details ===

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    var type: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    var channel: NotificationChannel,

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    var priority: NotificationPriority = NotificationPriority.MEDIUM,

    @Column(name = "subject", length = 500)
    var subject: String? = null,

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    var message: String,

    @Column(name = "html_content", columnDefinition = "TEXT")
    var htmlContent: String? = null,

    // === Template Info ===

    @Column(name = "template_id", length = 100)
    var templateId: String? = null,

    @Column(name = "template_variables", columnDefinition = "TEXT")
    var templateVariables: String? = null, // JSON string of variables

    // === Delivery Status ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: NotificationStatus = NotificationStatus.PENDING,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "retry_count")
    var retryCount: Int = 0,

    @Column(name = "max_retries")
    var maxRetries: Int = 3,

    // === Scheduling ===

    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    // === Context & Metadata ===

    @Column(name = "context_type", length = 100)
    var contextType: String? = null, // e.g., "booking", "payment", "membership"

    @Column(name = "context_id")
    var contextId: UUID? = null, // Reference to related entity

    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String? = null, // JSON string for additional data

    // === External Provider ===

    @Column(name = "provider", length = 100)
    var provider: String? = null, // e.g., "sendgrid", "twilio", "firebase"

    @Column(name = "provider_message_id", length = 255)
    var providerMessageId: String? = null,

    @Column(name = "provider_response", columnDefinition = "TEXT")
    var providerResponse: String? = null,

    // === User Interaction ===

    @Column(name = "click_tracked")
    var clickTracked: Boolean = false,

    @Column(name = "clicked_at")
    var clickedAt: Instant? = null,

    @Column(name = "unsubscribed")
    var unsubscribed: Boolean = false,

    @Column(name = "unsubscribed_at")
    var unsubscribedAt: Instant? = null

) : BaseEntity() {

    // === Status Management ===

    fun markAsSent(providerMessageId: String? = null) {
        this.status = NotificationStatus.SENT
        this.sentAt = Instant.now()
        this.providerMessageId = providerMessageId
    }

    fun markAsDelivered() {
        this.status = NotificationStatus.DELIVERED
        this.deliveredAt = Instant.now()
    }

    fun markAsRead() {
        this.readAt = Instant.now()
    }

    fun markAsFailed(errorMessage: String) {
        this.status = NotificationStatus.FAILED
        this.failedAt = Instant.now()
        this.errorMessage = errorMessage
    }

    fun markAsClicked() {
        this.clickTracked = true
        this.clickedAt = Instant.now()
    }

    fun incrementRetry() {
        this.retryCount++
    }

    fun canRetry(): Boolean {
        return retryCount < maxRetries &&
               status == NotificationStatus.FAILED &&
               (expiresAt == null || expiresAt!!.isAfter(Instant.now()))
    }

    fun isExpired(): Boolean {
        return expiresAt != null && expiresAt!!.isBefore(Instant.now())
    }

    fun shouldSend(): Boolean {
        return status == NotificationStatus.PENDING &&
               !isExpired() &&
               (scheduledAt == null || scheduledAt!!.isBefore(Instant.now()))
    }
}

/**
 * Recipient type for notification routing and permissions.
 */
enum class RecipientType {
    MEMBER,        // Customer/member
    EMPLOYEE,      // Internal staff
    FACILITY,      // Facility-wide broadcast
    TENANT,        // Tenant-wide broadcast
    SYSTEM         // System administrators
}

/**
 * Notification channel for multi-channel delivery.
 */
enum class NotificationChannel {
    EMAIL,
    SMS,
    PUSH,          // Mobile push notifications
    IN_APP,        // In-app notifications
    WEBHOOK        // External webhook delivery
}

/**
 * Notification priority for routing and SLA.
 */
enum class NotificationPriority {
    CRITICAL,      // Security alerts, payment failures - immediate delivery
    HIGH,          // Booking confirmations, important updates
    MEDIUM,        // General communications
    LOW            // Marketing, tips, non-urgent
}

/**
 * Notification delivery status.
 */
enum class NotificationStatus {
    PENDING,       // Queued for delivery
    SENT,          // Sent to provider
    DELIVERED,     // Confirmed delivered by provider
    FAILED,        // Delivery failed
    BOUNCED,       // Permanently failed (invalid address)
    CANCELLED      // Cancelled before sending
}

/**
 * Notification type for categorization and analytics.
 */
enum class NotificationType {
    // Authentication
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED,
    LOGIN_NOTIFICATION,

    // Booking
    BOOKING_CONFIRMATION,
    BOOKING_REMINDER,
    BOOKING_CANCELLATION,
    BOOKING_MODIFIED,

    // Payment
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    REFUND_PROCESSED,
    PAYMENT_METHOD_EXPIRING,

    // Membership
    MEMBERSHIP_ACTIVATED,
    MEMBERSHIP_RENEWED,
    MEMBERSHIP_EXPIRING,
    MEMBERSHIP_EXPIRED,
    MEMBERSHIP_UPGRADED,

    // Operational
    FACILITY_ANNOUNCEMENT,
    SCHEDULE_CHANGE,
    MAINTENANCE_NOTICE,
    WEATHER_ALERT,

    // Marketing
    PROMOTIONAL_OFFER,
    NEWSLETTER,
    EVENT_INVITATION,
    SURVEY_REQUEST,

    // Support
    SUPPORT_TICKET_CREATED,
    SUPPORT_TICKET_UPDATED,
    SUPPORT_TICKET_RESOLVED,

    // System
    SYSTEM_ALERT,
    SECURITY_ALERT,

    // Custom
    CUSTOM
}
