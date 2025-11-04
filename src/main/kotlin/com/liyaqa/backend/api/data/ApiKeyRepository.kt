package com.liyaqa.backend.api.data

import com.liyaqa.backend.api.domain.ApiKey
import com.liyaqa.backend.api.domain.ApiKeyStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for API Key management.
 */
@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    /**
     * Find API key by key prefix (for identification).
     */
    fun findByKeyPrefix(keyPrefix: String): ApiKey?

    /**
     * Find API keys by facility.
     */
    fun findByFacilityIdOrderByCreatedAtDesc(facilityId: UUID): List<ApiKey>

    /**
     * Find API keys by tenant.
     */
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: String): List<ApiKey>

    /**
     * Find active API keys by facility.
     */
    fun findByFacilityIdAndStatus(facilityId: UUID, status: ApiKeyStatus): List<ApiKey>

    /**
     * Find API key by prefix and check if active.
     */
    @Query("""
        SELECT k FROM ApiKey k
        WHERE k.keyPrefix = :prefix
        AND k.status = 'ACTIVE'
    """)
    fun findActiveByPrefix(@Param("prefix") prefix: String): ApiKey?

    /**
     * Count active keys for a facility.
     */
    fun countByFacilityIdAndStatus(facilityId: UUID, status: ApiKeyStatus): Long
}
