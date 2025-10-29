package com.liyaqa.backend.internal.repository

import com.liyaqa.backend.internal.domain.audit.AuditAction
import com.liyaqa.backend.internal.domain.audit.AuditLog
import com.liyaqa.backend.internal.domain.audit.EntityType
import com.liyaqa.backend.internal.domain.employee.EmployeeGroup
import com.liyaqa.backend.internal.domain.employee.Permission
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Repository for employee group management.
 *
 * Groups are the cornerstone of our RBAC system. This repository design
 * ensures we can efficiently manage permission inheritance and group
 * hierarchies as our organization scales.
 */
@Repository
interface EmployeeGroupRepository : JpaRepository<EmployeeGroup, UUID> {

    fun findByName(name: String): EmployeeGroup?
    fun existsByName(name: String): Boolean

    /**
     * System groups have special protection - they can't be deleted.
     * This query supports our UI's need to distinguish editable from
     * protected groups.
     */
    fun findByIsSystemTrue(): List<EmployeeGroup>
    fun findByIsSystemFalse(): List<EmployeeGroup>

    /**
     * Permission-based group discovery for role assignment workflows.
     * This allows admins to quickly find appropriate groups when
     * onboarding new team members with specific responsibilities.
     */
    @Query("""
        SELECT DISTINCT g FROM EmployeeGroup g 
        JOIN g.permissions p 
        WHERE p IN :permissions
    """)
    fun findByPermissions(@Param("permissions") permissions: Set<Permission>): List<EmployeeGroup>
}