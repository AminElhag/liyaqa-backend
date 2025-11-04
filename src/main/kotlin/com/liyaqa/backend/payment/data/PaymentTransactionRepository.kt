package com.liyaqa.backend.payment.data

import com.liyaqa.backend.payment.domain.PaymentTransaction
import com.liyaqa.backend.payment.domain.PaymentTransactionStatus
import com.liyaqa.backend.payment.gateway.PaymentGatewayType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for PaymentTransaction entity.
 */
@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, UUID> {

    /**
     * Find transaction by transaction number.
     */
    fun findByTransactionNumber(transactionNumber: String): PaymentTransaction?

    /**
     * Find transaction by gateway and gateway payment ID.
     */
    fun findByGatewayAndGatewayPaymentId(
        gateway: PaymentGatewayType,
        gatewayPaymentId: String
    ): PaymentTransaction?

    /**
     * Find transactions by booking.
     */
    @Query("""
        SELECT pt FROM PaymentTransaction pt
        WHERE pt.booking.id = :bookingId
        ORDER BY pt.createdAt DESC
    """)
    fun findByBookingId(@Param("bookingId") bookingId: UUID): List<PaymentTransaction>

    /**
     * Find transactions by member.
     */
    @Query("""
        SELECT pt FROM PaymentTransaction pt
        WHERE pt.member.id = :memberId
        ORDER BY pt.createdAt DESC
    """)
    fun findByMemberId(
        @Param("memberId") memberId: UUID,
        pageable: Pageable
    ): Page<PaymentTransaction>

    /**
     * Find transactions by status.
     */
    fun findByStatus(
        status: PaymentTransactionStatus,
        pageable: Pageable
    ): Page<PaymentTransaction>

    /**
     * Find failed transactions for retry.
     */
    @Query("""
        SELECT pt FROM PaymentTransaction pt
        WHERE pt.status = 'FAILED'
        AND pt.createdAt >= :since
        ORDER BY pt.createdAt DESC
    """)
    fun findFailedTransactionsSince(
        @Param("since") since: LocalDateTime
    ): List<PaymentTransaction>

    /**
     * Find pending transactions (for cleanup).
     */
    @Query("""
        SELECT pt FROM PaymentTransaction pt
        WHERE pt.status = 'PENDING'
        AND pt.createdAt < :before
    """)
    fun findPendingTransactionsBefore(
        @Param("before") before: LocalDateTime
    ): List<PaymentTransaction>

    /**
     * Get revenue by branch and date range.
     */
    @Query("""
        SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt
        WHERE pt.branch.id = :branchId
        AND pt.status = 'COMPLETED'
        AND DATE(pt.capturedAt) >= :startDate
        AND DATE(pt.capturedAt) <= :endDate
    """)
    fun getRevenueByBranchAndDateRange(
        @Param("branchId") branchId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal

    /**
     * Get total refunds by branch and date range.
     */
    @Query("""
        SELECT COALESCE(SUM(pt.refundAmount), 0) FROM PaymentTransaction pt
        WHERE pt.branch.id = :branchId
        AND pt.status IN ('PARTIALLY_REFUNDED', 'REFUNDED')
        AND DATE(pt.refundedAt) >= :startDate
        AND DATE(pt.refundedAt) <= :endDate
    """)
    fun getRefundsByBranchAndDateRange(
        @Param("branchId") branchId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal

    /**
     * Count transactions by status and date range.
     */
    @Query("""
        SELECT COUNT(pt) FROM PaymentTransaction pt
        WHERE pt.branch.id = :branchId
        AND pt.status = :status
        AND pt.createdAt >= :startDate
        AND pt.createdAt <= :endDate
    """)
    fun countByStatusAndDateRange(
        @Param("branchId") branchId: UUID,
        @Param("status") status: PaymentTransactionStatus,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    /**
     * Search transactions with filters.
     */
    @Query("""
        SELECT pt FROM PaymentTransaction pt
        WHERE (:branchId IS NULL OR pt.branch.id = :branchId)
        AND (:memberId IS NULL OR pt.member.id = :memberId)
        AND (:status IS NULL OR pt.status = :status)
        AND (:gateway IS NULL OR pt.gateway = :gateway)
        AND (:startDate IS NULL OR pt.createdAt >= :startDate)
        AND (:endDate IS NULL OR pt.createdAt <= :endDate)
        ORDER BY pt.createdAt DESC
    """)
    fun searchTransactions(
        @Param("branchId") branchId: UUID?,
        @Param("memberId") memberId: UUID?,
        @Param("status") status: PaymentTransactionStatus?,
        @Param("gateway") gateway: PaymentGatewayType?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<PaymentTransaction>
}
