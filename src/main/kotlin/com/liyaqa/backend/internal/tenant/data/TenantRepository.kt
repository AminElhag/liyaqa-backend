package com.liyaqa.backend.internal.tenant.data

import com.liyaqa.backend.internal.tenant.domain.PlanTier
import com.liyaqa.backend.internal.tenant.domain.SubscriptionStatus
import com.liyaqa.backend.internal.tenant.domain.Tenant
import com.liyaqa.backend.internal.tenant.domain.TenantStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

/**
 * Repository for Tenant entity with comprehensive query capabilities.
 *
 * This supports the internal team's needs to manage customer organizations
 * across multiple dimensions: status, subscription, billing, contracts, etc.
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {

    /**
     * Core identity queries for uniqueness validation and lookups.
     */
    fun findByTenantId(tenantId: String): Tenant?
    fun existsByTenantId(tenantId: String): Boolean
    fun findBySubdomain(subdomain: String): Tenant?
    fun existsBySubdomain(subdomain: String): Boolean

    /**
     * Status-based queries for operational monitoring.
     */
    fun findByStatus(status: TenantStatus): List<Tenant>
    fun countByStatus(status: TenantStatus): Long

    /**
     * Subscription-based queries for billing and account management.
     */
    fun findBySubscriptionStatus(subscriptionStatus: SubscriptionStatus): List<Tenant>
    fun countBySubscriptionStatus(subscriptionStatus: SubscriptionStatus): Long

    /**
     * Plan tier queries for revenue analytics and capacity planning.
     */
    fun findByPlanTier(planTier: PlanTier): List<Tenant>
    fun countByPlanTier(planTier: PlanTier): Long

    /**
     * Combined status queries for account health monitoring.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.status = :tenantStatus
        AND t.subscriptionStatus = :subscriptionStatus
        """
    )
    fun findByStatusAndSubscriptionStatus(
        @Param("tenantStatus") tenantStatus: TenantStatus,
        @Param("subscriptionStatus") subscriptionStatus: SubscriptionStatus
    ): List<Tenant>

    /**
     * Find tenants with active subscriptions (ACTIVE or TRIAL).
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.subscriptionStatus IN ('ACTIVE', 'TRIAL')
        AND t.status = 'ACTIVE'
        """
    )
    fun findActiveSubscriptions(): List<Tenant>

    /**
     * Find tenants with past due payments needing attention.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.subscriptionStatus = 'PAST_DUE'
        AND t.status != 'TERMINATED'
        ORDER BY t.updatedAt ASC
        """
    )
    fun findPastDueTenants(): List<Tenant>

    /**
     * Find suspended tenants for review.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.status = 'SUSPENDED'
        ORDER BY t.suspendedAt DESC
        """
    )
    fun findSuspendedTenants(): List<Tenant>

    /**
     * Find tenants with expiring contracts (within N days).
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.contractEndDate IS NOT NULL
        AND t.contractEndDate BETWEEN :today AND :expiryDate
        AND t.status = 'ACTIVE'
        ORDER BY t.contractEndDate ASC
        """
    )
    fun findExpiringContracts(
        @Param("today") today: LocalDate,
        @Param("expiryDate") expiryDate: LocalDate
    ): List<Tenant>

    /**
     * Find tenants with expired contracts.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.contractEndDate < :today
        AND t.status = 'ACTIVE'
        """
    )
    fun findExpiredContracts(@Param("today") today: LocalDate): List<Tenant>

    /**
     * Find tenants who haven't accepted latest terms.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE (t.termsAcceptedAt IS NULL OR t.termsVersion != :currentVersion)
        AND t.status = 'ACTIVE'
        """
    )
    fun findTenantsWithoutCurrentTerms(@Param("currentVersion") currentVersion: String): List<Tenant>

    /**
     * Find tenants in trial period.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.subscriptionStatus = 'TRIAL'
        AND t.status = 'ACTIVE'
        ORDER BY t.createdAt DESC
        """
    )
    fun findTrialTenants(): List<Tenant>

    /**
     * Comprehensive search supporting multiple filter criteria.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE (:searchTerm IS NULL OR
               LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(t.tenantId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR t.status = :status)
        AND (:subscriptionStatus IS NULL OR t.subscriptionStatus = :subscriptionStatus)
        AND (:planTier IS NULL OR t.planTier = :planTier)
        AND (:facilityType IS NULL OR t.facilityType = :facilityType)
        AND (:includeSuspended = true OR t.status != 'SUSPENDED')
        AND (:includeTerminated = true OR t.status != 'TERMINATED')
        """
    )
    fun searchTenants(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: TenantStatus?,
        @Param("subscriptionStatus") subscriptionStatus: SubscriptionStatus?,
        @Param("planTier") planTier: PlanTier?,
        @Param("facilityType") facilityType: String?,
        @Param("includeSuspended") includeSuspended: Boolean,
        @Param("includeTerminated") includeTerminated: Boolean,
        pageable: Pageable
    ): Page<Tenant>

    /**
     * Count tenants by plan tier for revenue analytics.
     */
    @Query(
        """
        SELECT t.planTier, COUNT(t)
        FROM Tenant t
        WHERE t.status = 'ACTIVE'
        GROUP BY t.planTier
        ORDER BY COUNT(t) DESC
        """
    )
    fun countActiveByPlanTier(): List<Array<Any>>

    /**
     * Count tenants by subscription status for billing health.
     */
    @Query(
        """
        SELECT t.subscriptionStatus, COUNT(t)
        FROM Tenant t
        GROUP BY t.subscriptionStatus
        ORDER BY COUNT(t) DESC
        """
    )
    fun countBySubscriptionStatusGrouped(): List<Array<Any>>

    /**
     * Find recently created tenants for onboarding monitoring.
     */
    @Query(
        """
        SELECT t FROM Tenant t
        WHERE t.createdAt >= :since
        ORDER BY t.createdAt DESC
        """
    )
    fun findRecentlyCreated(@Param("since") since: java.time.Instant): List<Tenant>

    /**
     * Find tenants by contact email for support lookup.
     */
    fun findByContactEmailContainingIgnoreCase(email: String): List<Tenant>

    /**
     * Find tenants by facility type for market segmentation.
     */
    fun findByFacilityType(facilityType: String): List<Tenant>

    /**
     * Count total active tenants (business metric).
     */
    @Query(
        """
        SELECT COUNT(t) FROM Tenant t
        WHERE t.status = 'ACTIVE'
        AND t.subscriptionStatus IN ('ACTIVE', 'TRIAL')
        """
    )
    fun countActiveTenants(): Long

    /**
     * Calculate monthly recurring revenue (MRR) by plan tier.
     * Note: This assumes plan pricing is defined elsewhere.
     */
    @Query(
        """
        SELECT t.planTier, COUNT(t)
        FROM Tenant t
        WHERE t.status = 'ACTIVE'
        AND t.subscriptionStatus = 'ACTIVE'
        GROUP BY t.planTier
        """
    )
    fun getMRRByPlanTier(): List<Array<Any>>
}
