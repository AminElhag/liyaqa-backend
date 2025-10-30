package com.liyaqa.backend.facility.employee.controller

import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupCreateRequest
import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupResponse
import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupUpdateRequest
import com.liyaqa.backend.facility.employee.service.FacilityEmployeeGroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing facility employee groups.
 *
 * Base path: /api/v1/facility/employee-groups
 */
@RestController
@RequestMapping("/api/v1/facility/employee-groups")
class FacilityEmployeeGroupController(
    private val groupService: FacilityEmployeeGroupService
) {

    /**
     * Create a new employee group.
     * POST /api/v1/facility/employee-groups
     */
    @PostMapping
    fun createGroup(
        @Valid @RequestBody request: FacilityEmployeeGroupCreateRequest
    ): ResponseEntity<FacilityEmployeeGroupResponse> {
        val group = groupService.createGroup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(group)
    }

    /**
     * Get group by ID.
     * GET /api/v1/facility/employee-groups/{id}
     */
    @GetMapping("/{id}")
    fun getGroupById(
        @PathVariable id: UUID
    ): ResponseEntity<FacilityEmployeeGroupResponse> {
        val group = groupService.getGroupById(id)
        return ResponseEntity.ok(group)
    }

    /**
     * Update group.
     * PUT /api/v1/facility/employee-groups/{id}
     */
    @PutMapping("/{id}")
    fun updateGroup(
        @PathVariable id: UUID,
        @Valid @RequestBody request: FacilityEmployeeGroupUpdateRequest
    ): ResponseEntity<FacilityEmployeeGroupResponse> {
        val group = groupService.updateGroup(id, request)
        return ResponseEntity.ok(group)
    }

    /**
     * Delete group.
     * DELETE /api/v1/facility/employee-groups/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteGroup(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        groupService.deleteGroup(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get all groups for a facility.
     * GET /api/v1/facility/employee-groups/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getGroupsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<FacilityEmployeeGroupResponse>> {
        val groups = groupService.getGroupsByFacility(facilityId)
        return ResponseEntity.ok(groups)
    }

    /**
     * Get system groups for a facility.
     * GET /api/v1/facility/employee-groups/by-facility/{facilityId}/system
     */
    @GetMapping("/by-facility/{facilityId}/system")
    fun getSystemGroupsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<FacilityEmployeeGroupResponse>> {
        val groups = groupService.getSystemGroupsByFacility(facilityId)
        return ResponseEntity.ok(groups)
    }

    /**
     * Get custom groups for a facility.
     * GET /api/v1/facility/employee-groups/by-facility/{facilityId}/custom
     */
    @GetMapping("/by-facility/{facilityId}/custom")
    fun getCustomGroupsByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<FacilityEmployeeGroupResponse>> {
        val groups = groupService.getCustomGroupsByFacility(facilityId)
        return ResponseEntity.ok(groups)
    }
}
