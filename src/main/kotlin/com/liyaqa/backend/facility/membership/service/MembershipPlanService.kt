package com.liyaqa.backend.facility.membership.service

import com.liyaqa.backend.facility.membership.data.MembershipPlanRepository
import com.liyaqa.backend.facility.membership.domain.MembershipPlan
import com.liyaqa.backend.facility.membership.domain.MembershipPlanType
import com.liyaqa.backend.facility.membership.dto.MembershipPlanBasicResponse
import com.liyaqa.backend.facility.membership.dto.MembershipPlanCreateRequest
import com.liyaqa.backend.facility.membership.dto.MembershipPlanResponse
import com.liyaqa.backend.facility.membership.dto.MembershipPlanUpdateRequest
import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for managing membership plans.
 */
@Service
@Transactional
class MembershipPlanService(
    private val planRepository: MembershipPlanRepository,
    private val branchRepository: FacilityBranchRepository,
    private val facilityRepository: SportFacilityRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new membership plan.
     */
    fun createPlan(request: MembershipPlanCreateRequest): MembershipPlanResponse {
        // Validate branch exists
        val branch = branchRepository.findById(request.branchId)
            .orElseThrow { EntityNotFoundException("Branch not found: ${request.branchId}") }

        // Check for duplicate name
        if (planRepository.existsByBranchIdAndName(request.branchId, request.name)) {
            throw IllegalArgumentException("Plan name '${request.name}' already exists for this branch")
        }

        val plan = MembershipPlan(
            branch = branch,
            facility = branch.facility,
            name = request.name,
            description = request.description,
            planType = request.planType,
            price = request.price,
            currency = request.currency,
            billingCycle = request.billingCycle,
            durationMonths = request.durationMonths,
            features = request.features,
            maxBookingsPerMonth = request.maxBookingsPerMonth,
            maxConcurrentBookings = request.maxConcurrentBookings,
            advanceBookingDays = request.advanceBookingDays,
            cancellationHours = request.cancellationHours,
            guestPasses = request.guestPasses,
            hasCourtAccess = request.hasCourtAccess,
            hasClassAccess = request.hasClassAccess,
            hasGymAccess = request.hasGymAccess,
            hasLockerAccess = request.hasLockerAccess,
            priorityLevel = request.priorityLevel,
            setupFee = request.setupFee,
            discountPercentage = request.discountPercentage,
            isActive = request.isActive,
            isVisible = request.isVisible,
            maxMembers = request.maxMembers,
            termsAndConditions = request.termsAndConditions,
            autoRenew = request.autoRenew
        )

        plan.tenantId = branch.facility.tenantId

        val savedPlan = planRepository.save(plan)

        logger.info("Membership plan created: ${savedPlan.name} for branch ${branch.name}")

        return MembershipPlanResponse.from(savedPlan)
    }

    /**
     * Get plan by ID.
     */
    @Transactional(readOnly = true)
    fun getPlanById(id: UUID): MembershipPlanResponse {
        val plan = planRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership plan not found: $id") }

        return MembershipPlanResponse.from(plan)
    }

    /**
     * Update plan.
     */
    fun updatePlan(id: UUID, request: MembershipPlanUpdateRequest): MembershipPlanResponse {
        val plan = planRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership plan not found: $id") }

        request.name?.let {
            if (it != plan.name && planRepository.existsByBranchIdAndName(plan.branch.id!!, it)) {
                throw IllegalArgumentException("Plan name '$it' already exists for this branch")
            }
            plan.name = it
        }

        request.description?.let { plan.description = it }
        request.planType?.let { plan.planType = it }
        request.price?.let { plan.price = it }
        request.currency?.let { plan.currency = it }
        request.billingCycle?.let { plan.billingCycle = it }
        request.durationMonths?.let { plan.durationMonths = it }
        request.features?.let { plan.features = it }
        request.maxBookingsPerMonth?.let { plan.maxBookingsPerMonth = it }
        request.maxConcurrentBookings?.let { plan.maxConcurrentBookings = it }
        request.advanceBookingDays?.let { plan.advanceBookingDays = it }
        request.cancellationHours?.let { plan.cancellationHours = it }
        request.guestPasses?.let { plan.guestPasses = it }
        request.hasCourtAccess?.let { plan.hasCourtAccess = it }
        request.hasClassAccess?.let { plan.hasClassAccess = it }
        request.hasGymAccess?.let { plan.hasGymAccess = it }
        request.hasLockerAccess?.let { plan.hasLockerAccess = it }
        request.priorityLevel?.let { plan.priorityLevel = it }
        request.setupFee?.let { plan.setupFee = it }
        request.discountPercentage?.let { plan.discountPercentage = it }
        request.isActive?.let { plan.isActive = it }
        request.isVisible?.let { plan.isVisible = it }
        request.maxMembers?.let { plan.maxMembers = it }
        request.termsAndConditions?.let { plan.termsAndConditions = it }
        request.autoRenew?.let { plan.autoRenew = it }

        val savedPlan = planRepository.save(plan)

        logger.info("Membership plan updated: ${savedPlan.name}")

        return MembershipPlanResponse.from(savedPlan)
    }

    /**
     * Delete plan.
     */
    fun deletePlan(id: UUID) {
        val plan = planRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Membership plan not found: $id") }

        // TODO: Check if plan has active memberships

        planRepository.delete(plan)

        logger.warn("Membership plan deleted: ${plan.name}")
    }

    /**
     * Get plans by branch.
     */
    @Transactional(readOnly = true)
    fun getPlansByBranch(branchId: UUID): List<MembershipPlanResponse> {
        return planRepository.findByBranchId(branchId)
            .map { MembershipPlanResponse.from(it) }
    }

    /**
     * Get active plans by branch.
     */
    @Transactional(readOnly = true)
    fun getActivePlansByBranch(branchId: UUID): List<MembershipPlanResponse> {
        return planRepository.findActiveByBranchId(branchId)
            .map { MembershipPlanResponse.from(it) }
    }

    /**
     * Get visible plans by branch (for public display).
     */
    @Transactional(readOnly = true)
    fun getVisiblePlansByBranch(branchId: UUID): List<MembershipPlanBasicResponse> {
        return planRepository.findVisibleByBranchId(branchId)
            .map { MembershipPlanBasicResponse.from(it) }
    }

    /**
     * Get plans by facility.
     */
    @Transactional(readOnly = true)
    fun getPlansByFacility(facilityId: UUID): List<MembershipPlanResponse> {
        return planRepository.findByFacilityId(facilityId)
            .map { MembershipPlanResponse.from(it) }
    }

    /**
     * Get plans by type.
     */
    @Transactional(readOnly = true)
    fun getPlansByType(planType: MembershipPlanType): List<MembershipPlanResponse> {
        return planRepository.findByPlanType(planType)
            .map { MembershipPlanResponse.from(it) }
    }
}
