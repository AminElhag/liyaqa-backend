package com.liyaqa.backend.internal.employee.dto

import com.liyaqa.backend.internal.employee.domain.EmployeeGroup
import com.liyaqa.backend.internal.employee.domain.Permission
import java.util.UUID

data class GroupResponse(
    val id: UUID,
    val name: String,
    val permissions: Set<Permission>
) {
    companion object {
        fun from(group: EmployeeGroup): GroupResponse {
            return GroupResponse(
                id = group.id!!,
                name = group.name,
                permissions = group.permissions
            )
        }
    }
}