package com.liyaqa.backend.facility.membership.controller

import com.liyaqa.backend.facility.membership.domain.MembershipPlanType
import com.liyaqa.backend.facility.membership.dto.MembershipPlanBasicResponse
import com.liyaqa.backend.facility.membership.dto.MembershipPlanCreateRequest
import com.liyaqa.backend.facility.membership.dto.MembershipPlanResponse
import com.liyaqa.backend.facility.membership.dto.MembershipPlanUpdateRequest
import com.liyaqa.backend.facility.membership.service.MembershipPlanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing membership plans.
 *
 * Base path: /api/v1/facility/membership-plans
 *
 * Endpoints:
 * - Plan CRUD: create, read, update, delete
 * - Query by branch, facility, type
 * - Public-facing visible plans
 */
@RestController
@RequestMapping("/api/v1/facility/membership-plans")
class MembershipPlanController(
    private val planService: MembershipPlanService
) {

    /**
     * Create a new membership plan.
     * POST /api/v1/facility/membership-plans
     */
    @PostMapping
    fun createPlan(
        @Valid @RequestBody request: MembershipPlanCreateRequest
    ): ResponseEntity<MembershipPlanResponse> {
        val plan = planService.createPlan(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(plan)
    }

    /**
     * Get plan by ID.
     * GET /api/v1/facility/membership-plans/{id}
     */
    @GetMapping("/{id}")
    fun getPlanById(
        @PathVariable id: UUID
    ): ResponseEntity<MembershipPlanResponse> {
        val plan = planService.getPlanById(id)
        return ResponseEntity.ok(plan)
    }

    /**
     * Update plan.
     * PUT /api/v1/facility/membership-plans/{id}
     */
    @PutMapping("/{id}")
    fun updatePlan(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MembershipPlanUpdateRequest
    ): ResponseEntity<MembershipPlanResponse> {
        val plan = planService.updatePlan(id, request)
        return ResponseEntity.ok(plan)
    }

    /**
     * Delete plan.
     * DELETE /api/v1/facility/membership-plans/{id}
     */
    @DeleteMapping("/{id}")
    fun deletePlan(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        planService.deletePlan(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get plans by branch.
     * GET /api/v1/facility/membership-plans/by-branch/{branchId}
     */
    @GetMapping("/by-branch/{branchId}")
    fun getPlansByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MembershipPlanResponse>> {
        val plans = planService.getPlansByBranch(branchId)
        return ResponseEntity.ok(plans)
    }

    /**
     * Get active plans by branch.
     * GET /api/v1/facility/membership-plans/by-branch/{branchId}/active
     */
    @GetMapping("/by-branch/{branchId}/active")
    fun getActivePlansByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MembershipPlanResponse>> {
        val plans = planService.getActivePlansByBranch(branchId)
        return ResponseEntity.ok(plans)
    }

    /**
     * Get visible plans by branch (for public display).
     * GET /api/v1/facility/membership-plans/by-branch/{branchId}/visible
     */
    @GetMapping("/by-branch/{branchId}/visible")
    fun getVisiblePlansByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MembershipPlanBasicResponse>> {
        val plans = planService.getVisiblePlansByBranch(branchId)
        return ResponseEntity.ok(plans)
    }

    /**
     * Get plans by facility.
     * GET /api/v1/facility/membership-plans/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getPlansByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<MembershipPlanResponse>> {
        val plans = planService.getPlansByFacility(facilityId)
        return ResponseEntity.ok(plans)
    }

    /**
     * Get plans by type.
     * GET /api/v1/facility/membership-plans/by-type/{planType}
     */
    @GetMapping("/by-type/{planType}")
    fun getPlansByType(
        @PathVariable planType: MembershipPlanType
    ): ResponseEntity<List<MembershipPlanResponse>> {
        val plans = planService.getPlansByType(planType)
        return ResponseEntity.ok(plans)
    }
}
