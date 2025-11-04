package com.liyaqa.backend.api.v1.controller

import com.liyaqa.backend.api.security.ApiKeyPrincipal
import com.liyaqa.backend.api.v1.dto.*
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Public API v1 - Facility Controller.
 *
 * Provides read-only access to facility, branch, and court information.
 */
@RestController
@RequestMapping("/api/v1/public/facilities")
class PublicFacilityController(
    private val facilityRepository: SportFacilityRepository
) {

    /**
     * Get facility information.
     * GET /api/v1/public/facilities
     *
     * Scope required: facilities:read
     */
    @GetMapping
    fun getFacility(
        @AuthenticationPrincipal principal: ApiKeyPrincipal
    ): ResponseEntity<PublicFacilityResponse> {
        val facility = facilityRepository.findById(principal.facilityId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val response = PublicFacilityResponse(
            id = facility.id!!,
            name = facility.name,
            description = facility.description,
            facilityType = facility.facilityType,
            contactEmail = facility.contactEmail,
            contactPhone = facility.contactPhone,
            website = facility.website,
            timezone = facility.timezone,
            currency = facility.currency
        )

        return ResponseEntity.ok(response)
    }

    /**
     * List all branches.
     * GET /api/v1/public/facilities/branches
     *
     * Scope required: facilities:read
     */
    @GetMapping("/branches")
    fun listBranches(
        @AuthenticationPrincipal principal: ApiKeyPrincipal
    ): ResponseEntity<ApiSuccessResponse<List<String>>> {
        // TODO: Implement branch listing
        return ResponseEntity.ok(ApiSuccessResponse(
            success = true,
            data = listOf("Branch listing endpoint - implementation pending")
        ))
    }

    /**
     * Get specific branch.
     * GET /api/v1/public/facilities/branches/{branchId}
     *
     * Scope required: facilities:read
     */
    @GetMapping("/branches/{branchId}")
    fun getBranch(
        @AuthenticationPrincipal principal: ApiKeyPrincipal,
        @PathVariable branchId: UUID
    ): ResponseEntity<ApiSuccessResponse<String>> {
        // TODO: Implement branch retrieval
        return ResponseEntity.ok(ApiSuccessResponse(
            success = true,
            data = "Branch retrieval endpoint - implementation pending"
        ))
    }

    /**
     * List courts at a branch.
     * GET /api/v1/public/facilities/branches/{branchId}/courts
     *
     * Scope required: courts:read
     */
    @GetMapping("/branches/{branchId}/courts")
    fun listCourts(
        @AuthenticationPrincipal principal: ApiKeyPrincipal,
        @PathVariable branchId: UUID
    ): ResponseEntity<ApiSuccessResponse<List<String>>> {
        // TODO: Implement court listing
        return ResponseEntity.ok(ApiSuccessResponse(
            success = true,
            data = listOf("Court listing endpoint - implementation pending")
        ))
    }

    /**
     * Get specific court.
     * GET /api/v1/public/facilities/courts/{courtId}
     *
     * Scope required: courts:read
     */
    @GetMapping("/courts/{courtId}")
    fun getCourt(
        @AuthenticationPrincipal principal: ApiKeyPrincipal,
        @PathVariable courtId: UUID
    ): ResponseEntity<ApiSuccessResponse<String>> {
        // TODO: Implement court retrieval
        return ResponseEntity.ok(ApiSuccessResponse(
            success = true,
            data = "Court retrieval endpoint - implementation pending"
        ))
    }
}
