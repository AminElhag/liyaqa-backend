package com.liyaqa.backend.internal.dto.employee

import com.liyaqa.backend.internal.domain.employee.Permission
import java.util.UUID

data class GroupResponse(
    val id: UUID,
    val name: String,
    val permissions: Set<Permission>
)