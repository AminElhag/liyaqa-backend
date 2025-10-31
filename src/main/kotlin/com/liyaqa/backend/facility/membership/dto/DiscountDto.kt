package com.liyaqa.backend.facility.membership.dto

import com.liyaqa.backend.facility.membership.domain.*
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Request to create a new discount.
 */
data class DiscountCreateRequest(
    @field:Size(max = 50, message = "Code must not exceed 50 characters")
    @field:Pattern(regexp = "^[A-Z0-9_-]*$", message = "Code must contain only uppercase letters, numbers, underscores, and hyphens")
    val code: String? = null,

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 200, message = "Name must not exceed 200 characters")
    val name: String,

    @field:Size(max = 2000, message = "Description must not exceed 2000 characters")
    val description: String? = null,

    @field:NotNull(message = "Discount type is required")
    val discountType: DiscountType,

    @field:NotNull(message = "Value is required")
    @field:DecimalMin(value = "0.01", message = "Value must be greater than 0")
    @field:DecimalMax(value = "100.00", message = "For percentage discounts, value must not exceed 100")
    val value: BigDecimal,

    @field:Size(max = 3, message = "Currency code must be 3 characters")
    val currency: String? = null,

    @field:NotNull(message = "Application method is required")
    val applicationMethod: DiscountApplicationMethod,

    @field:NotNull(message = "Scope is required")
    val scope: DiscountScope = DiscountScope.ALL_PLANS,

    @field:NotNull(message = "Facility ID is required")
    val facilityId: UUID,

    val branchId: UUID? = null,

    @field:NotNull(message = "Valid from date is required")
    val validFrom: LocalDate,

    @field:NotNull(message = "Valid until date is required")
    val validUntil: LocalDate,

    val isActive: Boolean = true,

    @field:Min(value = 1, message = "Max total usage must be at least 1")
    val maxTotalUsage: Int? = null,

    @field:Min(value = 1, message = "Max usage per member must be at least 1")
    val maxUsagePerMember: Int? = null,

    @field:DecimalMin(value = "0.00", message = "Minimum purchase amount must be non-negative")
    val minPurchaseAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.01", message = "Maximum discount amount must be positive")
    val maxDiscountAmount: BigDecimal? = null,

    val applicablePlanIds: Set<UUID> = emptySet(),

    val applicableTypes: Set<MembershipPlanType> = emptySet(),

    @field:Size(max = 2000, message = "Internal notes must not exceed 2000 characters")
    val internalNotes: String? = null
)

/**
 * Request to update an existing discount.
 */
data class DiscountUpdateRequest(
    @field:Size(max = 200, message = "Name must not exceed 200 characters")
    val name: String? = null,

    @field:Size(max = 2000, message = "Description must not exceed 2000 characters")
    val description: String? = null,

    @field:DecimalMin(value = "0.01", message = "Value must be greater than 0")
    val value: BigDecimal? = null,

    val validFrom: LocalDate? = null,

    val validUntil: LocalDate? = null,

    val isActive: Boolean? = null,

    @field:Min(value = 1, message = "Max total usage must be at least 1")
    val maxTotalUsage: Int? = null,

    @field:Min(value = 1, message = "Max usage per member must be at least 1")
    val maxUsagePerMember: Int? = null,

    @field:DecimalMin(value = "0.00", message = "Minimum purchase amount must be non-negative")
    val minPurchaseAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.01", message = "Maximum discount amount must be positive")
    val maxDiscountAmount: BigDecimal? = null,

    val applicablePlanIds: Set<UUID>? = null,

    val applicableTypes: Set<MembershipPlanType>? = null,

    @field:Size(max = 2000, message = "Internal notes must not exceed 2000 characters")
    val internalNotes: String? = null
)

/**
 * Response containing full discount details.
 */
data class DiscountResponse(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val discountType: DiscountType,
    val value: BigDecimal,
    val currency: String?,
    val applicationMethod: DiscountApplicationMethod,
    val scope: DiscountScope,
    val facilityId: UUID,
    val facilityName: String,
    val branchId: UUID?,
    val branchName: String?,
    val validFrom: LocalDate,
    val validUntil: LocalDate,
    val isActive: Boolean,
    val isCurrentlyValid: Boolean,
    val maxTotalUsage: Int?,
    val maxUsagePerMember: Int?,
    val currentUsageCount: Int,
    val hasReachedUsageLimit: Boolean,
    val minPurchaseAmount: BigDecimal?,
    val maxDiscountAmount: BigDecimal?,
    val applicablePlans: List<MembershipPlanBasicInfo>,
    val applicableTypes: Set<MembershipPlanType>,
    val internalNotes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(discount: Discount): DiscountResponse {
            return DiscountResponse(
                id = discount.id!!,
                code = discount.code,
                name = discount.name,
                description = discount.description,
                discountType = discount.discountType,
                value = discount.value,
                currency = discount.currency,
                applicationMethod = discount.applicationMethod,
                scope = discount.scope,
                facilityId = discount.facility.id!!,
                facilityName = discount.facility.name,
                branchId = discount.branch?.id,
                branchName = discount.branch?.name,
                validFrom = discount.validFrom,
                validUntil = discount.validUntil,
                isActive = discount.isActive,
                isCurrentlyValid = discount.isCurrentlyValid(),
                maxTotalUsage = discount.maxTotalUsage,
                maxUsagePerMember = discount.maxUsagePerMember,
                currentUsageCount = discount.currentUsageCount,
                hasReachedUsageLimit = discount.hasReachedUsageLimit(),
                minPurchaseAmount = discount.minPurchaseAmount,
                maxDiscountAmount = discount.maxDiscountAmount,
                applicablePlans = discount.applicablePlans.map { MembershipPlanBasicInfo.from(it) },
                applicableTypes = discount.applicableTypes,
                internalNotes = discount.internalNotes,
                createdAt = discount.createdAt!!,
                updatedAt = discount.updatedAt!!
            )
        }
    }

    /**
     * Basic plan info for discount response.
     */
    data class MembershipPlanBasicInfo(
        val id: UUID,
        val name: String,
        val planType: MembershipPlanType
    ) {
        companion object {
            fun from(plan: MembershipPlan): MembershipPlanBasicInfo {
                return MembershipPlanBasicInfo(
                    id = plan.id!!,
                    name = plan.name,
                    planType = plan.planType
                )
            }
        }
    }
}

