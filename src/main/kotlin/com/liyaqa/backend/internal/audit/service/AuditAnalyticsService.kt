package com.liyaqa.backend.internal.audit.service

import com.liyaqa.backend.internal.audit.domain.AuditAction
import com.liyaqa.backend.internal.audit.domain.AuditResult
import com.liyaqa.backend.internal.audit.domain.RiskLevel
import com.liyaqa.backend.internal.audit.domain.EntityType
import com.liyaqa.backend.internal.audit.data.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Analytics service for audit log reporting and insights.
 *
 * This service provides high-level analytics and reporting capabilities
 * built on top of the raw audit logs. Use cases:
 *
 * - **Compliance Reports**: "Show me all GDPR-related data accesses"
 * - **Security Dashboards**: "What are the most suspicious activities?"
 * - **Operational Metrics**: "Which actions take the longest?"
 * - **Usage Analytics**: "Who are our most/least active employees?"
 * - **Trend Analysis**: "How has login activity changed over time?"
 *
 * All analytics are read-only and don't modify audit logs (immutability).
 */
@Service
class AuditAnalyticsService(
    private val auditLogRepository: AuditLogRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // ========== Activity Analytics ==========

    /**
     * Get action distribution over a time period.
     * Returns map of action -> count, sorted by frequency.
     *
     * Use case: Dashboard showing most common operations.
     */
    fun getActionDistribution(start: Instant, end: Instant): Map<String, Long> {
        logger.debug("Getting action distribution from $start to $end")
        val results = auditLogRepository.countActionsByType(start, end)

        return results.associate { row ->
            val action = row[0] as AuditAction
            val count = (row[1] as Number).toLong()
            action.name to count
        }.toSortedMap()
    }

    /**
     * Get employee activity leaderboard.
     * Returns map of employee email -> action count.
     *
     * Use case: Identify most/least active employees for workload balancing.
     */
    fun getEmployeeActivityLeaderboard(start: Instant, end: Instant, limit: Int = 20): Map<String, Long> {
        logger.debug("Getting employee activity leaderboard (top $limit)")
        val results = auditLogRepository.countActionsByEmployee(start, end)

        return results.take(limit).associate { row ->
            val email = row[0] as String
            val count = (row[1] as Number).toLong()
            email to count
        }
    }

    /**
     * Get most accessed entities.
     * Returns list of (entityType, entityId, accessCount).
     *
     * Use case: Identify hot spots in the system for optimization.
     */
    fun getMostAccessedEntities(start: Instant, end: Instant, limit: Int = 20): List<EntityAccess> {
        logger.debug("Getting most accessed entities (top $limit)")
        val pageable = PageRequest.of(0, limit)
        val results = auditLogRepository.getMostAccessedEntities(start, end, pageable)

        return results.map { row ->
            EntityAccess(
                entityType = (row[0] as EntityType).name,
                entityId = row[1] as String,
                accessCount = (row[2] as Number).toLong()
            )
        }
    }

    /**
     * Get hourly activity pattern.
     * Returns map of hour (0-23) -> count.
     *
     * Use case: Capacity planning and identifying peak usage hours.
     */
    fun getHourlyActivityPattern(start: Instant, end: Instant): Map<Int, Long> {
        logger.debug("Getting hourly activity pattern")
        val results = auditLogRepository.getHourlyActivityDistribution(start, end)

        return results.associate { row ->
            val hour = (row[0] as Number).toInt()
            val count = (row[1] as Number).toLong()
            hour to count
        }.toSortedMap()
    }

    // ========== Security Analytics ==========

    /**
     * Get risk level distribution.
     * Returns map of risk level -> count.
     *
     * Use case: Security posture assessment and trend monitoring.
     */
    fun getRiskLevelDistribution(start: Instant, end: Instant): Map<String, Long> {
        logger.debug("Getting risk level distribution")
        val results = auditLogRepository.getRiskLevelDistribution(start, end)

        return results.associate { row ->
            val riskLevel = row[0] as RiskLevel
            val count = (row[1] as Number).toLong()
            riskLevel.name to count
        }
    }

    /**
     * Get failed action statistics.
     * Returns map of action -> failure count.
     *
     * Use case: Error rate monitoring and quality metrics.
     */
    fun getFailureStatistics(start: Instant, end: Instant): Map<String, Long> {
        logger.debug("Getting failure statistics")
        val results = auditLogRepository.countFailuresByAction(start, end)

        return results.associate { row ->
            val action = row[0] as AuditAction
            val count = (row[1] as Number).toLong()
            action.name to count
        }
    }

    /**
     * Get login patterns by IP address.
     * Returns list of (ipAddress, uniqueUsers, totalLogins).
     *
     * Use case: Geographic distribution and anomaly detection.
     */
    fun getLoginPatternsByIp(start: Instant, end: Instant, limit: Int = 50): List<IpLoginPattern> {
        logger.debug("Getting login patterns by IP (top $limit)")
        val pageable = PageRequest.of(0, limit)
        val results = auditLogRepository.getLoginPatternsByIp(start, end, pageable)

        return results.map { row ->
            IpLoginPattern(
                ipAddress = row[0] as String,
                uniqueUsers = (row[1] as Number).toLong(),
                totalLogins = (row[2] as Number).toLong()
            )
        }
    }

    /**
     * Detect potential security anomalies.
     * Returns summary of suspicious patterns.
     *
     * Use case: Proactive security monitoring.
     */
    fun detectSecurityAnomalies(start: Instant, end: Instant): SecurityAnomalyReport {
        logger.debug("Detecting security anomalies")

        // Get high-risk actions
        val highRiskActions = auditLogRepository.findByRiskLevelInAndTimestampAfterOrderByTimestampDesc(
            listOf(RiskLevel.HIGH, RiskLevel.CRITICAL),
            start,
            PageRequest.of(0, 100)
        )

        // Get failed actions
        val failedActions = auditLogRepository.findByResultAndTimestampAfterOrderByTimestampDesc(
            AuditResult.FAILURE,
            start,
            PageRequest.of(0, 100)
        )

        // Analyze patterns
        val suspiciousIps = highRiskActions.content
            .mapNotNull { it.ipAddress }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 5 } // More than 5 high-risk actions from one IP
            .keys.toList()

        val accountsAtRisk = highRiskActions.content
            .groupBy { it.employeeEmail }
            .filter { it.value.size > 3 } // More than 3 high-risk actions per account
            .keys.toList()

        return SecurityAnomalyReport(
            totalHighRiskActions = highRiskActions.totalElements,
            totalFailedActions = failedActions.totalElements,
            suspiciousIpAddresses = suspiciousIps,
            accountsAtRisk = accountsAtRisk,
            timeRange = TimeRange(start, end)
        )
    }

    // ========== Performance Analytics ==========

    /**
     * Get action performance metrics.
     * Returns map of action -> (avgDuration, maxDuration, minDuration).
     *
     * Use case: Performance optimization and SLA monitoring.
     */
    fun getActionPerformanceMetrics(start: Instant, end: Instant): Map<String, PerformanceMetric> {
        logger.debug("Getting action performance metrics")
        val results = auditLogRepository.calculateAverageDurationByAction(start, end)

        return results.associate { row ->
            val action = row[0] as AuditAction
            val avgMs = (row[1] as Number).toDouble()
            val maxMs = (row[2] as Number).toLong()
            val minMs = (row[3] as Number).toLong()

            action.name to PerformanceMetric(
                averageDurationMs = avgMs,
                maxDurationMs = maxMs,
                minDurationMs = minMs
            )
        }
    }

    // ========== Tenant Analytics ==========

    /**
     * Get tenant activity summary.
     * Returns per-tenant usage metrics.
     *
     * Use case: Tenant billing and resource allocation.
     */
    fun getTenantActivitySummary(start: Instant, end: Instant, limit: Int = 100): List<TenantActivity> {
        logger.debug("Getting tenant activity summary (top $limit)")
        val pageable = PageRequest.of(0, limit)
        val results = auditLogRepository.getTenantActivitySummary(start, end, pageable)

        return results.map { row ->
            TenantActivity(
                tenantId = row[0] as String,
                actionCount = (row[1] as Number).toLong(),
                uniqueEmployees = (row[2] as Number).toLong()
            )
        }
    }

    // ========== Compliance Reports ==========

    /**
     * Generate comprehensive compliance report.
     * Includes all audit activity for regulatory reporting.
     *
     * Use case: GDPR, SOC2, HIPAA compliance audits.
     */
    fun generateComplianceReport(start: Instant, end: Instant): ComplianceReport {
        logger.info("Generating compliance report from $start to $end")

        val actionDist = getActionDistribution(start, end)
        val riskDist = getRiskLevelDistribution(start, end)
        val failures = getFailureStatistics(start, end)

        val totalActions = actionDist.values.sum()
        val highRiskCount = riskDist.getOrDefault("HIGH", 0) + riskDist.getOrDefault("CRITICAL", 0)
        val totalFailures = failures.values.sum()

        return ComplianceReport(
            reportPeriod = TimeRange(start, end),
            totalAuditedActions = totalActions,
            totalHighRiskActions = highRiskCount,
            totalFailures = totalFailures,
            actionBreakdown = actionDist,
            riskBreakdown = riskDist,
            failureBreakdown = failures,
            generatedAt = Instant.now()
        )
    }

    /**
     * Get quick summary for the last 24 hours.
     * Convenient method for daily dashboards.
     */
    fun getDailySummary(): DailySummary {
        val now = Instant.now()
        val yesterday = now.minus(24, ChronoUnit.HOURS)

        val actionDist = getActionDistribution(yesterday, now)
        val riskDist = getRiskLevelDistribution(yesterday, now)
        val topEmployees = getEmployeeActivityLeaderboard(yesterday, now, 5)

        return DailySummary(
            date = now,
            totalActions = actionDist.values.sum(),
            criticalActions = riskDist.getOrDefault("CRITICAL", 0),
            topActions = actionDist.entries.take(5).associate { it.key to it.value },
            topEmployees = topEmployees
        )
    }

    // ========== Data Classes for Responses ==========

    data class EntityAccess(
        val entityType: String,
        val entityId: String,
        val accessCount: Long
    )

    data class IpLoginPattern(
        val ipAddress: String,
        val uniqueUsers: Long,
        val totalLogins: Long
    )

    data class SecurityAnomalyReport(
        val totalHighRiskActions: Long,
        val totalFailedActions: Long,
        val suspiciousIpAddresses: List<String>,
        val accountsAtRisk: List<String>,
        val timeRange: TimeRange
    )

    data class PerformanceMetric(
        val averageDurationMs: Double,
        val maxDurationMs: Long,
        val minDurationMs: Long
    )

    data class TenantActivity(
        val tenantId: String,
        val actionCount: Long,
        val uniqueEmployees: Long
    )

    data class TimeRange(
        val start: Instant,
        val end: Instant
    )

    data class ComplianceReport(
        val reportPeriod: TimeRange,
        val totalAuditedActions: Long,
        val totalHighRiskActions: Long,
        val totalFailures: Long,
        val actionBreakdown: Map<String, Long>,
        val riskBreakdown: Map<String, Long>,
        val failureBreakdown: Map<String, Long>,
        val generatedAt: Instant
    )

    data class DailySummary(
        val date: Instant,
        val totalActions: Long,
        val criticalActions: Long,
        val topActions: Map<String, Long>,
        val topEmployees: Map<String, Long>
    )
}
