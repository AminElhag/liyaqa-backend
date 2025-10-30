package com.liyaqa.backend.facility.membership.dto

import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.facility.membership.domain.MembershipStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Request to create a new membership (subscribe a member to a plan).
 */
data class MembershipCreateRequest(
    @field:NotBlank(message = "Member ID is required")
    val memberId: UUID,

    @field:NotBlank(message = "Plan ID is required")
    val planId: UUID,

    val startDate: LocalDate = LocalDate.now(),

    @field:DecimalMin(value = "0.0", message = "Price must be non-negative")
    val pricePaid: BigDecimal? = null, // If null, uses plan's effective price

    val setupFeePaid: BigDecimal? = null,
    val paymentMethod: String? = null,
    val paymentReference: String? = null,

    val autoRenew: Boolean = false,
    val notes: String? = null
)

/**
 * Request to renew a membership.
 */
data class MembershipRenewRequest(
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    val pricePaid: BigDecimal,

    val paymentMethod: String? = null,
    val paymentReference: String? = null,
    val autoRenew: Boolean? = null
)

/**
 * Request to cancel a membership.
 */
data class MembershipCancelRequest(
    @field:NotBlank(message = "Cancellation reason is required")
    val reason: String,

    @field:NotBlank(message = "Cancelled by is required")
    val cancelledBy: String
)

/**
 * Request to suspend a membership.
 */
data class MembershipSuspendRequest(
    @field:NotBlank(message = "Suspension reason is required")
    val reason: String
)

/**
 * Response DTO for membership with full details.
 */
data class MembershipResponse(
    val id: UUID,
    val membershipNumber: String,

    val member: MemberBasicInfo,
    val plan: MembershipPlanBasicInfo,
    val branch: BranchBasicInfo,
    val facility: FacilityBasicInfo,

    val tenantId: String,

    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val isCurrentlyActive: Boolean,

    val status: MembershipStatus,
    val statusChangedAt: Instant?,
    val statusReason: String?,

    val pricePaid: BigDecimal,
    val setupFeePaid: BigDecimal?,
    val currency: String,
    val paymentMethod: String?,
    val paymentReference: String?,
    val paidAt: Instant?,

    val autoRenew: Boolean,
    val nextBillingDate: LocalDate?,
    val renewalReminderSent: Boolean,

    val bookingsUsed: Int,
    val maxBookingsPerMonth: Int?,
    val bookingsRemaining: Int?,
    val guestPassesUsed: Int,
    val guestPassesAvailable: Int?,
    val lastUsedAt: Instant?,

    val cancelledAt: Instant?,
    val cancelledBy: String?,
    val cancellationReason: String?,

    val notes: String?,

    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(membership: Membership): MembershipResponse {
            val today = LocalDate.now()
            val daysRemaining = if (today.isBefore(membership.endDate)) {
                java.time.temporal.ChronoUnit.DAYS.between(today, membership.endDate)
            } else {
                0L
            }

            val bookingsRemaining = membership.plan.maxBookingsPerMonth?.let {
                (it - membership.bookingsUsed).coerceAtLeast(0)
            }

            val guestPassesAvailable = membership.plan.guestPasses?.let {
                (it - membership.guestPassesUsed).coerceAtLeast(0)
            }

            return MembershipResponse(
                id = membership.id!!,
                membershipNumber = membership.membershipNumber,
                member = MemberBasicInfo(
                    id = membership.member.id!!,
                    fullName = membership.member.getFullName(),
                    email = membership.member.email,
                    memberNumber = membership.member.memberNumber
                ),
                plan = MembershipPlanBasicInfo(
                    id = membership.plan.id!!,
                    name = membership.plan.name,
                    planType = membership.plan.planType
                ),
                branch = BranchBasicInfo(
                    id = membership.branch.id!!,
                    name = membership.branch.name
                ),
                facility = FacilityBasicInfo(
                    id = membership.facility.id!!,
                    name = membership.facility.name
                ),
                tenantId = membership.tenantId,
                startDate = membership.startDate,
                endDate = membership.endDate,
                daysRemaining = daysRemaining,
                isExpired = membership.isExpired(),
                isCurrentlyActive = membership.isCurrentlyActive(),
                status = membership.status,
                statusChangedAt = membership.statusChangedAt,
                statusReason = membership.statusReason,
                pricePaid = membership.pricePaid,
                setupFeePaid = membership.setupFeePaid,
                currency = membership.currency,
                paymentMethod = membership.paymentMethod,
                paymentReference = membership.paymentReference,
                paidAt = membership.paidAt,
                autoRenew = membership.autoRenew,
                nextBillingDate = membership.nextBillingDate,
                renewalReminderSent = membership.renewalReminderSent,
                bookingsUsed = membership.bookingsUsed,
                maxBookingsPerMonth = membership.plan.maxBookingsPerMonth,
                bookingsRemaining = bookingsRemaining,
                guestPassesUsed = membership.guestPassesUsed,
                guestPassesAvailable = guestPassesAvailable,
                lastUsedAt = membership.lastUsedAt,
                cancelledAt = membership.cancelledAt,
                cancelledBy = membership.cancelledBy,
                cancellationReason = membership.cancellationReason,
                notes = membership.notes,
                createdAt = membership.createdAt,
                updatedAt = membership.updatedAt
            )
        }
    }
}

/**
 * Response DTO for membership with basic information (for lists).
 */
data class MembershipBasicResponse(
    val id: UUID,
    val membershipNumber: String,
    val memberName: String,
    val planName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: MembershipStatus,
    val isCurrentlyActive: Boolean
) {
    companion object {
        fun from(membership: Membership): MembershipBasicResponse {
            return MembershipBasicResponse(
                id = membership.id!!,
                membershipNumber = membership.membershipNumber,
                memberName = membership.member.getFullName(),
                planName = membership.plan.name,
                startDate = membership.startDate,
                endDate = membership.endDate,
                status = membership.status,
                isCurrentlyActive = membership.isCurrentlyActive()
            )
        }
    }
}

/**
 * Basic member information for nested responses.
 */
data class MemberBasicInfo(
    val id: UUID,
    val fullName: String,
    val email: String,
    val memberNumber: String?
)

/**
 * Basic membership plan information for nested responses.
 */
data class MembershipPlanBasicInfo(
    val id: UUID,
    val name: String,
    val planType: com.liyaqa.backend.facility.membership.domain.MembershipPlanType
)

/**
 * Basic branch information for nested responses.
 */
data class BranchBasicInfo(
    val id: UUID,
    val name: String
)

/**
 * Basic facility information for nested responses.
 */
data class FacilityBasicInfo(
    val id: UUID,
    val name: String
)
