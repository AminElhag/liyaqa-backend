package com.liyaqa.backend.facility.employee.repository

import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for FacilityEmployeeGroup entity.
 */
@Repository
interface FacilityEmployeeGroupRepository : JpaRepository<FacilityEmployeeGroup, UUID> {

    /**
     * Find groups by facility.
     */
    @Query("""
        SELECT g FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
        ORDER BY g.name ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<FacilityEmployeeGroup>

    /**
     * Find groups by tenant ID (for multi-tenancy).
     */
    fun findByTenantId(tenantId: String): List<FacilityEmployeeGroup>

    /**
     * Find group by facility and name.
     */
    @Query("""
        SELECT g FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
        AND LOWER(g.name) = LOWER(:name)
    """)
    fun findByFacilityIdAndName(
        @Param("facilityId") facilityId: UUID,
        @Param("name") name: String
    ): FacilityEmployeeGroup?

    /**
     * Check if group name exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END
        FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
        AND LOWER(g.name) = LOWER(:name)
    """)
    fun existsByFacilityIdAndName(
        @Param("facilityId") facilityId: UUID,
        @Param("name") name: String
    ): Boolean

    /**
     * Find system groups by facility.
     */
    @Query("""
        SELECT g FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
        AND g.isSystem = TRUE
        ORDER BY g.name ASC
    """)
    fun findSystemGroupsByFacility(@Param("facilityId") facilityId: UUID): List<FacilityEmployeeGroup>

    /**
     * Find custom groups by facility.
     */
    @Query("""
        SELECT g FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
        AND g.isSystem = FALSE
        ORDER BY g.name ASC
    """)
    fun findCustomGroupsByFacility(@Param("facilityId") facilityId: UUID): List<FacilityEmployeeGroup>

    /**
     * Count groups by facility.
     */
    @Query("""
        SELECT COUNT(g) FROM FacilityEmployeeGroup g
        WHERE g.facility.id = :facilityId
    """)
    fun countByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Find groups by employee ID (for finding which groups an employee belongs to).
     */
    @Query("""
        SELECT g FROM FacilityEmployeeGroup g
        JOIN g.facility.id fid
        WHERE EXISTS (
            SELECT 1 FROM FacilityEmployee e
            JOIN e.groups eg
            WHERE e.id = :employeeId
            AND eg.id = g.id
        )
    """)
    fun findByEmployeeId(@Param("employeeId") employeeId: UUID): List<FacilityEmployeeGroup>
}
