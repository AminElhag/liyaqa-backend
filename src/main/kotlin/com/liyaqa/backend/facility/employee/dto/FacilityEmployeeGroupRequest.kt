package com.liyaqa.backend.facility.employee.dto

import com.liyaqa.backend.facility.employee.domain.FacilityPermission
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * Request DTO for creating a new facility employee group.
 */
data class FacilityEmployeeGroupCreateRequest(
    @field:NotBlank(message = "Facility ID is required")
    val facilityId: UUID,

    @field:NotBlank(message = "Group name is required")
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String,

    val description: String? = null,

    val permissions: Set<FacilityPermission> = emptySet()
)

/**
 * Request DTO for updating a facility employee group.
 */
data class FacilityEmployeeGroupUpdateRequest(
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String? = null,

    val description: String? = null,

    val permissions: Set<FacilityPermission>? = null
)
