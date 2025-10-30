package com.liyaqa.backend.facility.employee.service

import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeGroup
import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupCreateRequest
import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupResponse
import com.liyaqa.backend.facility.employee.dto.FacilityEmployeeGroupUpdateRequest
import com.liyaqa.backend.facility.employee.repository.FacilityEmployeeGroupRepository
import com.liyaqa.backend.internal.repository.SportFacilityRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for managing facility employee groups.
 */
@Service
@Transactional
class FacilityEmployeeGroupService(
    private val groupRepository: FacilityEmployeeGroupRepository,
    private val facilityRepository: SportFacilityRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new employee group.
     */
    fun createGroup(request: FacilityEmployeeGroupCreateRequest): FacilityEmployeeGroupResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Check for duplicate name
        if (groupRepository.existsByFacilityIdAndName(request.facilityId, request.name)) {
            throw IllegalArgumentException("Group name '${request.name}' already exists for this facility")
        }

        // Create group
        val group = FacilityEmployeeGroup(
            facility = facility,
            name = request.name,
            description = request.description,
            isSystem = false,
            permissions = request.permissions.toMutableSet()
        )

        // Set tenant ID for multi-tenancy
        group.tenantId = facility.tenantId

        val savedGroup = groupRepository.save(group)

        logger.info("Employee group created: ${savedGroup.name} for facility ${facility.name} with ${savedGroup.permissions.size} permissions")

        return FacilityEmployeeGroupResponse.from(savedGroup)
    }

    /**
     * Get group by ID.
     */
    @Transactional(readOnly = true)
    fun getGroupById(id: UUID): FacilityEmployeeGroupResponse {
        val group = groupRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Group not found: $id") }

        return FacilityEmployeeGroupResponse.from(group)
    }

    /**
     * Update group.
     */
    fun updateGroup(id: UUID, request: FacilityEmployeeGroupUpdateRequest): FacilityEmployeeGroupResponse {
        val group = groupRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Group not found: $id") }

        // System groups cannot be modified
        if (group.isSystem) {
            throw IllegalStateException("System groups cannot be modified")
        }

        // Update fields
        request.name?.let {
            // Check if new name is already in use
            if (it != group.name && groupRepository.existsByFacilityIdAndName(group.facility.id!!, it)) {
                throw IllegalArgumentException("Group name '$it' already exists for this facility")
            }
            group.name = it
        }
        request.description?.let { group.description = it }
        request.permissions?.let { group.setPermissions(it) }

        val savedGroup = groupRepository.save(group)

        logger.info("Employee group updated: ${savedGroup.name}")

        return FacilityEmployeeGroupResponse.from(savedGroup)
    }

    /**
     * Delete group.
     */
    fun deleteGroup(id: UUID) {
        val group = groupRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Group not found: $id") }

        // System groups cannot be deleted
        if (group.isSystem) {
            throw IllegalStateException("System groups cannot be deleted")
        }

        groupRepository.delete(group)

        logger.warn("Employee group deleted: ${group.name}")
    }

    /**
     * Get groups by facility.
     */
    @Transactional(readOnly = true)
    fun getGroupsByFacility(facilityId: UUID): List<FacilityEmployeeGroupResponse> {
        return groupRepository.findByFacilityId(facilityId)
            .map { FacilityEmployeeGroupResponse.from(it) }
    }

    /**
     * Get system groups by facility.
     */
    @Transactional(readOnly = true)
    fun getSystemGroupsByFacility(facilityId: UUID): List<FacilityEmployeeGroupResponse> {
        return groupRepository.findSystemGroupsByFacility(facilityId)
            .map { FacilityEmployeeGroupResponse.from(it) }
    }

    /**
     * Get custom groups by facility.
     */
    @Transactional(readOnly = true)
    fun getCustomGroupsByFacility(facilityId: UUID): List<FacilityEmployeeGroupResponse> {
        return groupRepository.findCustomGroupsByFacility(facilityId)
            .map { FacilityEmployeeGroupResponse.from(it) }
    }
}
