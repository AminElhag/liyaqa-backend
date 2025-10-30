package com.liyaqa.backend.facility.employee.repository

import com.liyaqa.backend.facility.employee.domain.FacilityEmployee
import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for FacilityEmployee entity with comprehensive query capabilities.
 */
@Repository
interface FacilityEmployeeRepository : JpaRepository<FacilityEmployee, UUID> {

    /**
     * Find employee by email and facility (for authentication).
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.email = :email
    """)
    fun findByFacilityIdAndEmail(
        @Param("facilityId") facilityId: UUID,
        @Param("email") email: String
    ): FacilityEmployee?

    /**
     * Find employee by email only (across all facilities).
     */
    fun findByEmail(email: String): FacilityEmployee?

    /**
     * Check if email exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END
        FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.email = :email
    """)
    fun existsByFacilityIdAndEmail(
        @Param("facilityId") facilityId: UUID,
        @Param("email") email: String
    ): Boolean

    /**
     * Check if employee number exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END
        FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.employeeNumber = :employeeNumber
    """)
    fun existsByFacilityIdAndEmployeeNumber(
        @Param("facilityId") facilityId: UUID,
        @Param("employeeNumber") employeeNumber: String
    ): Boolean

    /**
     * Find employees by facility.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        ORDER BY e.lastName ASC, e.firstName ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<FacilityEmployee>

    /**
     * Find employees by tenant ID (for multi-tenancy).
     */
    fun findByTenantId(tenantId: String): List<FacilityEmployee>

    /**
     * Find employees by status.
     */
    fun findByStatus(status: FacilityEmployeeStatus): List<FacilityEmployee>

    /**
     * Count employees by status.
     */
    fun countByStatus(status: FacilityEmployeeStatus): Long

    /**
     * Find active employees by facility.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.status = 'ACTIVE'
        ORDER BY e.lastName ASC, e.firstName ASC
    """)
    fun findActiveByFacility(@Param("facilityId") facilityId: UUID): List<FacilityEmployee>

    /**
     * Search employees with filters.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE (:searchTerm IS NULL
            OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR e.status = :status)
        AND (:facilityId IS NULL OR e.facility.id = :facilityId)
        AND (:department IS NULL OR LOWER(e.department) = LOWER(:department))
        ORDER BY e.lastName ASC, e.firstName ASC
    """)
    fun searchEmployees(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: FacilityEmployeeStatus?,
        @Param("facilityId") facilityId: UUID?,
        @Param("department") department: String?,
        pageable: Pageable
    ): Page<FacilityEmployee>

    /**
     * Find employees by group.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        JOIN e.groups g
        WHERE g.id = :groupId
        ORDER BY e.lastName ASC, e.firstName ASC
    """)
    fun findByGroupId(@Param("groupId") groupId: UUID): List<FacilityEmployee>

    /**
     * Count employees by facility.
     */
    @Query("""
        SELECT COUNT(e) FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
    """)
    fun countByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Count active employees by facility.
     */
    @Query("""
        SELECT COUNT(e) FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.status = 'ACTIVE'
    """)
    fun countActiveByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Find employees with locked accounts.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE e.accountLockedUntil IS NOT NULL
        AND e.accountLockedUntil > CURRENT_TIMESTAMP
        ORDER BY e.accountLockedUntil DESC
    """)
    fun findLockedAccounts(): List<FacilityEmployee>

    /**
     * Find employees who haven't logged in recently.
     */
    @Query("""
        SELECT e FROM FacilityEmployee e
        WHERE e.status = 'ACTIVE'
        AND (e.lastLoginAt IS NULL OR e.lastLoginAt < :cutoffDate)
        ORDER BY e.lastLoginAt ASC NULLS FIRST
    """)
    fun findInactiveLogins(
        @Param("cutoffDate") cutoffDate: java.time.Instant
    ): List<FacilityEmployee>

    /**
     * Get employee statistics grouped by status.
     */
    @Query("""
        SELECT e.status, COUNT(e)
        FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        GROUP BY e.status
    """)
    fun countByStatusGrouped(@Param("facilityId") facilityId: UUID): List<Array<Any>>

    /**
     * Get employee statistics grouped by department.
     */
    @Query("""
        SELECT e.department, COUNT(e)
        FROM FacilityEmployee e
        WHERE e.facility.id = :facilityId
        AND e.status = 'ACTIVE'
        AND e.department IS NOT NULL
        GROUP BY e.department
        ORDER BY COUNT(e) DESC
    """)
    fun countByDepartmentGrouped(@Param("facilityId") facilityId: UUID): List<Array<Any>>
}
