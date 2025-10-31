package com.liyaqa.backend.facility.membership.service

import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import com.liyaqa.backend.facility.membership.data.*
import com.liyaqa.backend.facility.membership.domain.*
import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service for managing discounts and discount validation/application.
 */
@Service
@Transactional
class DiscountService(
    private val discountRepository: DiscountRepository,
    private val discountUsageRepository: DiscountUsageRepository,
    private val memberRepository: MemberRepository,
    private val membershipPlanRepository: MembershipPlanRepository,
    private val facilityRepository: SportFacilityRepository,
    private val branchRepository: FacilityBranchRepository,
    private val employeeRepository: EmployeeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new discount.
     */
    fun createDiscount(request: DiscountCreateRequest): DiscountResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Validate branch if provided
        val branch = request.branchId?.let {
            branchRepository.findById(it)
                .orElseThrow { EntityNotFoundException("Branch not found: $it") }
        }

        // Validate discount code uniqueness if provided
        if (request.code != null && discountRepository.existsByCodeAndFacilityId(request.code, request.facilityId)) {
            throw IllegalArgumentException("Discount code '${request.code}' already exists for this facility")
        }

        // Validate dates
        if (request.validUntil.isBefore(request.validFrom)) {
            throw IllegalArgumentException("Valid until date must be after valid from date")
        }

        // Validate percentage value
        if (request.discountType == DiscountType.PERCENTAGE && request.value > BigDecimal(100)) {
            throw IllegalArgumentException("Percentage discount cannot exceed 100%")
        }

        // Validate currency for fixed amount discounts
        if (request.discountType == DiscountType.FIXED_AMOUNT && request.currency.isNullOrBlank()) {
            throw IllegalArgumentException("Currency is required for fixed amount discounts")
        }

        // Validate scope-specific requirements
        val applicablePlans = when (request.scope) {
            DiscountScope.SPECIFIC_PLANS -> {
                if (request.applicablePlanIds.isEmpty()) {
                    throw IllegalArgumentException("Applicable plan IDs are required for SPECIFIC_PLANS scope")
                }
                request.applicablePlanIds.mapNotNull {
                    membershipPlanRepository.findById(it).orElse(null)
                }.toMutableSet()
            }
            DiscountScope.SPECIFIC_TYPES -> {
                if (request.applicableTypes.isEmpty()) {
                    throw IllegalArgumentException("Applicable types are required for SPECIFIC_TYPES scope")
                }
                mutableSetOf()
            }
            else -> mutableSetOf()
        }

        val discount = Discount(
            code = request.code?.uppercase(),
            name = request.name,
            description = request.description,
            discountType = request.discountType,
            value = request.value,
            currency = request.currency,
            applicationMethod = request.applicationMethod,
            scope = request.scope,
            facility = facility,
            branch = branch,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            isActive = request.isActive,
            maxTotalUsage = request.maxTotalUsage,
            maxUsagePerMember = request.maxUsagePerMember,
            minPurchaseAmount = request.minPurchaseAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            internalNotes = request.internalNotes
        )

        discount.tenantId = facility.tenantId
        discount.applicablePlans = applicablePlans

        if (request.scope == DiscountScope.SPECIFIC_TYPES) {
            discount.applicableTypes = request.applicableTypes.toMutableSet()
        }

        val savedDiscount = discountRepository.save(discount)

        logger.info("Discount created: ${savedDiscount.name} (${savedDiscount.code ?: "employee-applied"})")

        return DiscountResponse.from(savedDiscount)
    }

    /**
     * Get discount by ID.
     */
    @Transactional(readOnly = true)
    fun getDiscountById(id: UUID): DiscountResponse {
        val discount = discountRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Discount not found: $id") }

        return DiscountResponse.from(discount)
    }

    /**
     * Update discount.
     */
    fun updateDiscount(id: UUID, request: DiscountUpdateRequest): DiscountResponse {
        val discount = discountRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Discount not found: $id") }

        request.name?.let { discount.name = it }
        request.description?.let { discount.description = it }
        request.value?.let {
            if (discount.discountType == DiscountType.PERCENTAGE && it > BigDecimal(100)) {
                throw IllegalArgumentException("Percentage discount cannot exceed 100%")
            }
            discount.value = it
        }
        request.validFrom?.let { discount.validFrom = it }
        request.validUntil?.let { discount.validUntil = it }
        request.isActive?.let { discount.isActive = it }
        request.maxTotalUsage?.let { discount.maxTotalUsage = it }
        request.maxUsagePerMember?.let { discount.maxUsagePerMember = it }
        request.minPurchaseAmount?.let { discount.minPurchaseAmount = it }
        request.maxDiscountAmount?.let { discount.maxDiscountAmount = it }
        request.internalNotes?.let { discount.internalNotes = it }

        request.applicablePlanIds?.let { planIds ->
            val plans = planIds.mapNotNull {
                membershipPlanRepository.findById(it).orElse(null)
            }.toMutableSet()
            discount.applicablePlans = plans
        }

        request.applicableTypes?.let {
            discount.applicableTypes = it.toMutableSet()
        }

        // Validate dates
        if (discount.validUntil.isBefore(discount.validFrom)) {
            throw IllegalArgumentException("Valid until date must be after valid from date")
        }

        val savedDiscount = discountRepository.save(discount)

        logger.info("Discount updated: ${savedDiscount.name}")

        return DiscountResponse.from(savedDiscount)
    }

    /**
     * Delete discount.
     */
    fun deleteDiscount(id: UUID) {
        val discount = discountRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Discount not found: $id") }

        // Check if discount has been used
        val usageCount = discountUsageRepository.countByDiscountId(id)
        if (usageCount > 0) {
            throw IllegalStateException("Cannot delete discount that has been used. Deactivate it instead.")
        }

        discountRepository.delete(discount)

        logger.info("Discount deleted: ${discount.name}")
    }

    /**
     * Get all discounts for a facility.
     */
    @Transactional(readOnly = true)
    fun getDiscountsByFacility(facilityId: UUID): List<DiscountResponse> {
        return discountRepository.findByFacilityId(facilityId)
            .map { DiscountResponse.from(it) }
    }

    /**
     * Get active discounts for a facility.
     */
    @Transactional(readOnly = true)
    fun getActiveDiscounts(facilityId: UUID): List<DiscountBasicResponse> {
        return discountRepository.findByFacilityIdAndIsActive(facilityId, true)
            .map { DiscountBasicResponse.from(it) }
    }

    /**
     * Get currently valid discounts (active and within date range).
     */
    @Transactional(readOnly = true)
    fun getCurrentlyValidDiscounts(facilityId: UUID): List<DiscountBasicResponse> {
        return discountRepository.findCurrentlyValidDiscounts(facilityId)
            .map { DiscountBasicResponse.from(it) }
    }

    /**
     * Validate a discount code and calculate the discount.
     */
    @Transactional(readOnly = true)
    fun validateDiscount(request: ValidateDiscountRequest): DiscountValidationResponse {
        // Find discount by code
        val discount = discountRepository.findByCodeAndFacilityId(
            request.code.uppercase(),
            // We need to get facility ID from member
            memberRepository.findById(request.memberId)
                .orElseThrow { EntityNotFoundException("Member not found: ${request.memberId}") }
                .facility.id!!
        )

        if (discount == null) {
            return DiscountValidationResponse(
                isValid = false,
                discount = null,
                originalPrice = request.originalPrice,
                discountAmount = BigDecimal.ZERO,
                finalPrice = request.originalPrice,
                savingsPercentage = BigDecimal.ZERO,
                errorMessage = "Invalid discount code"
            )
        }

        // Validate discount
        val validationResult = validateDiscountForMember(discount, request.memberId, request.planId, request.originalPrice)

        if (!validationResult.isValid) {
            return DiscountValidationResponse(
                isValid = false,
                discount = DiscountBasicResponse.from(discount),
                originalPrice = request.originalPrice,
                discountAmount = BigDecimal.ZERO,
                finalPrice = request.originalPrice,
                savingsPercentage = BigDecimal.ZERO,
                errorMessage = validationResult.errorMessage
            )
        }

        // Calculate discount
        val discountAmount = discount.calculateDiscountAmount(request.originalPrice)
        val finalPrice = discount.calculateFinalPrice(request.originalPrice)
        val savingsPercentage = if (request.originalPrice > BigDecimal.ZERO) {
            discountAmount.multiply(BigDecimal(100)).divide(request.originalPrice, 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return DiscountValidationResponse(
            isValid = true,
            discount = DiscountBasicResponse.from(discount),
            originalPrice = request.originalPrice,
            discountAmount = discountAmount,
            finalPrice = finalPrice,
            savingsPercentage = savingsPercentage
        )
    }

    /**
     * Record discount usage.
     */
    fun recordDiscountUsage(
        discount: Discount,
        member: Member,
        membership: Membership,
        originalPrice: BigDecimal,
        appliedByEmployeeId: UUID? = null,
        notes: String? = null
    ): DiscountUsageResponse {
        val discountAmount = discount.calculateDiscountAmount(originalPrice)
        val finalPrice = discount.calculateFinalPrice(originalPrice)

        val appliedByEmployee = appliedByEmployeeId?.let {
            employeeRepository.findById(it)
                .orElseThrow { EntityNotFoundException("Employee not found: $it") }
        }

        val usage = DiscountUsage(
            discount = discount,
            member = member,
            membership = membership,
            facility = membership.facility,
            originalPrice = originalPrice,
            discountAmount = discountAmount,
            finalPrice = finalPrice,
            appliedByEmployee = appliedByEmployee,
            notes = notes
        )

        usage.tenantId = discount.tenantId

        // Increment discount usage count
        discount.incrementUsage()
        discountRepository.save(discount)

        val savedUsage = discountUsageRepository.save(usage)

        logger.info("Discount applied: ${discount.name} to member ${member.getFullName()} - Saved: $discountAmount")

        return DiscountUsageResponse.from(savedUsage)
    }

    /**
     * Get discount usage history for a discount.
     */
    @Transactional(readOnly = true)
    fun getDiscountUsageHistory(discountId: UUID): List<DiscountUsageResponse> {
        return discountUsageRepository.findByDiscountId(discountId)
            .map { DiscountUsageResponse.from(it) }
    }

    /**
     * Get discount usage history for a member.
     */
    @Transactional(readOnly = true)
    fun getMemberDiscountUsage(memberId: UUID): List<DiscountUsageResponse> {
        return discountUsageRepository.findByMemberId(memberId)
            .map { DiscountUsageResponse.from(it) }
    }

    /**
     * Validate discount applicability for a member.
     */
    private fun validateDiscountForMember(
        discount: Discount,
        memberId: UUID,
        planId: UUID,
        price: BigDecimal
    ): ValidationResult {
        // Check if discount is active
        if (!discount.isActive) {
            return ValidationResult(false, "Discount is not active")
        }

        // Check if discount is within valid date range
        if (!discount.isCurrentlyValid()) {
            return ValidationResult(false, "Discount is not valid at this time")
        }

        // Check if discount has reached usage limit
        if (discount.hasReachedUsageLimit()) {
            return ValidationResult(false, "Discount usage limit reached")
        }

        // Check per-member usage limit
        discount.maxUsagePerMember?.let { maxPerMember ->
            val memberUsageCount = discountUsageRepository.countByDiscountIdAndMemberId(discount.id!!, memberId)
            if (memberUsageCount >= maxPerMember) {
                return ValidationResult(false, "You have already used this discount the maximum number of times")
            }
        }

        // Check if discount applies to the plan
        val plan = membershipPlanRepository.findById(planId)
            .orElseThrow { EntityNotFoundException("Plan not found: $planId") }

        if (!discount.isApplicableToPlan(plan)) {
            return ValidationResult(false, "Discount is not applicable to this membership plan")
        }

        // Check minimum purchase requirement
        if (!discount.meetsMinimumPurchase(price)) {
            return ValidationResult(
                false,
                "Minimum purchase amount of ${discount.currency} ${discount.minPurchaseAmount} required"
            )
        }

        return ValidationResult(true)
    }

    /**
     * Get expiring discounts (expiring within days).
     */
    @Transactional(readOnly = true)
    fun getExpiringDiscounts(facilityId: UUID, days: Int): List<DiscountBasicResponse> {
        val today = LocalDate.now()
        val expiryDate = today.plusDays(days.toLong())
        return discountRepository.findExpiringDiscounts(facilityId, today, expiryDate)
            .map { DiscountBasicResponse.from(it) }
    }

    /**
     * Deactivate expired discounts (background job).
     */
    fun deactivateExpiredDiscounts(facilityId: UUID): Int {
        val today = LocalDate.now()
        val activeDiscounts = discountRepository.findByFacilityIdAndIsActive(facilityId, true)
        var count = 0

        activeDiscounts.forEach { discount ->
            if (discount.validUntil.isBefore(today)) {
                discount.isActive = false
                discountRepository.save(discount)
                count++
            }
        }

        if (count > 0) {
            logger.info("Deactivated $count expired discounts")
        }

        return count
    }

    /**
     * Validation result data class.
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
}
