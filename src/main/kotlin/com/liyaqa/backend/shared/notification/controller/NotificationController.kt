package com.liyaqa.backend.shared.notification.controller

import com.liyaqa.backend.shared.notification.data.NotificationRepository
import com.liyaqa.backend.shared.notification.data.NotificationPreferenceRepository
import com.liyaqa.backend.shared.notification.domain.*
import com.liyaqa.backend.shared.notification.service.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * API endpoints for notification management.
 *
 * Member Endpoints:
 * - GET /api/member/notifications - List my notifications
 * - PUT /api/member/notifications/{id}/read - Mark as read
 * - GET /api/member/notifications/preferences - Get preferences
 * - PUT /api/member/notifications/preferences - Update preferences
 *
 * Internal Endpoints:
 * - POST /api/internal/notifications/send - Send notification (admin only)
 * - GET /api/internal/notifications/stats - Analytics
 */
@RestController
class NotificationController(
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository,
    private val preferenceRepository: NotificationPreferenceRepository
) {

    // === Member Endpoints ===

    @GetMapping("/api/member/notifications")
    fun getMyNotifications(
        @RequestAttribute("memberId") memberIdStr: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) unreadOnly: Boolean?
    ): ResponseEntity<Map<String, Any>> {
        val memberId = UUID.fromString(memberIdStr)

        val notifications = if (unreadOnly == true) {
            notificationRepository.findUnreadByRecipientAndChannel(
                RecipientType.MEMBER,
                memberId,
                NotificationChannel.IN_APP
            )
        } else {
            notificationRepository.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                RecipientType.MEMBER,
                memberId,
                PageRequest.of(page, size)
            ).content
        }

        val unreadCount = notificationRepository.findUnreadByRecipientAndChannel(
            RecipientType.MEMBER,
            memberId,
            NotificationChannel.IN_APP
        ).size

        return ResponseEntity.ok(
            mapOf(
                "notifications" to notifications.map { it.toDto() },
                "unreadCount" to unreadCount,
                "page" to page,
                "size" to size
            )
        )
    }

    @PutMapping("/api/member/notifications/{id}/read")
    fun markAsRead(
        @RequestAttribute("memberId") memberIdStr: String,
        @PathVariable id: UUID
    ): ResponseEntity<Map<String, String>> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok(mapOf("message" to "Marked as read"))
    }

    @GetMapping("/api/member/notifications/preferences")
    fun getPreferences(
        @RequestAttribute("memberId") memberIdStr: String,
        @RequestAttribute("tenantId") tenantId: String
    ): ResponseEntity<NotificationPreference> {
        val memberId = UUID.fromString(memberIdStr)
        val prefs = preferenceRepository.findByTenantIdAndUserTypeAndUserId(
            tenantId,
            RecipientType.MEMBER,
            memberId
        ) ?: NotificationPreference(
            userType = RecipientType.MEMBER,
            userId = memberId
        ).also { it.tenantId = tenantId }
        return ResponseEntity.ok(prefs)
    }

    @PutMapping("/api/member/notifications/preferences")
    fun updatePreferences(
        @RequestAttribute("memberId") memberIdStr: String,
        @RequestAttribute("tenantId") tenantId: String,
        @RequestBody request: UpdatePreferencesRequest
    ): ResponseEntity<Map<String, String>> {
        val memberId = UUID.fromString(memberIdStr)
        var prefs = preferenceRepository.findByTenantIdAndUserTypeAndUserId(
            tenantId,
            RecipientType.MEMBER,
            memberId
        ) ?: NotificationPreference(
            userType = RecipientType.MEMBER,
            userId = memberId
        ).also { it.tenantId = tenantId }

        request.emailEnabled?.let { prefs.emailEnabled = it }
        request.smsEnabled?.let { prefs.smsEnabled = it }
        request.pushEnabled?.let { prefs.pushEnabled = it }
        request.marketingEnabled?.let { prefs.marketingEnabled = it }
        request.reminderEnabled?.let { prefs.reminderEnabled = it }

        preferenceRepository.save(prefs)

        return ResponseEntity.ok(mapOf("message" to "Preferences updated"))
    }
}

// === DTOs ===

data class UpdatePreferencesRequest(
    val emailEnabled: Boolean?,
    val smsEnabled: Boolean?,
    val pushEnabled: Boolean?,
    val marketingEnabled: Boolean?,
    val reminderEnabled: Boolean?
)

fun Notification.toDto() = mapOf(
    "id" to id.toString(),
    "type" to type.name,
    "channel" to channel.name,
    "priority" to priority.name,
    "subject" to subject,
    "message" to message,
    "status" to status.name,
    "sentAt" to sentAt?.toString(),
    "readAt" to readAt?.toString(),
    "createdAt" to createdAt?.toString()
)
