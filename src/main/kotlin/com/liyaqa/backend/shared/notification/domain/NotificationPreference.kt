package com.liyaqa.backend.shared.notification.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import java.util.*

/**
 * User notification preferences for granular control.
 *
 * Design Philosophy:
 * - Respect user choice and privacy
 * - Granular control by channel and type
 * - Support for opt-out while maintaining critical notifications
 * - Compliance with communication regulations (CAN-SPAM, GDPR, etc.)
 *
 * Critical Notifications:
 * Some notifications (security, transactional) may override preferences
 * for legal and security reasons.
 */
@Entity
@Table(
    name = "notification_preferences",
    indexes = [
        Index(name = "idx_notif_pref_user", columnList = "user_type,user_id", unique = true),
        Index(name = "idx_notif_pref_tenant", columnList = "tenant_id")
    ]
)
class NotificationPreference(

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 50)
    var userType: RecipientType,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    // === Global Channel Preferences ===

    @Column(name = "email_enabled")
    var emailEnabled: Boolean = true,

    @Column(name = "sms_enabled")
    var smsEnabled: Boolean = false, // Opt-in by default for SMS

    @Column(name = "push_enabled")
    var pushEnabled: Boolean = true,

    @Column(name = "in_app_enabled")
    var inAppEnabled: Boolean = true,

    // === Category Preferences ===

    @Column(name = "transactional_enabled")
    var transactionalEnabled: Boolean = true, // Cannot be fully disabled

    @Column(name = "operational_enabled")
    var operationalEnabled: Boolean = true,

    @Column(name = "marketing_enabled")
    var marketingEnabled: Boolean = false, // Opt-in for marketing

    @Column(name = "reminder_enabled")
    var reminderEnabled: Boolean = true,

    // === Notification Type Overrides ===
    // JSON string mapping notification type to boolean
    @Column(name = "type_overrides", columnDefinition = "TEXT")
    var typeOverrides: String? = null,

    // === Quiet Hours ===

    @Column(name = "quiet_hours_enabled")
    var quietHoursEnabled: Boolean = false,

    @Column(name = "quiet_hours_start")
    var quietHoursStart: Int? = null, // 0-23 hours

    @Column(name = "quiet_hours_end")
    var quietHoursEnd: Int? = null, // 0-23 hours

    @Column(name = "quiet_hours_timezone", length = 50)
    var quietHoursTimezone: String? = null,

    // === Frequency Controls ===

    @Column(name = "digest_mode_enabled")
    var digestModeEnabled: Boolean = false, // Bundle notifications

    @Column(name = "digest_frequency", length = 50)
    var digestFrequency: String? = null, // DAILY, WEEKLY

    // === Language Preference ===

    @Column(name = "preferred_language", length = 10)
    var preferredLanguage: String = "en",

    // === Contact Info ===

    @Column(name = "email", length = 255)
    var email: String? = null,

    @Column(name = "phone_number", length = 50)
    var phoneNumber: String? = null,

    @Column(name = "device_tokens", columnDefinition = "TEXT")
    var deviceTokens: String? = null // JSON array of push notification tokens

) : BaseEntity() {

    /**
     * Check if a notification should be sent based on preferences.
     */
    fun shouldReceive(
        channel: NotificationChannel,
        type: NotificationType,
        priority: NotificationPriority
    ): Boolean {
        // Critical and high priority notifications always go through
        if (priority == NotificationPriority.CRITICAL || priority == NotificationPriority.HIGH) {
            return isChannelEnabled(channel)
        }

        // Check channel enabled
        if (!isChannelEnabled(channel)) {
            return false
        }

        // Check category
        if (!isCategoryEnabled(type)) {
            return false
        }

        // Check specific type override
        // TODO: Parse typeOverrides JSON if needed

        return true
    }

    private fun isChannelEnabled(channel: NotificationChannel): Boolean {
        return when (channel) {
            NotificationChannel.EMAIL -> emailEnabled
            NotificationChannel.SMS -> smsEnabled
            NotificationChannel.PUSH -> pushEnabled
            NotificationChannel.IN_APP -> inAppEnabled
            NotificationChannel.WEBHOOK -> true // System controlled
        }
    }

    private fun isCategoryEnabled(type: NotificationType): Boolean {
        return when (type) {
            // Transactional - always enabled for security/legal
            NotificationType.EMAIL_VERIFICATION,
            NotificationType.PASSWORD_RESET,
            NotificationType.PASSWORD_CHANGED,
            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.REFUND_PROCESSED -> true

            // Booking reminders
            NotificationType.BOOKING_REMINDER -> reminderEnabled

            // Marketing
            NotificationType.PROMOTIONAL_OFFER,
            NotificationType.NEWSLETTER,
            NotificationType.EVENT_INVITATION,
            NotificationType.SURVEY_REQUEST -> marketingEnabled

            // Operational
            NotificationType.FACILITY_ANNOUNCEMENT,
            NotificationType.SCHEDULE_CHANGE,
            NotificationType.MAINTENANCE_NOTICE,
            NotificationType.WEATHER_ALERT -> operationalEnabled

            // Default to operational category
            else -> operationalEnabled
        }
    }

    fun isInQuietHours(currentHour: Int): Boolean {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false
        }

        val start = quietHoursStart!!
        val end = quietHoursEnd!!

        return if (start < end) {
            currentHour in start until end
        } else {
            // Quiet hours span midnight
            currentHour >= start || currentHour < end
        }
    }
}
