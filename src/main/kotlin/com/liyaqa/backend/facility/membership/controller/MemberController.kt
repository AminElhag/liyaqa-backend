package com.liyaqa.backend.facility.membership.controller

import com.liyaqa.backend.facility.membership.domain.MemberStatus
import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.facility.membership.service.MemberService
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
 * REST API for managing members (customers).
 *
 * Base path: /api/v1/facility/members
 *
 * Endpoints:
 * - Member CRUD: create, read, update, delete
 * - Member lifecycle: suspend, reactivate, ban
 * - Search and filtering
 */
@RestController
@RequestMapping("/api/v1/facility/members")
class MemberController(
    private val memberService: MemberService
) {

    /**
     * Create a new member.
     * POST /api/v1/facility/members
     */
    @PostMapping
    fun createMember(
        @Valid @RequestBody request: MemberCreateRequest
    ): ResponseEntity<MemberResponse> {
        val member = memberService.createMember(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(member)
    }

    /**
     * Get member by ID.
     * GET /api/v1/facility/members/{id}
     */
    @GetMapping("/{id}")
    fun getMemberById(
        @PathVariable id: UUID
    ): ResponseEntity<MemberResponse> {
        val member = memberService.getMemberById(id)
        return ResponseEntity.ok(member)
    }

    /**
     * Update member.
     * PUT /api/v1/facility/members/{id}
     */
    @PutMapping("/{id}")
    fun updateMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MemberUpdateRequest
    ): ResponseEntity<MemberResponse> {
        val member = memberService.updateMember(id, request)
        return ResponseEntity.ok(member)
    }

    /**
     * Delete member.
     * DELETE /api/v1/facility/members/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteMember(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        memberService.deleteMember(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Search members with filters.
     * GET /api/v1/facility/members
     */
    @GetMapping
    fun searchMembers(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: MemberStatus?,
        @RequestParam(required = false) facilityId: UUID?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<Page<MemberBasicResponse>> {
        val members = memberService.searchMembers(searchTerm, status, facilityId, pageable)
        return ResponseEntity.ok(members)
    }

    /**
     * Get all members for a facility.
     * GET /api/v1/facility/members/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getMembersByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<MemberResponse>> {
        val members = memberService.getMembersByFacility(facilityId)
        return ResponseEntity.ok(members)
    }

    /**
     * Get active members for a facility.
     * GET /api/v1/facility/members/by-facility/{facilityId}/active
     */
    @GetMapping("/by-facility/{facilityId}/active")
    fun getActiveMembersByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<MemberResponse>> {
        val members = memberService.getActiveMembersByFacility(facilityId)
        return ResponseEntity.ok(members)
    }

    /**
     * Find member by email.
     * GET /api/v1/facility/members/by-facility/{facilityId}/by-email
     */
    @GetMapping("/by-facility/{facilityId}/by-email")
    fun findMemberByEmail(
        @PathVariable facilityId: UUID,
        @RequestParam email: String
    ): ResponseEntity<MemberResponse> {
        val member = memberService.findMemberByEmail(facilityId, email)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member)
    }

    /**
     * Find member by member number.
     * GET /api/v1/facility/members/by-facility/{facilityId}/by-number
     */
    @GetMapping("/by-facility/{facilityId}/by-number")
    fun findMemberByMemberNumber(
        @PathVariable facilityId: UUID,
        @RequestParam memberNumber: String
    ): ResponseEntity<MemberResponse> {
        val member = memberService.findMemberByMemberNumber(facilityId, memberNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member)
    }

    /**
     * Suspend member.
     * POST /api/v1/facility/members/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    fun suspendMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SuspendMemberRequest
    ): ResponseEntity<MemberResponse> {
        val member = memberService.suspendMember(id, request)
        return ResponseEntity.ok(member)
    }

    /**
     * Reactivate member.
     * POST /api/v1/facility/members/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    fun reactivateMember(
        @PathVariable id: UUID
    ): ResponseEntity<MemberResponse> {
        val member = memberService.reactivateMember(id)
        return ResponseEntity.ok(member)
    }

    /**
     * Ban member.
     * POST /api/v1/facility/members/{id}/ban
     */
    @PostMapping("/{id}/ban")
    fun banMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BanMemberRequest
    ): ResponseEntity<MemberResponse> {
        val member = memberService.banMember(id, request)
        return ResponseEntity.ok(member)
    }

    // ===== Branch-Level Endpoints =====

    /**
     * Get all members for a branch.
     * GET /api/v1/facility/members/by-branch/{branchId}
     */
    @GetMapping("/by-branch/{branchId}")
    fun getMembersByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MemberResponse>> {
        val members = memberService.getMembersByBranch(branchId)
        return ResponseEntity.ok(members)
    }

    /**
     * Get active members for a branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/active
     */
    @GetMapping("/by-branch/{branchId}/active")
    fun getActiveMembersByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<MemberResponse>> {
        val members = memberService.getActiveMembersByBranch(branchId)
        return ResponseEntity.ok(members)
    }

    /**
     * Get members by branch and status.
     * GET /api/v1/facility/members/by-branch/{branchId}/by-status
     */
    @GetMapping("/by-branch/{branchId}/by-status")
    fun getMembersByBranchAndStatus(
        @PathVariable branchId: UUID,
        @RequestParam status: MemberStatus
    ): ResponseEntity<List<MemberResponse>> {
        val members = memberService.getMembersByBranchAndStatus(branchId, status)
        return ResponseEntity.ok(members)
    }

    /**
     * Search members within a branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/search
     */
    @GetMapping("/by-branch/{branchId}/search")
    fun searchMembersByBranch(
        @PathVariable branchId: UUID,
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: MemberStatus?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<Page<MemberBasicResponse>> {
        val members = memberService.searchMembersByBranch(branchId, searchTerm, status, pageable)
        return ResponseEntity.ok(members)
    }

    /**
     * Find member by email in branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/by-email
     */
    @GetMapping("/by-branch/{branchId}/by-email")
    fun findMemberByEmailInBranch(
        @PathVariable branchId: UUID,
        @RequestParam email: String
    ): ResponseEntity<MemberResponse> {
        val member = memberService.findMemberByEmailInBranch(branchId, email)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member)
    }

    /**
     * Find member by member number in branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/by-number
     */
    @GetMapping("/by-branch/{branchId}/by-number")
    fun findMemberByMemberNumberInBranch(
        @PathVariable branchId: UUID,
        @RequestParam memberNumber: String
    ): ResponseEntity<MemberResponse> {
        val member = memberService.findMemberByMemberNumberInBranch(branchId, memberNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member)
    }

    /**
     * Count members by branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/count
     */
    @GetMapping("/by-branch/{branchId}/count")
    fun countMembersByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<Long> {
        val count = memberService.countMembersByBranch(branchId)
        return ResponseEntity.ok(count)
    }

    /**
     * Count active members by branch.
     * GET /api/v1/facility/members/by-branch/{branchId}/count/active
     */
    @GetMapping("/by-branch/{branchId}/count/active")
    fun countActiveMembersByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<Long> {
        val count = memberService.countActiveMembersByBranch(branchId)
        return ResponseEntity.ok(count)
    }

    /**
     * Count members by branch and status.
     * GET /api/v1/facility/members/by-branch/{branchId}/count/by-status
     */
    @GetMapping("/by-branch/{branchId}/count/by-status")
    fun countMembersByBranchAndStatus(
        @PathVariable branchId: UUID,
        @RequestParam status: MemberStatus
    ): ResponseEntity<Long> {
        val count = memberService.countMembersByBranchAndStatus(branchId, status)
        return ResponseEntity.ok(count)
    }
}
