package com.liyaqa.backend.facility.membership.service

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.data.MembershipPlanRepository
import com.liyaqa.backend.facility.membership.data.MembershipRepository
import com.liyaqa.backend.facility.membership.data.DiscountRepository
import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.facility.membership.domain.MembershipStatus
import com.liyaqa.backend.facility.membership.dto.*
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Service for managing memberships (member-plan subscriptions).
 */
@Service
@Transactional
class MembershipService(
    private val membershipRepository: MembershipRepository,
    private val memberRepository: MemberRepository,
    private val planRepository: MembershipPlanRepository,
    private val discountRepository: DiscountRepository,
    private val discountService: DiscountService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new membership (subscribe member to plan).
     */
    fun createMembership(request: MembershipCreateRequest): MembershipResponse {
        // Validate member exists
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { EntityNotFoundException("Member not found: ${request.memberId}") }

        // Validate plan exists
        val plan = planRepository.findById(request.planId)
            .orElseThrow { EntityNotFoundException("Membership plan not found: ${request.planId}") }

        // Check if plan is available
        if (!plan.isAvailable()) {
            throw IllegalStateException("Membership plan '${plan.name}' is not available")
        }

        // Check if member already has active membership
        val existingMembership = membershipRepository.findActiveMembershipByMemberId(member.id!!)
        if (existingMembership != null) {
            throw IllegalStateException("Member already has an active membership")
        }

        // Calculate dates
        val startDate = request.startDate
        val endDate = startDate.plusMonths(plan.durationMonths.toLong())

        // Use provided price or plan's effective price
        var originalPrice = request.pricePaid ?: plan.getEffectivePrice()
        var finalPrice = originalPrice
        val setupFeePaid = request.setupFeePaid ?: plan.setupFee

        // Handle discount application
        var appliedDiscount: com.liyaqa.backend.facility.membership.domain.Discount? = null

        if (request.discountCode != null) {
            // Customer entered a promo code
            val discount = discountRepository.findValidDiscountByCode(request.discountCode, member.facility.id!!)
            if (discount != null) {
                // Validate discount for this member and plan
                val validation = discountService.validateDiscount(
                    ValidateDiscountRequest(
                        code = request.discountCode,
                        memberId = member.id!!,
                        planId = plan.id!!,
                        originalPrice = originalPrice
                    )
                )

                if (validation.isValid) {
                    finalPrice = validation.finalPrice
                    appliedDiscount = discount
                    logger.info("Discount code '${request.discountCode}' applied: ${validation.discountAmount} off")
                } else {
                    logger.warn("Discount code '${request.discountCode}' validation failed: ${validation.errorMessage}")
                }
            } else {
                logger.warn("Invalid discount code: ${request.discountCode}")
            }
        } else if (request.discountId != null) {
            // Employee applied a discount
            val discount = discountRepository.findById(request.discountId).orElse(null)
            if (discount != null && discount.isCurrentlyValid() && discount.isApplicableToPlan(plan)) {
                finalPrice = discount.calculateFinalPrice(originalPrice)
                appliedDiscount = discount
                logger.info("Employee discount '${discount.name}' applied by employee ${request.appliedByEmployeeId}")
            }
        }

        // Generate membership number
        val membershipNumber = generateMembershipNumber()

        val membership = Membership(
            member = member,
            plan = plan,
            branch = plan.branch,
            facility = plan.facility,
            membershipNumber = membershipNumber,
            startDate = startDate,
            endDate = endDate,
            pricePaid = finalPrice, // Use discounted price
            setupFeePaid = setupFeePaid,
            currency = plan.currency,
            paymentMethod = request.paymentMethod,
            paymentReference = request.paymentReference,
            paidAt = Instant.now(),
            autoRenew = request.autoRenew,
            nextBillingDate = if (request.autoRenew) endDate else null,
            notes = request.notes
        )

        membership.tenantId = plan.tenantId

        // Increment plan member count
        plan.incrementMemberCount()
        planRepository.save(plan)

        val savedMembership = membershipRepository.save(membership)

        // Record discount usage if discount was applied
        if (appliedDiscount != null) {
            discountService.recordDiscountUsage(
                discount = appliedDiscount,
                member = member,
                membership = savedMembership,
                originalPrice = originalPrice,
                appliedByEmployeeId = request.appliedByEmployeeId,
                notes = "Applied during membership creation"
            )
        }

        val logMessage = if (appliedDiscount != null) {
            "Membership created: ${membershipNumber} for member ${member.getFullName()} - Plan: ${plan.name} - Discount applied: ${appliedDiscount.name} (Original: $originalPrice, Final: $finalPrice)"
        } else {
            "Membership created: ${membershipNumber} for member ${member.getFullName()} - Plan: ${plan.name}"
        }
        logger.info(logMessage)

        return MembershipResponse.from(savedMembership)
    }

    /**
     * Get membership by ID.
     */
    @Transactional(readOnly = true)
    fun getMembershipById(id: UUID): MembershipResponse {
        val membership = membershipRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership not found: $id") }

        return MembershipResponse.from(membership)
    }

    /**
     * Get membership by membership number.
     */
    @Transactional(readOnly = true)
    fun getMembershipByNumber(membershipNumber: String): MembershipResponse {
        val membership = membershipRepository.findByMembershipNumber(membershipNumber)
            ?: throw EntityNotFoundException("Membership not found: $membershipNumber")

        return MembershipResponse.from(membership)
    }

    /**
     * Get memberships by member.
     */
    @Transactional(readOnly = true)
    fun getMembershipsByMember(memberId: UUID): List<MembershipResponse> {
        return membershipRepository.findByMemberId(memberId)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Get active membership for member.
     */
    @Transactional(readOnly = true)
    fun getActiveMembershipByMember(memberId: UUID): MembershipResponse? {
        return membershipRepository.findActiveMembershipByMemberId(memberId)
            ?.let { MembershipResponse.from(it) }
    }

    /**
     * Get memberships by plan.
     */
    @Transactional(readOnly = true)
    fun getMembershipsByPlan(planId: UUID): List<MembershipResponse> {
        return membershipRepository.findByPlanId(planId)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Get memberships by branch.
     */
    @Transactional(readOnly = true)
    fun getMembershipsByBranch(branchId: UUID): List<MembershipResponse> {
        return membershipRepository.findByBranchId(branchId)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Get active memberships by branch.
     */
    @Transactional(readOnly = true)
    fun getActiveMembershipsByBranch(branchId: UUID): List<MembershipResponse> {
        return membershipRepository.findActiveByBranchId(branchId)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Get memberships by facility.
     */
    @Transactional(readOnly = true)
    fun getMembershipsByFacility(facilityId: UUID): List<MembershipResponse> {
        return membershipRepository.findByFacilityId(facilityId)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Search memberships.
     */
    @Transactional(readOnly = true)
    fun searchMemberships(
        searchTerm: String?,
        status: MembershipStatus?,
        branchId: UUID?,
        facilityId: UUID?,
        pageable: Pageable
    ): Page<MembershipBasicResponse> {
        val page = membershipRepository.searchMemberships(searchTerm, status, branchId, facilityId, pageable)
        return page.map { MembershipBasicResponse.from(it) }
    }

    /**
     * Renew membership.
     */
    fun renewMembership(id: UUID, request: MembershipRenewRequest): MembershipResponse {
        val membership = membershipRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership not found: $id") }

        if (membership.status != MembershipStatus.EXPIRED && membership.status != MembershipStatus.ACTIVE) {
            throw IllegalStateException("Only active or expired memberships can be renewed")
        }

        // Calculate new end date
        val newEndDate = if (membership.isExpired()) {
            LocalDate.now().plusMonths(membership.plan.durationMonths.toLong())
        } else {
            membership.endDate.plusMonths(membership.plan.durationMonths.toLong())
        }

        membership.renew(newEndDate, request.pricePaid)
        membership.paymentMethod = request.paymentMethod
        membership.paymentReference = request.paymentReference

        request.autoRenew?.let {
            membership.autoRenew = it
            membership.nextBillingDate = if (it) newEndDate else null
        }

        val savedMembership = membershipRepository.save(membership)

        logger.info("Membership renewed: ${membership.membershipNumber} until $newEndDate")

        return MembershipResponse.from(savedMembership)
    }

    /**
     * Cancel membership.
     */
    fun cancelMembership(id: UUID, request: MembershipCancelRequest): MembershipResponse {
        val membership = membershipRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership not found: $id") }

        if (membership.status == MembershipStatus.CANCELLED) {
            throw IllegalStateException("Membership is already cancelled")
        }

        membership.cancel(request.reason, request.cancelledBy)

        // Decrement plan member count
        membership.plan.decrementMemberCount()
        planRepository.save(membership.plan)

        val savedMembership = membershipRepository.save(membership)

        logger.warn("Membership cancelled: ${membership.membershipNumber} - Reason: ${request.reason}")

        return MembershipResponse.from(savedMembership)
    }

    /**
     * Suspend membership.
     */
    fun suspendMembership(id: UUID, request: MembershipSuspendRequest): MembershipResponse {
        val membership = membershipRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership not found: $id") }

        if (membership.status == MembershipStatus.SUSPENDED) {
            throw IllegalStateException("Membership is already suspended")
        }

        membership.suspend(request.reason)
        val savedMembership = membershipRepository.save(membership)

        logger.warn("Membership suspended: ${membership.membershipNumber} - Reason: ${request.reason}")

        return MembershipResponse.from(savedMembership)
    }

    /**
     * Reactivate suspended membership.
     */
    fun reactivateMembership(id: UUID): MembershipResponse {
        val membership = membershipRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership not found: $id") }

        if (membership.status != MembershipStatus.SUSPENDED) {
            throw IllegalStateException("Only suspended memberships can be reactivated")
        }

        membership.reactivate()
        val savedMembership = membershipRepository.save(membership)

        logger.info("Membership reactivated: ${membership.membershipNumber}")

        return MembershipResponse.from(savedMembership)
    }

    /**
     * Get expiring memberships (within days).
     */
    @Transactional(readOnly = true)
    fun getExpiringMemberships(days: Int): List<MembershipResponse> {
        val expiryDate = LocalDate.now().plusDays(days.toLong())
        return membershipRepository.findExpiringBefore(expiryDate)
            .map { MembershipResponse.from(it) }
    }

    /**
     * Mark expired memberships as expired (background job).
     */
    fun processExpiredMemberships(): Int {
        val expiredMemberships = membershipRepository.findExpiredActive()
        var count = 0

        expiredMemberships.forEach { membership ->
            membership.markExpired()

            // Decrement plan member count
            membership.plan.decrementMemberCount()
            planRepository.save(membership.plan)

            membershipRepository.save(membership)
            count++
        }

        if (count > 0) {
            logger.info("Marked $count memberships as expired")
        }

        return count
    }

    /**
     * Record booking usage for membership.
     */
    fun recordBookingUsage(membershipId: UUID) {
        val membership = membershipRepository.findById(membershipId)
            .orElseThrow { EntityNotFoundException("Membership not found: $membershipId") }

        if (!membership.canMakeBooking()) {
            throw IllegalStateException("Membership cannot make more bookings")
        }

        membership.recordBooking()
        membershipRepository.save(membership)

        logger.info("Booking recorded for membership: ${membership.membershipNumber}")
    }

    /**
     * Generate unique membership number.
     */
    private fun generateMembershipNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "MEMBERSHIP-${timestamp}-${random}"
    }
}
