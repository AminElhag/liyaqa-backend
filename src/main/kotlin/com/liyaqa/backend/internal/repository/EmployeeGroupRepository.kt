package com.liyaqa.backend.internal.repository

import com.liyaqa.backend.internal.domain.audit.AuditAction
import com.liyaqa.backend.internal.domain.audit.AuditLog
import com.liyaqa.backend.internal.domain.audit.EntityType
import com.liyaqa.backend.internal.domain.employee.EmployeeGroup
import com.liyaqa.backend.internal.domain.employee.Permission
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Repository for employee group management.
 *
 * Groups are the cornerstone of our RBAC system. This repository design
 * ensures we can efficiently manage permission inheritance and group
 * hierarchies as our organization scales.
 */
@Repository
interface EmployeeGroupRepository : JpaRepository<EmployeeGroup, UUID> {

    fun findByName(name: String): EmployeeGroup?
    fun existsByName(name: String): Boolean

    /**
     * System groups have special protection - they can't be deleted.
     * This query supports our UI's need to distinguish editable from
     * protected groups.
     */
    fun findByIsSystemTrue(): List<EmployeeGroup>
    fun findByIsSystemFalse(): List<EmployeeGroup>

    /**
     * Permission-based group discovery for role assignment workflows.
     * This allows admins to quickly find appropriate groups when
     * onboarding new team members with specific responsibilities.
     */
    @Query("""
        SELECT DISTINCT g FROM EmployeeGroup g 
        JOIN g.permissions p 
        WHERE p IN :permissions
    """)
    fun findByPermissions(@Param("permissions") permissions: Set<Permission>): List<EmployeeGroup>
}

/**
 * Repository for immutable audit logs.
 *
 * This design reflects our commitment to transparency and accountability.
 * Audit logs are write-only from the application perspective - we never
 * update or delete them, ensuring an unalterable record of system activity.
 *
 * From a business perspective, this protects us during:
 * - Security incidents (forensic analysis)
 * - Compliance audits (demonstrable controls)
 * - Customer disputes (activity verification)
 * - Performance reviews (objective activity metrics)
 */
@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {

    /**
     * Employee activity queries for security monitoring and HR purposes.
     * These support both real-time threat detection and periodic reviews.
     */
    fun findByEmployeeIdOrderByTimestampDesc(
        employeeId: UUID,
        pageable: Pageable
    ): Page<AuditLog>

    fun findByEmployeeEmailOrderByTimestampDesc(
        email: String,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Entity-focused queries for tracking object lifecycle.
     * Critical for debugging and understanding how data evolved.
     */
    fun findByEntityTypeAndEntityIdOrderByTimestampDesc(
        entityType: EntityType,
        entityId: String,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Tenant impact queries for customer support and compliance.
     * This allows us to provide complete activity reports to customers
     * who request them for their own compliance needs.
     */
    fun findByAffectedTenantIdOrderByTimestampDesc(
        tenantId: String,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Risk-based queries for security team dashboards.
     * This design allows real-time monitoring of high-risk activities
     * without overwhelming operators with noise.
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.riskLevel IN ('HIGH', 'CRITICAL') 
        AND a.timestamp > :since 
        ORDER BY a.timestamp DESC
    """)
    fun findHighRiskActivities(
        @Param("since") since: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Failed operation queries for system health monitoring.
     * These feed into our alerting system to detect systemic issues
     * before they impact users.
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.result != 'SUCCESS' 
        AND a.timestamp > :since 
        ORDER BY a.timestamp DESC
    """)
    fun findFailedOperations(
        @Param("since") since: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Authentication security queries supporting threat detection.
     * These patterns were developed in response to actual attack vectors
     * we've observed in production.
     */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a 
        WHERE a.action = 'PASSWORD_RESET_REQUESTED' 
        AND a.employeeEmail = :email 
        AND a.timestamp > :since
    """)
    fun countRecentPasswordResets(
        @Param("email") email: String,
        @Param("since") since: Instant
    ): Long

    @Query("""
        SELECT COUNT(DISTINCT a.ipAddress) FROM AuditLog a 
        WHERE a.employeeId = :employeeId 
        AND a.action = 'LOGIN' 
        AND a.result = 'SUCCESS' 
        AND a.timestamp > :since
    """)
    fun countDistinctLoginLocations(
        @Param("employeeId") employeeId: UUID,
        @Param("since") since: Instant
    ): Long

    /**
     * Compliance reporting queries for regulatory requirements.
     * These support our ability to demonstrate control effectiveness
     * to auditors and regulators.
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.action IN :actions 
        AND a.timestamp BETWEEN :startDate AND :endDate 
        ORDER BY a.timestamp DESC
    """)
    fun findByActionsInDateRange(
        @Param("actions") actions: List<AuditAction>,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<AuditLog>
}