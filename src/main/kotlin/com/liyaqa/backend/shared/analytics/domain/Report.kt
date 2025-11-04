package com.liyaqa.backend.shared.analytics.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Report entity for saved/scheduled reports.
 *
 * Design Philosophy:
 * Reports transform raw operational data into strategic business insights.
 * This entity supports both ad-hoc analysis and scheduled recurring reports.
 *
 * Use Cases:
 * - Monthly revenue summaries for stakeholders
 * - Weekly utilization reports for operations
 * - Daily booking summaries for staff
 * - Annual performance reviews for management
 * - Custom analysis for specific time periods
 */
@Entity
@Table(
    name = "reports",
    indexes = [
        Index(name = "idx_report_tenant", columnList = "tenant_id,created_at"),
        Index(name = "idx_report_type", columnList = "report_type,created_at"),
        Index(name = "idx_report_status", columnList = "status"),
        Index(name = "idx_report_schedule", columnList = "is_scheduled,next_run_at")
    ]
)
class Report(

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 100)
    var reportType: ReportType,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    // === Time Range ===

    @Column(name = "start_date")
    var startDate: Instant,

    @Column(name = "end_date")
    var endDate: Instant,

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 50)
    var periodType: PeriodType? = null, // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY

    // === Filters ===

    @Column(name = "facility_id")
    var facilityId: UUID? = null,

    @Column(name = "branch_id")
    var branchId: UUID? = null,

    @Column(name = "filters", columnDefinition = "TEXT")
    var filters: String? = null, // JSON string of additional filters

    // === Generation ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(name = "generated_at")
    var generatedAt: Instant? = null,

    @Column(name = "generation_duration_ms")
    var generationDurationMs: Long? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    // === Results ===

    @Column(name = "data", columnDefinition = "TEXT")
    var data: String? = null, // JSON string of report data

    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = null, // JSON string of summary metrics

    @Column(name = "file_path", length = 500)
    var filePath: String? = null, // Path to exported file (PDF, Excel, etc.)

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 50)
    var format: ReportFormat = ReportFormat.JSON,

    // === Scheduling ===

    @Column(name = "is_scheduled")
    var isScheduled: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_frequency", length = 50)
    var scheduleFrequency: ScheduleFrequency? = null,

    @Column(name = "next_run_at")
    var nextRunAt: Instant? = null,

    @Column(name = "last_run_at")
    var lastRunAt: Instant? = null,

    @Column(name = "recipient_emails", columnDefinition = "TEXT")
    var recipientEmails: String? = null, // JSON array of email addresses

    @Column(name = "notify_on_completion")
    var notifyOnCompletion: Boolean = false,

    // === Metadata ===

    @Column(name = "created_by_id")
    var createdById: UUID? = null,

    @Column(name = "created_by_name", length = 255)
    var createdByName: String? = null

) : BaseEntity() {

    fun markAsGenerating() {
        this.status = ReportStatus.GENERATING
    }

    fun markAsCompleted(data: String, summary: String?, durationMs: Long) {
        this.status = ReportStatus.COMPLETED
        this.generatedAt = Instant.now()
        this.data = data
        this.summary = summary
        this.generationDurationMs = durationMs
        this.lastRunAt = Instant.now()
    }

    fun markAsFailed(errorMessage: String) {
        this.status = ReportStatus.FAILED
        this.errorMessage = errorMessage
        this.lastRunAt = Instant.now()
    }

    fun scheduleNextRun() {
        if (!isScheduled || scheduleFrequency == null) return

        nextRunAt = when (scheduleFrequency) {
            ScheduleFrequency.DAILY -> Instant.now().plusSeconds(86400)
            ScheduleFrequency.WEEKLY -> Instant.now().plusSeconds(604800)
            ScheduleFrequency.MONTHLY -> Instant.now().plusSeconds(2592000)
            ScheduleFrequency.QUARTERLY -> Instant.now().plusSeconds(7776000)
            null -> null
        }
    }
}

/**
 * Types of reports available in the system.
 */
enum class ReportType {
    REVENUE_SUMMARY,           // Total revenue, by payment method, refunds
    BOOKING_ANALYTICS,         // Utilization, peak times, popular courts
    MEMBERSHIP_ANALYTICS,      // Active members, churn, new signups
    FACILITY_PERFORMANCE,      // Overall KPIs and metrics
    PAYMENT_RECONCILIATION,    // Payment transactions, settlements
    CUSTOMER_INSIGHTS,         // Member behavior, engagement
    COURT_UTILIZATION,         // Per-court usage and revenue
    STAFF_ACTIVITY,            // Staff actions and productivity
    CUSTOM                     // User-defined custom reports
}

/**
 * Report generation status.
 */
enum class ReportStatus {
    PENDING,       // Queued for generation
    GENERATING,    // Currently being generated
    COMPLETED,     // Successfully generated
    FAILED        // Generation failed
}

/**
 * Report output format.
 */
enum class ReportFormat {
    JSON,          // Raw JSON data
    PDF,           // PDF document
    EXCEL,         // Excel spreadsheet
    CSV            // CSV file
}

/**
 * Time period type for reports.
 */
enum class PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

/**
 * Schedule frequency for recurring reports.
 */
enum class ScheduleFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY
}
