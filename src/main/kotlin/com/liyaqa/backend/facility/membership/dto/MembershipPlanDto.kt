package com.liyaqa.backend.facility.membership.dto

import com.liyaqa.backend.facility.membership.domain.BillingCycle
import com.liyaqa.backend.facility.membership.domain.MembershipPlan
import com.liyaqa.backend.facility.membership.domain.MembershipPlanType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Request to create a membership plan.
 */
data class MembershipPlanCreateRequest(
    @field:NotBlank(message = "Branch ID is required")
    val branchId: UUID,

    @field:NotBlank(message = "Plan name is required")
    @field:Size(max = 100, message = "Plan name must not exceed 100 characters")
    val name: String,

    val description: String? = null,

    val planType: MembershipPlanType = MembershipPlanType.INDIVIDUAL,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    val price: BigDecimal,

    val currency: String = "USD",

    val billingCycle: BillingCycle = BillingCycle.MONTHLY,

    @field:Min(value = 1, message = "Duration must be at least 1 month")
    val durationMonths: Int,

    val features: String? = null,
    val maxBookingsPerMonth: Int? = null,
    val maxConcurrentBookings: Int? = null,
    val advanceBookingDays: Int? = null,
    val cancellationHours: Int? = null,
    val guestPasses: Int? = null,

    val hasCourtAccess: Boolean = true,
    val hasClassAccess: Boolean = false,
    val hasGymAccess: Boolean = false,
    val hasLockerAccess: Boolean = false,
    val priorityLevel: Int = 0,

    val setupFee: BigDecimal? = null,
    val discountPercentage: BigDecimal? = null,

    val isActive: Boolean = true,
    val isVisible: Boolean = true,
    val maxMembers: Int? = null,

    val termsAndConditions: String? = null,
    val autoRenew: Boolean = true
)

/**
 * Request to update membership plan.
 */
data class MembershipPlanUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val planType: MembershipPlanType? = null,
    val price: BigDecimal? = null,
    val currency: String? = null,
    val billingCycle: BillingCycle? = null,
    val durationMonths: Int? = null,
    val features: String? = null,
    val maxBookingsPerMonth: Int? = null,
    val maxConcurrentBookings: Int? = null,
    val advanceBookingDays: Int? = null,
    val cancellationHours: Int? = null,
    val guestPasses: Int? = null,
    val hasCourtAccess: Boolean? = null,
    val hasClassAccess: Boolean? = null,
    val hasGymAccess: Boolean? = null,
    val hasLockerAccess: Boolean? = null,
    val priorityLevel: Int? = null,
    val setupFee: BigDecimal? = null,
    val discountPercentage: BigDecimal? = null,
    val isActive: Boolean? = null,
    val isVisible: Boolean? = null,
    val maxMembers: Int? = null,
    val termsAndConditions: String? = null,
    val autoRenew: Boolean? = null
)

/**
 * Response DTO for membership plan with full details.
 */
data class MembershipPlanResponse(
    val id: UUID,
    val branchId: UUID,
    val branchName: String,
    val facilityId: UUID,
    val facilityName: String,
    val tenantId: String,

    val name: String,
    val description: String?,
    val planType: MembershipPlanType,

    val price: BigDecimal,
    val effectivePrice: BigDecimal,
    val currency: String,
    val billingCycle: BillingCycle,
    val durationMonths: Int,

    val features: String?,
    val maxBookingsPerMonth: Int?,
    val maxConcurrentBookings: Int?,
    val advanceBookingDays: Int?,
    val cancellationHours: Int?,
    val guestPasses: Int?,

    val hasCourtAccess: Boolean,
    val hasClassAccess: Boolean,
    val hasGymAccess: Boolean,
    val hasLockerAccess: Boolean,
    val priorityLevel: Int,

    val setupFee: BigDecimal?,
    val discountPercentage: BigDecimal?,
    val firstPaymentAmount: BigDecimal,

    val isActive: Boolean,
    val isVisible: Boolean,
    val isAvailable: Boolean,
    val maxMembers: Int?,
    val currentMembers: Int,
    val isAtCapacity: Boolean,

    val termsAndConditions: String?,
    val autoRenew: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(plan: MembershipPlan): MembershipPlanResponse {
            return MembershipPlanResponse(
                id = plan.id!!,
                branchId = plan.branch.id!!,
                branchName = plan.branch.name,
                facilityId = plan.facility.id!!,
                facilityName = plan.facility.name,
                tenantId = plan.tenantId,
                name = plan.name,
                description = plan.description,
                planType = plan.planType,
                price = plan.price,
                effectivePrice = plan.getEffectivePrice(),
                currency = plan.currency,
                billingCycle = plan.billingCycle,
                durationMonths = plan.durationMonths,
                features = plan.features,
                maxBookingsPerMonth = plan.maxBookingsPerMonth,
                maxConcurrentBookings = plan.maxConcurrentBookings,
                advanceBookingDays = plan.advanceBookingDays,
                cancellationHours = plan.cancellationHours,
                guestPasses = plan.guestPasses,
                hasCourtAccess = plan.hasCourtAccess,
                hasClassAccess = plan.hasClassAccess,
                hasGymAccess = plan.hasGymAccess,
                hasLockerAccess = plan.hasLockerAccess,
                priorityLevel = plan.priorityLevel,
                setupFee = plan.setupFee,
                discountPercentage = plan.discountPercentage,
                firstPaymentAmount = plan.getFirstPaymentAmount(),
                isActive = plan.isActive,
                isVisible = plan.isVisible,
                isAvailable = plan.isAvailable(),
                maxMembers = plan.maxMembers,
                currentMembers = plan.currentMembers,
                isAtCapacity = plan.isAtCapacity(),
                termsAndConditions = plan.termsAndConditions,
                autoRenew = plan.autoRenew,
                createdAt = plan.createdAt,
                updatedAt = plan.updatedAt
            )
        }
    }
}

/**
 * Response DTO for membership plan with basic information (for lists).
 */
data class MembershipPlanBasicResponse(
    val id: UUID,
    val name: String,
    val planType: MembershipPlanType,
    val price: BigDecimal,
    val effectivePrice: BigDecimal,
    val currency: String,
    val billingCycle: BillingCycle,
    val durationMonths: Int,
    val isActive: Boolean,
    val isAvailable: Boolean,
    val currentMembers: Int
) {
    companion object {
        fun from(plan: MembershipPlan): MembershipPlanBasicResponse {
            return MembershipPlanBasicResponse(
                id = plan.id!!,
                name = plan.name,
                planType = plan.planType,
                price = plan.price,
                effectivePrice = plan.getEffectivePrice(),
                currency = plan.currency,
                billingCycle = plan.billingCycle,
                durationMonths = plan.durationMonths,
                isActive = plan.isActive,
                isAvailable = plan.isAvailable(),
                currentMembers = plan.currentMembers
            )
        }
    }
}
