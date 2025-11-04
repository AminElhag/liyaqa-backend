package com.liyaqa.backend.shared.analytics.service

import com.liyaqa.backend.shared.analytics.data.ReportRepository
import com.liyaqa.backend.shared.analytics.domain.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Report generation and management service.
 */
@Service
@Transactional
class ReportService(
    private val reportRepository: ReportRepository,
    private val analyticsService: AnalyticsService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generate a report.
     */
    @Async
    fun generateReport(reportId: UUID) {
        val report = reportRepository.findById(reportId).orElse(null) ?: return

        report.markAsGenerating()
        reportRepository.save(report)

        val startTime = System.currentTimeMillis()

        try {
            val data = when (report.reportType) {
                ReportType.REVENUE_SUMMARY -> {
                    val metrics = analyticsService.calculateRevenueMetrics(
                        report.tenantId,
                        report.startDate,
                        report.endDate,
                        report.branchId
                    )
                    objectMapper.writeValueAsString(metrics)
                }
                ReportType.BOOKING_ANALYTICS -> {
                    val metrics = analyticsService.calculateBookingMetrics(
                        report.tenantId,
                        report.startDate,
                        report.endDate,
                        report.branchId
                    )
                    objectMapper.writeValueAsString(metrics)
                }
                ReportType.MEMBERSHIP_ANALYTICS -> {
                    val metrics = analyticsService.calculateMembershipMetrics(
                        report.tenantId,
                        report.startDate,
                        report.endDate,
                        report.branchId
                    )
                    objectMapper.writeValueAsString(metrics)
                }
                ReportType.FACILITY_PERFORMANCE -> {
                    val metrics = analyticsService.calculateFacilityPerformance(
                        report.tenantId,
                        report.startDate,
                        report.endDate,
                        report.branchId
                    )
                    objectMapper.writeValueAsString(metrics)
                }
                else -> "{}"
            }

            val duration = System.currentTimeMillis() - startTime
            report.markAsCompleted(data, null, duration)

            if (report.isScheduled) {
                report.scheduleNextRun()
            }

            reportRepository.save(report)

            logger.info("Report generated successfully: id={}, type={}, duration={}ms",
                reportId, report.reportType, duration)

        } catch (ex: Exception) {
            logger.error("Report generation failed: id={}, error={}", reportId, ex.message, ex)
            report.markAsFailed("Generation error: ${ex.message}")
            reportRepository.save(report)
        }
    }

    /**
     * Process scheduled reports every hour.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    fun processScheduledReports() {
        val reports = reportRepository.findScheduledReportsReadyToRun(Instant.now())
        logger.info("Processing {} scheduled reports", reports.size)

        reports.forEach { report ->
            generateReport(report.id!!)
        }
    }

    /**
     * Create and queue a report.
     */
    fun createReport(
        tenantId: String,
        reportType: ReportType,
        name: String,
        startDate: Instant,
        endDate: Instant,
        branchId: UUID? = null
    ): UUID {
        val report = Report(
            reportType = reportType,
            name = name,
            description = null,
            startDate = startDate,
            endDate = endDate,
            periodType = null,
            facilityId = null,
            branchId = branchId
        )
        report.tenantId = tenantId

        val saved = reportRepository.save(report)
        generateReport(saved.id!!)

        return saved.id!!
    }
}
