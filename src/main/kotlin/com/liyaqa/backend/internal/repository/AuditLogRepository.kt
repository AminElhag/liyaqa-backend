package com.liyaqa.backend.internal.repository

import com.liyaqa.backend.internal.domain.audit.AuditAction
import com.liyaqa.backend.internal.domain.audit.AuditLog
import com.liyaqa.backend.internal.domain.audit.AuditResult
import com.liyaqa.backend.internal.domain.audit.EntityType
import com.liyaqa.backend.internal.domain.audit.RiskLevel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Repository for audit log persistence and analytics queries.
 *
 * This repository provides comprehensive querying capabilities for:
 * - Compliance reporting (who did what, when)
 * - Security monitoring (failed logins, suspicious activity)
 * - Operational analytics (action frequency, performance metrics)
 * - Forensic investigation (detailed activity timelines)
 *
 * All queries are optimized with appropriate indexes defined in AuditLog entity.
 */
@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {

    // ========== Basic Queries ==========

    /**
     * Find all logs for a specific employee.
     * Used for employee activity history and compliance reports.
     */
    fun findByEmployeeIdOrderByTimestampDesc(
        employeeId: UUID,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Find logs for a specific entity (e.g., all changes to a particular employee or tenant).
     * Critical for audit trails and change history.
     */
    fun findByEntityTypeAndEntityIdOrderByTimestampDesc(
        entityType: EntityType,
        entityId: String,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Find logs by action type.
     * Useful for analyzing specific operations (e.g., all password resets, all logins).
     */
    fun findByActionOrderByTimestampDesc(
        action: AuditAction,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Find logs within a time range.
     * Essential for compliance reports and time-based analysis.
     */
    fun findByTimestampBetweenOrderByTimestampDesc(
        start: Instant,
        end: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    // ========== Security & Compliance Queries ==========

    /**
     * Find failed login attempts for rate limiting and security monitoring.
     */
    fun findByActionAndEmployeeEmailAndTimestampAfterOrderByTimestampDesc(
        action: AuditAction,
        email: String,
        since: Instant
    ): List<AuditLog>

    /**
     * Count recent password reset requests (for rate limiting).
     */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.action = 'PASSWORD_RESET_REQUESTED'
        AND a.employeeEmail = :email
        AND a.timestamp >= :since
    """)
    fun countRecentPasswordResets(
        @Param("email") email: String,
        @Param("since") since: Instant
    ): Long

    /**
     * Find high-risk actions within a time period.
     * Critical for security incident investigation.
     */
    fun findByRiskLevelInAndTimestampAfterOrderByTimestampDesc(
        riskLevels: List<RiskLevel>,
        since: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Find failed actions for security monitoring.
     */
    fun findByResultAndTimestampAfterOrderByTimestampDesc(
        result: AuditResult,
        since: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Find suspicious activities for a specific employee.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.employeeId = :employeeId
        AND a.riskLevel IN ('HIGH', 'CRITICAL')
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
    """)
    fun findSuspiciousActivitiesByEmployee(
        @Param("employeeId") employeeId: UUID,
        @Param("since") since: Instant
    ): List<AuditLog>

    // ========== Analytics & Reporting Queries ==========

    /**
     * Count actions by type within a time period.
     * Used for operational dashboards and trend analysis.
     */
    @Query("""
        SELECT a.action, COUNT(a)
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
    """)
    fun countActionsByType(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Count actions by employee within a time period.
     * Identifies most/least active employees.
     */
    @Query("""
        SELECT a.employeeEmail, COUNT(a)
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        GROUP BY a.employeeEmail
        ORDER BY COUNT(a) DESC
    """)
    fun countActionsByEmployee(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Calculate average action duration by type.
     * Performance monitoring and optimization insights.
     */
    @Query("""
        SELECT a.action, AVG(a.durationMs), MAX(a.durationMs), MIN(a.durationMs)
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        AND a.durationMs IS NOT NULL
        GROUP BY a.action
        ORDER BY AVG(a.durationMs) DESC
    """)
    fun calculateAverageDurationByAction(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Count failed actions by type.
     * Error rate monitoring and quality metrics.
     */
    @Query("""
        SELECT a.action, COUNT(a)
        FROM AuditLog a
        WHERE a.result = 'FAILURE'
        AND a.timestamp BETWEEN :start AND :end
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
    """)
    fun countFailuresByAction(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Get risk level distribution.
     * Security posture assessment.
     */
    @Query("""
        SELECT a.riskLevel, COUNT(a)
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        GROUP BY a.riskLevel
        ORDER BY a.riskLevel DESC
    """)
    fun getRiskLevelDistribution(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Find most accessed entities.
     * Usage pattern analysis.
     */
    @Query("""
        SELECT a.entityType, a.entityId, COUNT(a)
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        AND a.entityId IS NOT NULL
        GROUP BY a.entityType, a.entityId
        ORDER BY COUNT(a) DESC
    """)
    fun getMostAccessedEntities(
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        pageable: Pageable
    ): List<Array<Any>>

    /**
     * Get hourly activity distribution.
     * Time-based usage patterns for capacity planning.
     */
    @Query("""
        SELECT
            FUNCTION('HOUR', a.timestamp) as hour,
            COUNT(a) as count
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        GROUP BY FUNCTION('HOUR', a.timestamp)
        ORDER BY hour
    """)
    fun getHourlyActivityDistribution(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<Array<Any>>

    /**
     * Find login patterns by IP address.
     * Geographic distribution and anomaly detection.
     */
    @Query("""
        SELECT a.ipAddress, COUNT(DISTINCT a.employeeId) as uniqueUsers, COUNT(a) as totalLogins
        FROM AuditLog a
        WHERE a.action = 'LOGIN'
        AND a.timestamp BETWEEN :start AND :end
        AND a.ipAddress IS NOT NULL
        GROUP BY a.ipAddress
        ORDER BY totalLogins DESC
    """)
    fun getLoginPatternsByIp(
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        pageable: Pageable
    ): List<Array<Any>>

    /**
     * Get tenant activity summary.
     * Per-tenant usage metrics and billing insights.
     */
    @Query("""
        SELECT a.affectedTenantId, COUNT(a) as actionCount,
               COUNT(DISTINCT a.employeeId) as uniqueEmployees
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        AND a.affectedTenantId IS NOT NULL
        GROUP BY a.affectedTenantId
        ORDER BY actionCount DESC
    """)
    fun getTenantActivitySummary(
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        pageable: Pageable
    ): List<Array<Any>>

    // ========== Advanced Search ==========

    /**
     * Full-text search across audit logs.
     * General purpose search for investigations.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (
            LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(a.employeeEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(a.employeeName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        AND a.timestamp BETWEEN :start AND :end
        ORDER BY a.timestamp DESC
    """)
    fun searchAuditLogs(
        @Param("searchTerm") searchTerm: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        pageable: Pageable
    ): Page<AuditLog>
}
