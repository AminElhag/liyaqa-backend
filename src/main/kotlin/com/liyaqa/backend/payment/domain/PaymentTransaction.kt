package com.liyaqa.backend.payment.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import com.liyaqa.backend.payment.gateway.PaymentGatewayType
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant

/**
 * Payment transaction entity.
 *
 * Tracks all payment transactions across different payment gateways.
 * Provides a unified view of payments regardless of which gateway was used.
 *
 * Key features:
 * - Gateway-agnostic tracking
 * - Full audit trail
 * - Idempotency support
 * - Refund tracking
 * - Multi-tenancy
 */
@Entity
@Table(
    name = "payment_transactions",
    indexes = [
        Index(name = "idx_payment_booking", columnList = "booking_id"),
        Index(name = "idx_payment_member", columnList = "member_id"),
        Index(name = "idx_payment_gateway_ref", columnList = "gateway,gateway_payment_id"),
        Index(name = "idx_payment_status", columnList = "status"),
        Index(name = "idx_payment_tenant", columnList = "tenant_id"),
        Index(name = "idx_payment_created", columnList = "created_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payment_transaction_number", columnNames = ["transaction_number"]),
        UniqueConstraint(name = "uk_gateway_payment_id", columnNames = ["gateway", "gateway_payment_id"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class PaymentTransaction(
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    var booking: Booking? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    var membership: Membership? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // Transaction Details
    @Column(name = "transaction_number", length = 50, nullable = false, unique = true)
    var transactionNumber: String,

    @Column(name = "transaction_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var transactionType: PaymentTransactionType,

    // Gateway Information
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var gateway: PaymentGatewayType,

    @Column(name = "gateway_payment_id", length = 255, nullable = false)
    var gatewayPaymentId: String, // Stripe payment_intent_id, PayPal transaction_id, etc.

    @Column(name = "gateway_client_secret", length = 500)
    var gatewayClientSecret: String? = null,

    // Amount Information
    @Column(precision = 10, scale = 2, nullable = false)
    var amount: BigDecimal,

    @Column(length = 3, nullable = false)
    var currency: String,

    @Column(name = "original_amount", precision = 10, scale = 2)
    var originalAmount: BigDecimal? = null, // For refunds, track original payment amount

    // Status
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: PaymentTransactionStatus = PaymentTransactionStatus.PENDING,

    @Column(name = "status_message", columnDefinition = "TEXT")
    var statusMessage: String? = null,

    // Payment Method Details
    @Column(name = "payment_method", length = 50)
    var paymentMethod: String? = null, // card, bank_transfer, wallet

    @Column(name = "payment_brand", length = 50)
    var paymentBrand: String? = null, // visa, mastercard, amex

    @Column(name = "last_four", length = 4)
    var lastFour: String? = null,

    // Timing
    @Column(name = "authorized_at")
    var authorizedAt: Instant? = null,

    @Column(name = "captured_at")
    var capturedAt: Instant? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null,

    @Column(name = "refunded_at")
    var refundedAt: Instant? = null,

    // Refund Information
    @Column(name = "refund_amount", precision = 10, scale = 2)
    var refundAmount: BigDecimal? = null,

    @Column(name = "refund_reason", length = 100)
    var refundReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_transaction_id")
    var parentTransaction: PaymentTransaction? = null, // For refunds, link to original payment

    // Receipt and Records
    @Column(name = "receipt_url", length = 500)
    var receiptUrl: String? = null,

    @Column(name = "receipt_number", length = 100)
    var receiptNumber: String? = null,

    // Metadata
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "customer_email", length = 255)
    var customerEmail: String? = null,

    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String? = null, // JSON

    // Error Details (for failed transactions)
    @Column(name = "error_code", length = 100)
    var errorCode: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "decline_code", length = 100)
    var declineCode: String? = null,

    // Idempotency
    @Column(name = "idempotency_key", length = 255)
    var idempotencyKey: String? = null

) : BaseEntity() {

    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Mark transaction as authorized.
     */
    fun markAuthorized(gatewayPaymentId: String) {
        this.gatewayPaymentId = gatewayPaymentId
        this.status = PaymentTransactionStatus.AUTHORIZED
        this.authorizedAt = Instant.now()
    }

    /**
     * Mark transaction as captured/completed.
     */
    fun markCaptured(
        paymentMethod: String?,
        brand: String?,
        last4: String?,
        receiptUrl: String?
    ) {
        this.status = PaymentTransactionStatus.COMPLETED
        this.capturedAt = Instant.now()
        this.paymentMethod = paymentMethod
        this.paymentBrand = brand
        this.lastFour = last4
        this.receiptUrl = receiptUrl
    }

    /**
     * Mark transaction as failed.
     */
    fun markFailed(errorCode: String?, errorMessage: String?, declineCode: String?) {
        this.status = PaymentTransactionStatus.FAILED
        this.failedAt = Instant.now()
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.declineCode = declineCode
    }

    /**
     * Mark transaction as refunded.
     */
    fun markRefunded(refundAmount: BigDecimal, reason: String?) {
        this.refundAmount = refundAmount
        this.refundReason = reason
        this.refundedAt = Instant.now()

        // Check if fully or partially refunded
        if (refundAmount >= amount) {
            this.status = PaymentTransactionStatus.REFUNDED
        } else {
            this.status = PaymentTransactionStatus.PARTIALLY_REFUNDED
        }
    }

    /**
     * Check if transaction can be refunded.
     */
    fun canBeRefunded(): Boolean {
        return status == PaymentTransactionStatus.COMPLETED &&
                (refundAmount == null || refundAmount!! < amount)
    }

    /**
     * Get remaining refundable amount.
     */
    fun getRemainingRefundableAmount(): BigDecimal {
        val alreadyRefunded = refundAmount ?: BigDecimal.ZERO
        return amount.subtract(alreadyRefunded)
    }

    /**
     * Check if this is a refund transaction.
     */
    fun isRefund(): Boolean {
        return transactionType == PaymentTransactionType.REFUND
    }

    override fun toString(): String {
        return "PaymentTransaction(number='$transactionNumber', gateway=$gateway, amount=$amount $currency, status=$status)"
    }
}

/**
 * Payment transaction type.
 */
enum class PaymentTransactionType {
    PAYMENT,        // Regular payment
    REFUND,         // Refund of a previous payment
    AUTHORIZATION,  // Authorization only (not captured yet)
    VOID            // Voided authorization
}

/**
 * Payment transaction status.
 */
enum class PaymentTransactionStatus {
    PENDING,                // Created but not yet processed
    AUTHORIZED,             // Authorized but not captured
    PROCESSING,             // Being processed by gateway
    COMPLETED,              // Successfully completed
    PARTIALLY_REFUNDED,     // Some amount refunded
    REFUNDED,               // Fully refunded
    FAILED,                 // Failed
    CANCELED,               // Canceled before completion
    EXPIRED                 // Authorization expired
}