/**
 * Basic discount information.
 */
data class DiscountBasicResponse(
    val id: UUID,
    val code: String?,
    val name: String,
    val discountType: DiscountType,
    val value: BigDecimal,
    val applicationMethod: DiscountApplicationMethod,
    val validFrom: LocalDate,
    val validUntil: LocalDate,
    val isActive: Boolean,
    val isCurrentlyValid: Boolean,
    val currentUsageCount: Int,
    val maxTotalUsage: Int?
) {
    companion object {
        fun from(discount: Discount): DiscountBasicResponse {
            return DiscountBasicResponse(
                id = discount.id!!,
                code = discount.code,
                name = discount.name,
                discountType = discount.discountType,
                value = discount.value,
                applicationMethod = discount.applicationMethod,
                validFrom = discount.validFrom,
                validUntil = discount.validUntil,
                isActive = discount.isActive,
                isCurrentlyValid = discount.isCurrentlyValid(),
                currentUsageCount = discount.currentUsageCount,
                maxTotalUsage = discount.maxTotalUsage
            )
        }
    }
}

/**
 * Request to validate and apply a discount code.
 */
data class ValidateDiscountRequest(
    @field:NotBlank(message = "Discount code is required")
    val code: String,

    @field:NotNull(message = "Member ID is required")
    val memberId: UUID,

    @field:NotNull(message = "Plan ID is required")
    val planId: UUID,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.01", message = "Price must be positive")
    val originalPrice: BigDecimal
)

/**
 * Response with discount validation result and calculation.
 */
data class DiscountValidationResponse(
    val isValid: Boolean,
    val discount: DiscountBasicResponse?,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val finalPrice: BigDecimal,
    val savingsPercentage: BigDecimal,
    val errorMessage: String? = null
)

/**
 * Request to apply discount (by employee or code).
 */
data class ApplyDiscountRequest(
    val discountCode: String? = null,
    val discountId: UUID? = null,
    val appliedByEmployeeId: UUID? = null,
    val notes: String? = null
)

/**
 * Discount usage response.
 */
data class DiscountUsageResponse(
    val id: UUID,
    val discount: DiscountBasicInfo,
    val member: MemberBasicInfo,
    val membership: MembershipBasicInfo,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val finalPrice: BigDecimal,
    val savingsPercentage: BigDecimal,
    val usedAt: Instant,
    val appliedByEmployee: EmployeeBasicInfo?,
    val notes: String?
) {
    companion object {
        fun from(usage: DiscountUsage): DiscountUsageResponse {
            return DiscountUsageResponse(
                id = usage.id!!,
                discount = DiscountBasicInfo.from(usage.discount),
                member = MemberBasicInfo.from(usage.member),
                membership = MembershipBasicInfo.from(usage.membership),
                originalPrice = usage.originalPrice,
                discountAmount = usage.discountAmount,
                finalPrice = usage.finalPrice,
                savingsPercentage = usage.getSavingsPercentage(),
                usedAt = usage.usedAt,
                appliedByEmployee = usage.appliedByEmployee?.let { EmployeeBasicInfo.from(it) },
                notes = usage.notes
            )
        }
    }

    data class DiscountBasicInfo(
        val id: UUID,
        val code: String?,
        val name: String,
        val discountType: DiscountType,
        val value: BigDecimal
    ) {
        companion object {
            fun from(discount: Discount): DiscountBasicInfo {
                return DiscountBasicInfo(
                    id = discount.id!!,
                    code = discount.code,
                    name = discount.name,
                    discountType = discount.discountType,
                    value = discount.value
                )
            }
        }
    }

    data class MemberBasicInfo(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val email: String
    ) {
        companion object {
            fun from(member: Member): MemberBasicInfo {
                return MemberBasicInfo(
                    id = member.id!!,
                    firstName = member.firstName,
                    lastName = member.lastName,
                    email = member.email
                )
            }
        }
    }

    data class MembershipBasicInfo(
        val id: UUID,
        val membershipNumber: String
    ) {
        companion object {
            fun from(membership: Membership): MembershipBasicInfo {
                return MembershipBasicInfo(
                    id = membership.id!!,
                    membershipNumber = membership.membershipNumber
                )
            }
        }
    }

    data class EmployeeBasicInfo(
        val id: UUID,
        val firstName: String,
        val lastName: String
    ) {
        companion object {
            fun from(employee: com.liyaqa.backend.internal.employee.domain.Employee): EmployeeBasicInfo {
                return EmployeeBasicInfo(
                    id = employee.id!!,
                    firstName = employee.firstName,
                    lastName = employee.lastName
                )
            }
        }
    }
}
