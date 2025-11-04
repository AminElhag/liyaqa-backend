package com.liyaqa.backend.shared.analytics.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * Data classes for various analytics metrics.
 *
 * Design Rationale:
 * These are DTOs (not entities) representing calculated metrics.
 * They're computed on-demand from transactional data and cached if needed.
 */

/**
 * Revenue metrics for financial analysis.
 */
data class RevenueMetrics(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalRevenue: BigDecimal,
    val revenueByPaymentMethod: Map<String, BigDecimal>,
    val refundTotal: BigDecimal,
    val netRevenue: BigDecimal,
    val averageTransactionValue: BigDecimal,
    val transactionCount: Long,
    val revenueByDay: List<DailyRevenue>,
    val topRevenueStreams: List<RevenueStream>
)

data class DailyRevenue(
    val date: Instant,
    val revenue: BigDecimal,
    val transactionCount: Long
)

data class RevenueStream(
    val source: String, // "booking", "membership", "merchandise"
    val revenue: BigDecimal,
    val percentage: Double
)

/**
 * Booking analytics for operational insights.
 */
data class BookingMetrics(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalBookings: Long,
    val completedBookings: Long,
    val cancelledBookings: Long,
    val noShowBookings: Long,
    val utilizationRate: Double, // Percentage of available slots booked
    val averageBookingDuration: Long, // Minutes
    val peakHours: List<PeakHour>,
    val popularCourts: List<CourtPopularity>,
    val bookingsByStatus: Map<String, Long>,
    val bookingsByDay: List<DailyBookingCount>
)

data class PeakHour(
    val hour: Int, // 0-23
    val bookingCount: Long,
    val utilizationPercentage: Double
)

data class CourtPopularity(
    val courtId: String,
    val courtName: String,
    val bookingCount: Long,
    val revenue: BigDecimal,
    val utilizationRate: Double
)

data class DailyBookingCount(
    val date: Instant,
    val bookingCount: Long,
    val utilizationRate: Double
)

/**
 * Membership analytics for retention and growth insights.
 */
data class MembershipMetrics(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalMembers: Long,
    val activeMembers: Long,
    val newMembers: Long,
    val churnedMembers: Long,
    val retentionRate: Double,
    val churnRate: Double,
    val membershipRevenue: BigDecimal,
    val averageLifetimeValue: BigDecimal,
    val membersByTier: Map<String, Long>,
    val memberGrowth: List<MembershipGrowth>
)

data class MembershipGrowth(
    val date: Instant,
    val totalMembers: Long,
    val newMembers: Long,
    val churnedMembers: Long,
    val netChange: Long
)

/**
 * Facility performance KPIs.
 */
data class FacilityPerformance(
    val periodStart: Instant,
    val periodEnd: Instant,
    val overallUtilization: Double,
    val revenue: BigDecimal,
    val totalBookings: Long,
    val activeMemberCount: Long,
    val customerSatisfactionScore: Double?,
    val repeatBookingRate: Double,
    val averageBookingValue: BigDecimal,
    val facilityCourts: Int,
    val operatingDays: Int,
    val topMetrics: List<KeyMetric>
)

data class KeyMetric(
    val name: String,
    val value: String,
    val trend: String?, // "up", "down", "stable"
    val changePercentage: Double?
)

/**
 * Customer insights for marketing and engagement.
 */
data class CustomerInsights(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalCustomers: Long,
    val newCustomers: Long,
    val returningCustomers: Long,
    val averageBookingsPerCustomer: Double,
    val averageRevenuePerCustomer: BigDecimal,
    val topCustomers: List<TopCustomer>,
    val customerSegments: Map<String, Long>,
    val engagementMetrics: EngagementMetrics
)

data class TopCustomer(
    val customerId: String,
    val customerName: String,
    val totalBookings: Long,
    val totalRevenue: BigDecimal,
    val lastBookingDate: Instant
)

data class EngagementMetrics(
    val emailOpenRate: Double?,
    val notificationClickRate: Double?,
    val appActiveUsers: Long?,
    val averageSessionDuration: Long? // Minutes
)

/**
 * Payment reconciliation data.
 */
data class PaymentReconciliation(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalProcessed: BigDecimal,
    val totalSettled: BigDecimal,
    val pendingSettlement: BigDecimal,
    val failedPayments: BigDecimal,
    val refundedAmount: BigDecimal,
    val fees: BigDecimal,
    val netAmount: BigDecimal,
    val transactionsByStatus: Map<String, Long>,
    val settlementDetails: List<SettlementDetail>
)

data class SettlementDetail(
    val date: Instant,
    val amount: BigDecimal,
    val transactionCount: Long,
    val provider: String
)
