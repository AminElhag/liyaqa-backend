package com.liyaqa.backend.shared.analytics.data

import com.liyaqa.backend.shared.analytics.domain.Report
import com.liyaqa.backend.shared.analytics.domain.ReportStatus
import com.liyaqa.backend.shared.analytics.domain.ReportType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface ReportRepository : JpaRepository<Report, UUID> {

    fun findByTenantIdOrderByCreatedAtDesc(tenantId: String, pageable: Pageable): Page<Report>

    fun findByTenantIdAndReportType(tenantId: String, reportType: ReportType): List<Report>

    fun findByStatus(status: ReportStatus): List<Report>

    @Query("""
        SELECT r FROM Report r
        WHERE r.isScheduled = true
        AND r.nextRunAt <= :now
        AND r.status != 'GENERATING'
        ORDER BY r.nextRunAt ASC
    """)
    fun findScheduledReportsReadyToRun(@Param("now") now: Instant): List<Report>

    fun findByTenantIdAndCreatedAtBetween(
        tenantId: String,
        start: Instant,
        end: Instant
    ): List<Report>

    fun countByTenantIdAndStatus(tenantId: String, status: ReportStatus): Long
}
