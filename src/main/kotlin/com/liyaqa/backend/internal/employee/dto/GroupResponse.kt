package com.liyaqa.backend.internal.employee.dto

import com.liyaqa.backend.internal.employee.domain.Permission
import java.util.UUID

data class GroupResponse(
    val id: UUID,
    val name: String,
    val permissions: Set<Permission>
)