package com.liyaqa.backend.internal.employee.dto

import com.liyaqa.backend.internal.employee.domain.Permission
import java.util.UUID

/**
 * Search filter supporting our operational need to quickly
 * locate employees across various dimensions.
 */
data class EmployeeSearchFilter(
    val searchTerm: String? = null,
    val status: String? = null,
    val includeTerminated: Boolean = false,
    val hasPermission: Permission? = null,
    val groupId: UUID? = null,
    val departmentFilter: String? = null
)