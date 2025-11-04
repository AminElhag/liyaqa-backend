package com.liyaqa.backend.internal.controller

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.shared.analytics.data.ReportRepository
import com.liyaqa.backend.shared.analytics.domain.*
import com.liyaqa.backend.shared.analytics.service.AnalyticsService
import com.liyaqa.backend.shared.analytics.service.ReportService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

/**
 * Analytics and reporting endpoints for internal employees.
 *
 * Design Philosophy:
 * "What gets measured gets managed."
 *
 * This controller provides real-time analytics and scheduled reporting
 * capabilities for business intelligence and operational insights.
 */
@RestController
@RequestMapping("/api/internal/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
    private val reportService: ReportService,
    private val reportRepository: ReportRepository
) {

    /**
     * Get revenue metrics for a specific period.
     */
    @GetMapping("/revenue")
    fun getRevenueMetrics(
        @AuthenticationPrincipal employee: Employee,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(required = false) branchId: UUID?
    ): ResponseEntity<RevenueMetrics> {
        val metrics = analyticsService.calculateRevenueMetrics(
            tenantId = employee.tenantId,
            startDate = startDate,
            endDate = endDate,
            branchId = branchId
        )
        return ResponseEntity.ok(metrics)
    }

    /**
     * Get booking analytics for a specific period.
     */
    @GetMapping("/bookings")
    fun getBookingMetrics(
        @AuthenticationPrincipal employee: Employee,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(required = false) branchId: UUID?
    ): ResponseEntity<BookingMetrics> {
        val metrics = analyticsService.calculateBookingMetrics(
            tenantId = employee.tenantId,
            startDate = startDate,
            endDate = endDate,
            branchId = branchId
        )
        return ResponseEntity.ok(metrics)
    }

    /**
     * Get membership analytics for a specific period.
     */
    @GetMapping("/membership")
    fun getMembershipMetrics(
        @AuthenticationPrincipal employee: Employee,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(required = false) branchId: UUID?
    ): ResponseEntity<MembershipMetrics> {
        val metrics = analyticsService.calculateMembershipMetrics(
            tenantId = employee.tenantId,
            startDate = startDate,
            endDate = endDate,
            branchId = branchId
        )
        return ResponseEntity.ok(metrics)
    }

    /**
     * Get overall facility performance KPIs.
     */
    @GetMapping("/facility-performance")
    fun getFacilityPerformance(
        @AuthenticationPrincipal employee: Employee,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(required = false) branchId: UUID?
    ): ResponseEntity<FacilityPerformance> {
        val performance = analyticsService.calculateFacilityPerformance(
            tenantId = employee.tenantId,
            startDate = startDate,
            endDate = endDate,
            branchId = branchId
        )
        return ResponseEntity.ok(performance)
    }

    /**
     * Create and queue a new report.
     */
    @PostMapping("/reports")
    fun createReport(
        @AuthenticationPrincipal employee: Employee,
        @RequestBody request: CreateReportRequest
    ): ResponseEntity<CreateReportResponse> {
        val reportId = reportService.createReport(
            tenantId = employee.tenantId,
            reportType = request.reportType,
            name = request.name,
            startDate = request.startDate,
            endDate = request.endDate,
            branchId = request.branchId
        )
        return ResponseEntity.ok(CreateReportResponse(reportId))
    }

    /**
     * List all reports for the tenant.
     */
    @GetMapping("/reports")
    fun listReports(
        @AuthenticationPrincipal employee: Employee,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) reportType: ReportType?
    ): ResponseEntity<Page<Report>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())

        val reports = if (reportType != null) {
            reportRepository.findByTenantIdAndReportType(employee.tenantId, reportType)
                .let { list ->
                    org.springframework.data.domain.PageImpl(
                        list.take(size),
                        pageable,
                        list.size.toLong()
                    )
                }
        } else {
            reportRepository.findByTenantIdOrderByCreatedAtDesc(employee.tenantId, pageable)
        }

        return ResponseEntity.ok(reports)
    }

    /**
     * Get a specific report by ID.
     */
    @GetMapping("/reports/{id}")
    fun getReport(
        @AuthenticationPrincipal employee: Employee,
        @PathVariable id: UUID
    ): ResponseEntity<Report> {
        val report = reportRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (report.tenantId != employee.tenantId) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(report)
    }

    /**
     * Delete a report.
     */
    @DeleteMapping("/reports/{id}")
    fun deleteReport(
        @AuthenticationPrincipal employee: Employee,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val report = reportRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (report.tenantId != employee.tenantId) {
            return ResponseEntity.notFound().build()
        }

        reportRepository.delete(report)
        return ResponseEntity.noContent().build()
    }
}

/**
 * Request/Response DTOs
 */
data class CreateReportRequest(
    val reportType: ReportType,
    val name: String,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val startDate: Instant,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val endDate: Instant,
    val branchId: UUID? = null
)

data class CreateReportResponse(
    val reportId: UUID
)
