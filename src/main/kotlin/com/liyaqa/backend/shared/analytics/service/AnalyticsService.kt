package com.liyaqa.backend.shared.analytics.service

import com.liyaqa.backend.facility.booking.data.BookingRepository
import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.payment.data.PaymentTransactionRepository
import com.liyaqa.backend.shared.analytics.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*

/**
 * Core analytics calculation service.
 *
 * Design Philosophy:
 * "You can't improve what you don't measure."
 *
 * This service transforms raw operational data into actionable insights.
 * Calculations are performed on-demand for real-time accuracy.
 * For frequently accessed metrics, consider caching strategies.
 */
@Service
class AnalyticsService(
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val paymentRepository: PaymentTransactionRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calculate revenue metrics for a period.
     */
    fun calculateRevenueMetrics(
        tenantId: String,
        startDate: Instant,
        endDate: Instant,
        branchId: UUID? = null
    ): RevenueMetrics {
        logger.info("Calculating revenue metrics: tenant={}, period={} to {}", tenantId, startDate, endDate)

        // Get all payments in period
        val payments = paymentRepository.findByTenantIdAndCreatedAtBetween(tenantId, startDate, endDate)

        val totalRevenue = payments
            .filter { it.status.name == "COMPLETED" || it.status.name == "CAPTURED" }
            .sumOf { it.amount }

        val refundTotal = payments
            .filter { it.status.name == "REFUNDED" || it.status.name == "PARTIALLY_REFUNDED" }
            .sumOf { it.refundAmount ?: BigDecimal.ZERO }

        val netRevenue = totalRevenue.subtract(refundTotal)

        val revenueByMethod = payments
            .filter { it.status.name == "COMPLETED" || it.status.name == "CAPTURED" }
            .groupBy { it.paymentMethod }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .mapKeys { (key, _) -> key ?: "Unknown" }

        val transactionCount = payments.count {
            it.status.name == "COMPLETED" || it.status.name == "CAPTURED"
        }.toLong()

        val avgTransactionValue = if (transactionCount > 0) {
            totalRevenue.divide(BigDecimal(transactionCount), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Daily revenue breakdown
        val dailyRevenue = payments
            .filter { it.status.name == "COMPLETED" || it.status.name == "CAPTURED" }
            .groupBy { it.createdAt?.toEpochMilli()?.div(86400000) } // Group by day
            .map { (_, txns) ->
                DailyRevenue(
                    date = txns.first().createdAt ?: Instant.now(),
                    revenue = txns.sumOf { it.amount },
                    transactionCount = txns.size.toLong()
                )
            }
            .sortedBy { it.date }

        // Revenue streams
        val topStreams = listOf(
            RevenueStream("bookings", totalRevenue, 100.0)
        )

        return RevenueMetrics(
            periodStart = startDate,
            periodEnd = endDate,
            totalRevenue = totalRevenue,
            revenueByPaymentMethod = revenueByMethod,
            refundTotal = refundTotal,
            netRevenue = netRevenue,
            averageTransactionValue = avgTransactionValue,
            transactionCount = transactionCount,
            revenueByDay = dailyRevenue,
            topRevenueStreams = topStreams
        )
    }

    /**
     * Calculate booking analytics for operational insights.
     */
    fun calculateBookingMetrics(
        tenantId: String,
        startDate: Instant,
        endDate: Instant,
        branchId: UUID? = null
    ): BookingMetrics {
        logger.info("Calculating booking metrics: tenant={}, period={} to {}", tenantId, startDate, endDate)

        // Simplified: get all bookings (would need to add repository method)
        val totalBookings = 0L
        val completedBookings = 0L
        val cancelledBookings = 0L
        val noShowBookings = 0L
        val utilizationRate = 0.0
        val avgDuration = 0L

        return BookingMetrics(
            periodStart = startDate,
            periodEnd = endDate,
            totalBookings = totalBookings,
            completedBookings = completedBookings,
            cancelledBookings = cancelledBookings,
            noShowBookings = noShowBookings,
            utilizationRate = utilizationRate,
            averageBookingDuration = avgDuration,
            peakHours = emptyList(),
            popularCourts = emptyList(),
            bookingsByStatus = emptyMap(),
            bookingsByDay = emptyList()
        )
    }

    /**
     * Calculate membership analytics.
     */
    fun calculateMembershipMetrics(
        tenantId: String,
        startDate: Instant,
        endDate: Instant,
        branchId: UUID? = null
    ): MembershipMetrics {
        logger.info("Calculating membership metrics: tenant={}, period={} to {}", tenantId, startDate, endDate)

        // Simplified implementation
        val totalMembers = 0L
        val activeMemberCount = 0L
        val newMemberCount = 0L
        val retentionRate = 0.0
        val churnRate = 0.0

        return MembershipMetrics(
            periodStart = startDate,
            periodEnd = endDate,
            totalMembers = totalMembers,
            activeMembers = activeMemberCount,
            newMembers = newMemberCount,
            churnedMembers = 0,
            retentionRate = retentionRate,
            churnRate = churnRate,
            membershipRevenue = BigDecimal.ZERO,
            averageLifetimeValue = BigDecimal.ZERO,
            membersByTier = emptyMap(),
            memberGrowth = emptyList()
        )
    }

    /**
     * Calculate overall facility performance KPIs.
     */
    fun calculateFacilityPerformance(
        tenantId: String,
        startDate: Instant,
        endDate: Instant,
        branchId: UUID? = null
    ): FacilityPerformance {
        logger.info("Calculating facility performance: tenant={}, period={} to {}", tenantId, startDate, endDate)

        val revenueMetrics = calculateRevenueMetrics(tenantId, startDate, endDate, branchId)
        val bookingMetrics = calculateBookingMetrics(tenantId, startDate, endDate, branchId)
        val membershipMetrics = calculateMembershipMetrics(tenantId, startDate, endDate, branchId)

        val topMetrics = listOf(
            KeyMetric("Total Revenue", revenueMetrics.totalRevenue.toString(), "up", 0.0),
            KeyMetric("Total Bookings", bookingMetrics.totalBookings.toString(), "up", 0.0),
            KeyMetric("Active Members", membershipMetrics.activeMembers.toString(), "stable", 0.0),
            KeyMetric("Utilization Rate", "${bookingMetrics.utilizationRate.toInt()}%", "up", 0.0)
        )

        return FacilityPerformance(
            periodStart = startDate,
            periodEnd = endDate,
            overallUtilization = bookingMetrics.utilizationRate,
            revenue = revenueMetrics.totalRevenue,
            totalBookings = bookingMetrics.totalBookings,
            activeMemberCount = membershipMetrics.activeMembers,
            customerSatisfactionScore = null,
            repeatBookingRate = 0.0,
            averageBookingValue = revenueMetrics.averageTransactionValue,
            facilityCourts = 0,
            operatingDays = 0,
            topMetrics = topMetrics
        )
    }
}
