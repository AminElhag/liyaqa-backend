package com.liyaqa.backend.facility.employee.dto

import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeGroup
import com.liyaqa.backend.facility.employee.domain.FacilityPermission
import java.time.Instant
import java.util.*

/**
 * Response DTO for facility employee group with full details.
 */
data class FacilityEmployeeGroupResponse(
    val id: UUID,
    val tenantId: String,
    val facilityId: UUID,
    val facilityName: String,

    val name: String,
    val description: String?,
    val isSystem: Boolean,

    val permissions: Set<FacilityPermission>,

    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long
) {
    companion object {
        fun from(group: FacilityEmployeeGroup): FacilityEmployeeGroupResponse {
            return FacilityEmployeeGroupResponse(
                id = group.id!!,
                tenantId = group.tenantId,
                facilityId = group.facility.id!!,
                facilityName = group.facility.name,
                name = group.name,
                description = group.description,
                isSystem = group.isSystem,
                permissions = group.permissions,
                createdAt = group.createdAt,
                updatedAt = group.updatedAt,
                version = group.version
            )
        }
    }
}

/**
 * Response DTO for facility employee group with basic information (for lists).
 */
data class FacilityEmployeeGroupBasicResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val permissionCount: Int,
    val isSystem: Boolean
) {
    companion object {
        fun from(group: FacilityEmployeeGroup): FacilityEmployeeGroupBasicResponse {
            return FacilityEmployeeGroupBasicResponse(
                id = group.id!!,
                name = group.name,
                description = group.description,
                permissionCount = group.permissions.size,
                isSystem = group.isSystem
            )
        }
    }
}
