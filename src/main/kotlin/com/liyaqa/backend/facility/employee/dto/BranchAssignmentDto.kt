package com.liyaqa.backend.facility.employee.dto

import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * Request to assign employee to branches.
 */
data class AssignBranchesRequest(
    @field:NotEmpty(message = "Branch IDs cannot be empty")
    val branchIds: Set<UUID>
)

/**
 * Request to remove employee from branches.
 */
data class RemoveBranchesRequest(
    @field:NotEmpty(message = "Branch IDs cannot be empty")
    val branchIds: Set<UUID>
)

/**
 * Response for branch assignment operations.
 */
data class BranchAssignmentResponse(
    val employeeId: UUID,
    val employeeName: String,
    val assignedBranches: List<BranchBasicInfo>,
    val hasAccessToAllBranches: Boolean,
    val message: String
)
