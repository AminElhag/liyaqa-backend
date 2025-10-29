package com.liyaqa.backend.internal.controller

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.domain.facility.BranchStatus
import com.liyaqa.backend.internal.domain.facility.FacilityStatus
import com.liyaqa.backend.internal.dto.facility.*
import com.liyaqa.backend.internal.service.FacilityService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing sport facilities and their branches.
 *
 * Base path: /api/v1/internal/facilities
 *
 * Endpoints:
 * - Facility CRUD: create, read, update, delete facilities
 * - Branch CRUD: create, read, update, delete branches
 * - Search: filter and search facilities and branches
 * - Tenant operations: get facilities/branches for a tenant
 */
@RestController
@RequestMapping("/api/v1/internal/facilities")
class FacilityController(
    private val facilityService: FacilityService
) {

    // ========== FACILITY ENDPOINTS ==========

    /**
     * Create a new sport facility.
     * POST /api/v1/internal/facilities
     */
    @PostMapping
    fun createFacility(
        @Valid @RequestBody request: FacilityCreateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<FacilityResponse> {
        val facility = facilityService.createFacility(request, employee)
        return ResponseEntity.status(HttpStatus.CREATED).body(facility)
    }

    /**
     * Get facility by ID.
     * GET /api/v1/internal/facilities/{id}
     */
    @GetMapping("/{id}")
    fun getFacilityById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<FacilityResponse> {
        val facility = facilityService.getFacilityById(id, employee)
        return ResponseEntity.ok(facility)
    }

    /**
     * Update facility.
     * PUT /api/v1/internal/facilities/{id}
     */
    @PutMapping("/{id}")
    fun updateFacility(
        @PathVariable id: UUID,
        @Valid @RequestBody request: FacilityUpdateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<FacilityResponse> {
        val facility = facilityService.updateFacility(id, request, employee)
        return ResponseEntity.ok(facility)
    }

    /**
     * Delete facility (cascade deletes branches).
     * DELETE /api/v1/internal/facilities/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteFacility(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Void> {
        facilityService.deleteFacility(id, employee)
        return ResponseEntity.noContent().build()
    }

    /**
     * Search facilities with filters.
     * GET /api/v1/internal/facilities
     */
    @GetMapping
    fun searchFacilities(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: FacilityStatus?,
        @RequestParam(required = false) facilityType: String?,
        @RequestParam(required = false) ownerTenantId: UUID?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Page<FacilityBasicResponse>> {
        val facilities = facilityService.searchFacilities(
            searchTerm, status, facilityType, ownerTenantId, pageable, employee
        )
        return ResponseEntity.ok(facilities)
    }

    /**
     * Get all facilities for a tenant.
     * GET /api/v1/internal/facilities/by-tenant/{tenantId}
     */
    @GetMapping("/by-tenant/{tenantId}")
    fun getFacilitiesByTenant(
        @PathVariable tenantId: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<List<FacilityResponse>> {
        val facilities = facilityService.getFacilitiesByTenant(tenantId, employee)
        return ResponseEntity.ok(facilities)
    }

    // ========== BRANCH ENDPOINTS ==========

    /**
     * Create a new facility branch.
     * POST /api/v1/internal/facilities/branches
     */
    @PostMapping("/branches")
    fun createBranch(
        @Valid @RequestBody request: BranchCreateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<BranchResponse> {
        val branch = facilityService.createBranch(request, employee)
        return ResponseEntity.status(HttpStatus.CREATED).body(branch)
    }

    /**
     * Get branch by ID.
     * GET /api/v1/internal/facilities/branches/{id}
     */
    @GetMapping("/branches/{id}")
    fun getBranchById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<BranchResponse> {
        val branch = facilityService.getBranchById(id, employee)
        return ResponseEntity.ok(branch)
    }

    /**
     * Update branch.
     * PUT /api/v1/internal/facilities/branches/{id}
     */
    @PutMapping("/branches/{id}")
    fun updateBranch(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BranchUpdateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<BranchResponse> {
        val branch = facilityService.updateBranch(id, request, employee)
        return ResponseEntity.ok(branch)
    }

    /**
     * Delete branch.
     * DELETE /api/v1/internal/facilities/branches/{id}
     */
    @DeleteMapping("/branches/{id}")
    fun deleteBranch(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Void> {
        facilityService.deleteBranch(id, employee)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get all branches for a facility.
     * GET /api/v1/internal/facilities/{facilityId}/branches
     */
    @GetMapping("/{facilityId}/branches")
    fun getBranchesByFacility(
        @PathVariable facilityId: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<List<BranchResponse>> {
        val branches = facilityService.getBranchesByFacility(facilityId, employee)
        return ResponseEntity.ok(branches)
    }

    /**
     * Search branches with filters.
     * GET /api/v1/internal/facilities/branches
     */
    @GetMapping("/branches")
    fun searchBranches(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: BranchStatus?,
        @RequestParam(required = false) facilityId: UUID?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) country: String?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Page<BranchBasicResponse>> {
        val branches = facilityService.searchBranches(
            searchTerm, status, facilityId, city, country, pageable, employee
        )
        return ResponseEntity.ok(branches)
    }
}
