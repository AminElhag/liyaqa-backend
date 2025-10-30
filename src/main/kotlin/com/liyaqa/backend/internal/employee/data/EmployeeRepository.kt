package com.liyaqa.backend.internal.employee.data

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.EmployeeStatus
import com.liyaqa.backend.internal.employee.domain.Permission
import com.liyaqa.backend.internal.employee.dto.EmployeeSearchFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface EmployeeRepository : JpaRepository<Employee, UUID> {

    /**
     * Core identity queries supporting authentication and uniqueness validation.
     * These form the foundation of our security model.
     */
    fun findByEmail(email: String): Employee?
    fun existsByEmail(email: String): Boolean

    /**
     * Status-based queries for operational visibility.
     * This allows us to quickly assess team capacity and health.
     */
    fun findByStatus(status: EmployeeStatus): List<Employee>
    fun countByStatus(status: EmployeeStatus): Long

    /**
     * Department-based queries supporting organizational structure.
     * Critical for workload distribution and team management.
     */
    fun findByDepartment(department: String): List<Employee>
    fun findByDepartmentAndStatus(department: String, status: EmployeeStatus): List<Employee>

    /**
     * Permission-based query for security and compliance.
     * This design allows us to quickly identify who has access to critical operations,
     * essential for both security audits and incident response.
     */
    @Query(
        """
        SELECT DISTINCT e FROM Employee e 
        JOIN e.groups g 
        JOIN g.permissions p 
        WHERE p = :permission
    """
    )
    fun findByPermission(@Param("permission") permission: Permission): List<Employee>

    @Query(
        """
        SELECT COUNT(DISTINCT e) FROM Employee e 
        JOIN e.groups g 
        JOIN g.permissions p 
        WHERE p = :permission AND e.status = 'ACTIVE'
    """
    )
    fun countByPermission(@Param("permission") permission: Permission): Long

    /**
     * Security monitoring queries supporting our threat detection capabilities.
     * These patterns emerged from real incidents and near-misses in our operational history.
     */
    @Query(
        """
        SELECT e FROM Employee e 
        WHERE e.failedLoginAttempts > :threshold 
        AND e.status = 'ACTIVE'
    """
    )
    fun findWithHighFailedAttempts(@Param("threshold") threshold: Int): List<Employee>

    @Query(
        """
        SELECT e FROM Employee e 
        WHERE e.lockedUntil IS NOT NULL 
        AND e.lockedUntil > :now
    """
    )
    fun findCurrentlyLocked(@Param("now") now: Instant): List<Employee>

    /**
     * Workload distribution queries for support team optimization.
     * This design reflects our commitment to preventing burnout while
     * maintaining service quality.
     */
    @Query(
        """
        SELECT e FROM Employee e 
        WHERE e.department = 'Support' 
        AND e.status = 'ACTIVE' 
        AND e.currentActiveTickets < e.maxConcurrentTickets
        ORDER BY e.currentActiveTickets ASC
    """
    )
    fun findAvailableSupportAgents(): List<Employee>

    /**
     * Complex search supporting our admin UI's filtering needs.
     * This single query method replaces what would otherwise be dozens of
     * service-layer combinations, improving performance and maintainability.
     *
     * We use JPQL here instead of Criteria API for readability - the trade-off
     * is slightly less type safety for significantly better maintainability.
     */
    @Query(
        """
        SELECT e FROM Employee e
        WHERE (:searchTerm IS NULL OR
               LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:department IS NULL OR e.department = :department)
        AND (:status IS NULL OR e.status = :status)
        AND (:includeTerminated = true OR e.status != 'TERMINATED')
    """
    )
    fun searchEmployees(
        @Param("searchTerm") searchTerm: String?,
        @Param("department") department: String?,
        @Param("status") status: String?,
        @Param("includeTerminated") includeTerminated: Boolean,
        pageable: Pageable
    ): Page<Employee>

    /**
     * Activity tracking queries for compliance and analytics.
     * These support our data-driven approach to team management.
     */
    @Query(
        """
        SELECT e FROM Employee e 
        WHERE e.lastLoginAt IS NULL 
        AND e.status = 'ACTIVE' 
        AND e.createdAt < :cutoffDate
    """
    )
    fun findNeverLoggedIn(@Param("cutoffDate") cutoffDate: Instant): List<Employee>

    @Query(
        """
        SELECT e FROM Employee e 
        WHERE e.lastLoginAt < :inactiveThreshold 
        AND e.status = 'ACTIVE'
    """
    )
    fun findInactiveEmployees(@Param("inactiveThreshold") inactiveThreshold: Instant): List<Employee>
}

