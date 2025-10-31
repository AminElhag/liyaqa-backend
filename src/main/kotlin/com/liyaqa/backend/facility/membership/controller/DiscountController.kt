package com.liyaqa.backend.facility.membership.controller

import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.facility.membership.service.DiscountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing discounts and discount codes.
 *
 * Base path: /api/v1/facility/discounts
 *
 * Endpoints:
 * - Discount CRUD: create, read, update, delete
 * - Discount validation: validate code, check applicability
 * - Usage tracking: view discount usage history
 * - Management: get expiring discounts, deactivate expired
 */
@RestController
@RequestMapping("/api/v1/facility/discounts")
class DiscountController(
    private val discountService: DiscountService
) {

    /**
     * Create a new discount.
     * POST /api/v1/facility/discounts
     */
    @PostMapping
    fun createDiscount(
        @Valid @RequestBody request: DiscountCreateRequest
    ): ResponseEntity<DiscountResponse> {
        val discount = discountService.createDiscount(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(discount)
    }

    /**
     * Get discount by ID.
     * GET /api/v1/facility/discounts/{id}
     */
    @GetMapping("/{id}")
    fun getDiscountById(
        @PathVariable id: UUID
    ): ResponseEntity<DiscountResponse> {
        val discount = discountService.getDiscountById(id)
        return ResponseEntity.ok(discount)
    }

    /**
     * Update discount.
     * PUT /api/v1/facility/discounts/{id}
     */
    @PutMapping("/{id}")
    fun updateDiscount(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DiscountUpdateRequest
    ): ResponseEntity<DiscountResponse> {
        val discount = discountService.updateDiscount(id, request)
        return ResponseEntity.ok(discount)
    }

    /**
     * Delete discount.
     * DELETE /api/v1/facility/discounts/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteDiscount(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        discountService.deleteDiscount(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get all discounts for a facility.
     * GET /api/v1/facility/discounts/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getDiscountsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<DiscountResponse>> {
        val discounts = discountService.getDiscountsByFacility(facilityId)
        return ResponseEntity.ok(discounts)
    }

    /**
     * Get active discounts for a facility.
     * GET /api/v1/facility/discounts/by-facility/{facilityId}/active
     */
    @GetMapping("/by-facility/{facilityId}/active")
    fun getActiveDiscounts(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<DiscountBasicResponse>> {
        val discounts = discountService.getActiveDiscounts(facilityId)
        return ResponseEntity.ok(discounts)
    }

    /**
     * Get currently valid discounts (active and within date range).
     * GET /api/v1/facility/discounts/by-facility/{facilityId}/valid
     */
    @GetMapping("/by-facility/{facilityId}/valid")
    fun getCurrentlyValidDiscounts(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<DiscountBasicResponse>> {
        val discounts = discountService.getCurrentlyValidDiscounts(facilityId)
        return ResponseEntity.ok(discounts)
    }

    /**
     * Get expiring discounts.
     * GET /api/v1/facility/discounts/by-facility/{facilityId}/expiring
     */
    @GetMapping("/by-facility/{facilityId}/expiring")
    fun getExpiringDiscounts(
        @PathVariable facilityId: UUID,
        @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<List<DiscountBasicResponse>> {
        val discounts = discountService.getExpiringDiscounts(facilityId, days)
        return ResponseEntity.ok(discounts)
    }

    /**
     * Validate a discount code.
     * POST /api/v1/facility/discounts/validate
     */
    @PostMapping("/validate")
    fun validateDiscount(
        @Valid @RequestBody request: ValidateDiscountRequest
    ): ResponseEntity<DiscountValidationResponse> {
        val validation = discountService.validateDiscount(request)
        return ResponseEntity.ok(validation)
    }

    /**
     * Get discount usage history for a discount.
     * GET /api/v1/facility/discounts/{id}/usage
     */
    @GetMapping("/{id}/usage")
    fun getDiscountUsageHistory(
        @PathVariable id: UUID
    ): ResponseEntity<List<DiscountUsageResponse>> {
        val usage = discountService.getDiscountUsageHistory(id)
        return ResponseEntity.ok(usage)
    }

    /**
     * Get discount usage history for a member.
     * GET /api/v1/facility/discounts/member/{memberId}/usage
     */
    @GetMapping("/member/{memberId}/usage")
    fun getMemberDiscountUsage(
        @PathVariable memberId: UUID
    ): ResponseEntity<List<DiscountUsageResponse>> {
        val usage = discountService.getMemberDiscountUsage(memberId)
        return ResponseEntity.ok(usage)
    }

    /**
     * Deactivate expired discounts.
     * POST /api/v1/facility/discounts/by-facility/{facilityId}/deactivate-expired
     */
    @PostMapping("/by-facility/{facilityId}/deactivate-expired")
    fun deactivateExpiredDiscounts(
        @PathVariable facilityId: UUID
    ): ResponseEntity<Map<String, Int>> {
        val count = discountService.deactivateExpiredDiscounts(facilityId)
        return ResponseEntity.ok(mapOf("deactivatedCount" to count))
    }
}
