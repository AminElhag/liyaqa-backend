package com.liyaqa.backend.facility.booking.controller

import com.liyaqa.backend.facility.booking.domain.CourtStatus
import com.liyaqa.backend.facility.booking.domain.CourtType
import com.liyaqa.backend.facility.booking.dto.CourtBasicResponse
import com.liyaqa.backend.facility.booking.dto.CourtCreateRequest
import com.liyaqa.backend.facility.booking.dto.CourtResponse
import com.liyaqa.backend.facility.booking.dto.CourtUpdateRequest
import com.liyaqa.backend.facility.booking.service.CourtService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing courts.
 *
 * Base path: /api/v1/facility/courts
 */
@RestController
@RequestMapping("/api/v1/facility/courts")
class CourtController(
    private val courtService: CourtService
) {

    /**
     * Create a new court.
     * POST /api/v1/facility/courts
     */
    @PostMapping
    fun createCourt(
        @Valid @RequestBody request: CourtCreateRequest
    ): ResponseEntity<CourtResponse> {
        val court = courtService.createCourt(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(court)
    }

    /**
     * Get court by ID.
     * GET /api/v1/facility/courts/{id}
     */
    @GetMapping("/{id}")
    fun getCourtById(
        @PathVariable id: UUID
    ): ResponseEntity<CourtResponse> {
        val court = courtService.getCourtById(id)
        return ResponseEntity.ok(court)
    }

    /**
     * Update court.
     * PUT /api/v1/facility/courts/{id}
     */
    @PutMapping("/{id}")
    fun updateCourt(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CourtUpdateRequest
    ): ResponseEntity<CourtResponse> {
        val court = courtService.updateCourt(id, request)
        return ResponseEntity.ok(court)
    }

    /**
     * Delete court.
     * DELETE /api/v1/facility/courts/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteCourt(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        courtService.deleteCourt(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get all courts for a facility.
     * GET /api/v1/facility/courts/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getCourtsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<CourtResponse>> {
        val courts = courtService.getCourtsByFacility(facilityId)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get all courts for a branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}
     */
    @GetMapping("/by-branch/{branchId}")
    fun getCourtsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<CourtResponse>> {
        val courts = courtService.getCourtsByBranch(branchId)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get active courts for a branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}/active
     */
    @GetMapping("/by-branch/{branchId}/active")
    fun getActiveCourtsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<CourtBasicResponse>> {
        val courts = courtService.getActiveCourtsByBranch(branchId)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get courts by branch and type.
     * GET /api/v1/facility/courts/by-branch/{branchId}/by-type
     */
    @GetMapping("/by-branch/{branchId}/by-type")
    fun getCourtsByBranchAndType(
        @PathVariable branchId: UUID,
        @RequestParam courtType: CourtType
    ): ResponseEntity<List<CourtBasicResponse>> {
        val courts = courtService.getCourtsByBranchAndType(branchId, courtType)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get courts by branch and status.
     * GET /api/v1/facility/courts/by-branch/{branchId}/by-status
     */
    @GetMapping("/by-branch/{branchId}/by-status")
    fun getCourtsByBranchAndStatus(
        @PathVariable branchId: UUID,
        @RequestParam status: CourtStatus
    ): ResponseEntity<List<CourtResponse>> {
        val courts = courtService.getCourtsByBranchAndStatus(branchId, status)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get indoor courts by branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}/indoor
     */
    @GetMapping("/by-branch/{branchId}/indoor")
    fun getIndoorCourtsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<CourtBasicResponse>> {
        val courts = courtService.getIndoorCourtsByBranch(branchId)
        return ResponseEntity.ok(courts)
    }

    /**
     * Get courts with lighting by branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}/with-lighting
     */
    @GetMapping("/by-branch/{branchId}/with-lighting")
    fun getCourtsWithLightingByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<CourtBasicResponse>> {
        val courts = courtService.getCourtsWithLightingByBranch(branchId)
        return ResponseEntity.ok(courts)
    }

    /**
     * Count courts by branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}/count
     */
    @GetMapping("/by-branch/{branchId}/count")
    fun countCourtsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<Long> {
        val count = courtService.countCourtsByBranch(branchId)
        return ResponseEntity.ok(count)
    }

    /**
     * Count active courts by branch.
     * GET /api/v1/facility/courts/by-branch/{branchId}/count/active
     */
    @GetMapping("/by-branch/{branchId}/count/active")
    fun countActiveCourtsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<Long> {
        val count = courtService.countActiveCourtsByBranch(branchId)
        return ResponseEntity.ok(count)
    }
}
