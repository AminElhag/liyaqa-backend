package com.liyaqa.backend.internal.dto.employee

import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class UpdateGroupsRequest(
    @field:NotEmpty(message = "At least one group must be assigned")
    val groupIds: Set<UUID>
)