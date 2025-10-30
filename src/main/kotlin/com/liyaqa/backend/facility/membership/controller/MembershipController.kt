package com.liyaqa.backend.facility.membership.controller

import com.liyaqa.backend.facility.membership.domain.MembershipStatus
import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.facility.membership.service.MembershipService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing memberships (member-plan subscriptions).
 *
 * Base path: /api/v1/facility/memberships
 *
 * Endpoints:
 * - Membership CRUD: create, read
 * - Membership lifecycle: renew, cancel, suspend, reactivate
 * - Query by member, plan, branch, facility
 * - Expiring memberships tracking
 */
@RestController
@RequestMapping("/api/v1/facility/memberships")
class MembershipController(
    private val membershipService: MembershipService
) {

    /**
     * Create a new membership (subscribe member to plan).
     * POST /api/v1/facility/memberships
     */
    @PostMapping
    fun createMembership(
        @Valid @RequestBody request: MembershipCreateRequest
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.createMembership(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(membership)
    }

    /**
     * Get membership by ID.
     * GET /api/v1/facility/memberships/{id}
     */
    @GetMapping("/{id}")
    fun getMembershipById(
        @PathVariable id: UUID
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.getMembershipById(id)
        return ResponseEntity.ok(membership)
    }

    /**
     * Get membership by membership number.
     * GET /api/v1/facility/memberships/by-number/{membershipNumber}
     */
    @GetMapping("/by-number/{membershipNumber}")
    fun getMembershipByNumber(
        @PathVariable membershipNumber: String
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.getMembershipByNumber(membershipNumber)
        return ResponseEntity.ok(membership)
    }

    /**
     * Search memberships with filters.
     * GET /api/v1/facility/memberships
     */
    @GetMapping
    fun searchMemberships(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: MembershipStatus?,
        @RequestParam(required = false) branchId: UUID?,
        @RequestParam(required = false) facilityId: UUID?,
        @PageableDefault(size = 20, sort = ["startDate"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<MembershipBasicResponse>> {
        val memberships = membershipService.searchMemberships(searchTerm, status, branchId, facilityId, pageable)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get memberships by member.
     * GET /api/v1/facility/memberships/by-member/{memberId}
     */
    @GetMapping("/by-member/{memberId}")
    fun getMembershipsByMember(
        @PathVariable memberId: UUID
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getMembershipsByMember(memberId)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get active membership for member.
     * GET /api/v1/facility/memberships/by-member/{memberId}/active
     */
    @GetMapping("/by-member/{memberId}/active")
    fun getActiveMembershipByMember(
        @PathVariable memberId: UUID
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.getActiveMembershipByMember(memberId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(membership)
    }

    /**
     * Get memberships by plan.
     * GET /api/v1/facility/memberships/by-plan/{planId}
     */
    @GetMapping("/by-plan/{planId}")
    fun getMembershipsByPlan(
        @PathVariable planId: UUID
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getMembershipsByPlan(planId)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get memberships by branch.
     * GET /api/v1/facility/memberships/by-branch/{branchId}
     */
    @GetMapping("/by-branch/{branchId}")
    fun getMembershipsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getMembershipsByBranch(branchId)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get active memberships by branch.
     * GET /api/v1/facility/memberships/by-branch/{branchId}/active
     */
    @GetMapping("/by-branch/{branchId}/active")
    fun getActiveMembershipsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getActiveMembershipsByBranch(branchId)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get memberships by facility.
     * GET /api/v1/facility/memberships/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getMembershipsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getMembershipsByFacility(facilityId)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Get expiring memberships (within days).
     * GET /api/v1/facility/memberships/expiring
     */
    @GetMapping("/expiring")
    fun getExpiringMemberships(
        @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<List<MembershipResponse>> {
        val memberships = membershipService.getExpiringMemberships(days)
        return ResponseEntity.ok(memberships)
    }

    /**
     * Renew membership.
     * POST /api/v1/facility/memberships/{id}/renew
     */
    @PostMapping("/{id}/renew")
    fun renewMembership(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MembershipRenewRequest
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.renewMembership(id, request)
        return ResponseEntity.ok(membership)
    }

    /**
     * Cancel membership.
     * POST /api/v1/facility/memberships/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    fun cancelMembership(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MembershipCancelRequest
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.cancelMembership(id, request)
        return ResponseEntity.ok(membership)
    }

    /**
     * Suspend membership.
     * POST /api/v1/facility/memberships/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    fun suspendMembership(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MembershipSuspendRequest
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.suspendMembership(id, request)
        return ResponseEntity.ok(membership)
    }

    /**
     * Reactivate membership.
     * POST /api/v1/facility/memberships/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    fun reactivateMembership(
        @PathVariable id: UUID
    ): ResponseEntity<MembershipResponse> {
        val membership = membershipService.reactivateMembership(id)
        return ResponseEntity.ok(membership)
    }

    /**
     * Record booking usage for membership.
     * POST /api/v1/facility/memberships/{id}/record-booking
     */
    @PostMapping("/{id}/record-booking")
    fun recordBookingUsage(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        membershipService.recordBookingUsage(id)
        return ResponseEntity.ok().build()
    }
}
