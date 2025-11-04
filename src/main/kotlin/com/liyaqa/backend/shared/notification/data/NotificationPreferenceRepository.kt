package com.liyaqa.backend.shared.notification.data

import com.liyaqa.backend.shared.notification.domain.NotificationPreference
import com.liyaqa.backend.shared.notification.domain.RecipientType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, UUID> {

    fun findByUserTypeAndUserId(
        userType: RecipientType,
        userId: UUID
    ): NotificationPreference?

    fun findByTenantIdAndUserTypeAndUserId(
        tenantId: String,
        userType: RecipientType,
        userId: UUID
    ): NotificationPreference?

    fun findByTenantIdAndMarketingEnabled(
        tenantId: String,
        marketingEnabled: Boolean
    ): List<NotificationPreference>

    fun existsByUserTypeAndUserId(
        userType: RecipientType,
        userId: UUID
    ): Boolean
}
