package com.liyaqa.backend.shared.notification.data

import com.liyaqa.backend.shared.notification.domain.NotificationChannel
import com.liyaqa.backend.shared.notification.domain.NotificationTemplate
import com.liyaqa.backend.shared.notification.domain.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, UUID> {

    fun findByTemplateCodeAndTenantId(
        templateCode: String,
        tenantId: String?
    ): NotificationTemplate?

    /**
     * Find template with tenant fallback to global.
     * Returns tenant-specific template if exists, otherwise global template.
     */
    @Query("""
        SELECT t FROM NotificationTemplate t
        WHERE t.templateCode = :templateCode
        AND (t.tenantId = :tenantId OR t.tenantId IS NULL)
        AND t.isActive = true
        ORDER BY t.tenantId DESC
    """)
    fun findByTemplateCodeWithFallback(
        @Param("templateCode") templateCode: String,
        @Param("tenantId") tenantId: String
    ): List<NotificationTemplate>

    fun findByNotificationTypeAndChannelAndLanguageAndIsActive(
        notificationType: NotificationType,
        channel: NotificationChannel,
        language: String,
        isActive: Boolean
    ): List<NotificationTemplate>

    fun findByTenantIdAndIsActive(
        tenantId: String?,
        isActive: Boolean
    ): List<NotificationTemplate>

    @Query("""
        SELECT t FROM NotificationTemplate t
        WHERE t.notificationType = :type
        AND t.channel = :channel
        AND t.isActive = true
        AND (t.tenantId = :tenantId OR t.tenantId IS NULL)
        AND t.language = :language
        ORDER BY t.tenantId DESC, t.version DESC
    """)
    fun findBestMatchingTemplate(
        @Param("type") type: NotificationType,
        @Param("channel") channel: NotificationChannel,
        @Param("tenantId") tenantId: String,
        @Param("language") language: String
    ): List<NotificationTemplate>

    fun findByAbTestVariantIsNotNullAndIsActive(
        isActive: Boolean
    ): List<NotificationTemplate>
}
