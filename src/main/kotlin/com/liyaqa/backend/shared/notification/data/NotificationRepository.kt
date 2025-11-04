package com.liyaqa.backend.shared.notification.data

import com.liyaqa.backend.shared.notification.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {

    // === Recipient Queries ===

    fun findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
        recipientType: RecipientType,
        recipientId: UUID,
        pageable: Pageable
    ): Page<Notification>

    fun findByRecipientTypeAndRecipientIdAndStatus(
        recipientType: RecipientType,
        recipientId: UUID,
        status: NotificationStatus
    ): List<Notification>

    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipientType = :recipientType
        AND n.recipientId = :recipientId
        AND n.channel = :channel
        AND n.readAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    fun findUnreadByRecipientAndChannel(
        @Param("recipientType") recipientType: RecipientType,
        @Param("recipientId") recipientId: UUID,
        @Param("channel") channel: NotificationChannel
    ): List<Notification>

    // === Tenant Queries ===

    fun findByTenantIdAndCreatedAtAfter(
        tenantId: String,
        since: Instant,
        pageable: Pageable
    ): Page<Notification>

    fun countByTenantIdAndCreatedAtBetween(
        tenantId: String,
        start: Instant,
        end: Instant
    ): Long

    // === Status Queries ===

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = :status
        AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now)
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        ORDER BY n.priority DESC, n.createdAt ASC
    """)
    fun findPendingNotifications(
        @Param("status") status: NotificationStatus,
        @Param("now") now: Instant,
        pageable: Pageable
    ): Page<Notification>

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = 'FAILED'
        AND n.retryCount < n.maxRetries
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        ORDER BY n.priority DESC, n.failedAt ASC
    """)
    fun findRetryableNotifications(
        @Param("now") now: Instant,
        pageable: Pageable
    ): Page<Notification>

    fun findByStatusAndSentAtBefore(
        status: NotificationStatus,
        before: Instant
    ): List<Notification>

    // === Channel and Type Queries ===

    fun findByChannelAndStatusAndCreatedAtAfter(
        channel: NotificationChannel,
        status: NotificationStatus,
        since: Instant
    ): List<Notification>

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.type = :type
        AND n.status = 'DELIVERED'
        AND n.createdAtBetween :start AND :end
    """)
    fun countDeliveredByTypeAndPeriod(
        @Param("type") type: NotificationType,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): Long

    // === Analytics Queries ===

    @Query("""
        SELECT n.channel, n.status, COUNT(n)
        FROM Notification n
        WHERE n.tenantId = :tenantId
        AND n.createdAt BETWEEN :start AND :end
        GROUP BY n.channel, n.status
    """)
    fun getDeliveryStatsByTenant(
        @Param("tenantId") tenantId: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    @Query("""
        SELECT n.type, COUNT(n), AVG(CAST(n.clickTracked AS int))
        FROM Notification n
        WHERE n.tenantId = :tenantId
        AND n.createdAt BETWEEN :start AND :end
        AND n.status = 'DELIVERED'
        GROUP BY n.type
    """)
    fun getEngagementStatsByType(
        @Param("tenantId") tenantId: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    // === Context Queries ===

    fun findByContextTypeAndContextId(
        contextType: String,
        contextId: UUID
    ): List<Notification>

    // === Cleanup ===

    fun deleteByCreatedAtBeforeAndStatusIn(
        before: Instant,
        statuses: List<NotificationStatus>
    ): Int
}
