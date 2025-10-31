package com.liyaqa.backend.facility.membership.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.SportFacility
import com.liyaqa.backend.internal.employee.domain.Employee
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Tracks usage of discounts on memberships.
 * Records who used what discount, when, and how much they saved.
 */
@Entity
@Table(
    name = "discount_usages",
    indexes = [
        Index(name = "idx_usage_discount", columnList = "discount_id"),
        Index(name = "idx_usage_member", columnList = "member_id"),
        Index(name = "idx_usage_membership", columnList = "membership_id"),
        Index(name = "idx_usage_employee", columnList = "applied_by_employee_id"),
        Index(name = "idx_usage_facility", columnList = "facility_id"),
        Index(name = "idx_usage_date", columnList = "used_at"),
        Index(name = "idx_usage_tenant", columnList = "tenant_id")
    ]
)
class DiscountUsage(

    /**
     * The discount that was used.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    var discount: Discount,

    /**
     * The member who used the discount.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    /**
     * The membership this discount was applied to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    var membership: Membership,

    /**
     * Facility context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    /**
     * Original price before discount.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    var originalPrice: BigDecimal,

    /**
     * Discount amount applied.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    var discountAmount: BigDecimal,

    /**
     * Final price after discount.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    var finalPrice: BigDecimal,

    /**
     * When the discount was used.
     */
    @Column(nullable = false)
    var usedAt: Instant = Instant.now(),

    /**
     * Employee who applied the discount (if EMPLOYEE_APPLIED).
     * Null for customer-entered promo codes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by_employee_id")
    var appliedByEmployee: Employee? = null,

    /**
     * Notes about this discount application.
     */
    @Column(columnDefinition = "TEXT")
    var notes: String? = null

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Calculate savings percentage.
     */
    fun getSavingsPercentage(): BigDecimal {
        if (originalPrice == BigDecimal.ZERO) return BigDecimal.ZERO
        return discountAmount.multiply(BigDecimal(100)).divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
    }
}
