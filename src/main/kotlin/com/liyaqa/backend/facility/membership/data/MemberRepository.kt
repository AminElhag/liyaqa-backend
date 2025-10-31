package com.liyaqa.backend.facility.membership.data

import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.MemberStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for Member entity with comprehensive query capabilities.
 */
@Repository
interface MemberRepository : JpaRepository<Member, UUID> {

    /**
     * Find member by email and facility.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.email = :email
    """)
    fun findByFacilityIdAndEmail(
        @Param("facilityId") facilityId: UUID,
        @Param("email") email: String
    ): Member?

    /**
     * Find member by phone and facility.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.phoneNumber = :phoneNumber
    """)
    fun findByFacilityIdAndPhone(
        @Param("facilityId") facilityId: UUID,
        @Param("phoneNumber") phoneNumber: String
    ): Member?

    /**
     * Find member by member number and facility.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.memberNumber = :memberNumber
    """)
    fun findByFacilityIdAndMemberNumber(
        @Param("facilityId") facilityId: UUID,
        @Param("memberNumber") memberNumber: String
    ): Member?

    /**
     * Check if email exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
        FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.email = :email
    """)
    fun existsByFacilityIdAndEmail(
        @Param("facilityId") facilityId: UUID,
        @Param("email") email: String
    ): Boolean

    /**
     * Check if member number exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
        FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.memberNumber = :memberNumber
    """)
    fun existsByFacilityIdAndMemberNumber(
        @Param("facilityId") facilityId: UUID,
        @Param("memberNumber") memberNumber: String
    ): Boolean

    /**
     * Find members by facility.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.facility.id = :facilityId
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<Member>

    /**
     * Find active members by facility.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.status = 'ACTIVE'
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun findActiveByFacility(@Param("facilityId") facilityId: UUID): List<Member>

    /**
     * Search members with filters.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE (:searchTerm IS NULL
            OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.memberNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR m.status = :status)
        AND (:facilityId IS NULL OR m.facility.id = :facilityId)
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun searchMembers(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: MemberStatus?,
        @Param("facilityId") facilityId: UUID?,
        pageable: Pageable
    ): Page<Member>

    /**
     * Count members by facility.
     */
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.facility.id = :facilityId
    """)
    fun countByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Count active members by facility.
     */
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.facility.id = :facilityId
        AND m.status = 'ACTIVE'
    """)
    fun countActiveByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Count members by status.
     */
    fun countByStatus(status: MemberStatus): Long

    /**
     * Find members by tenant ID.
     */
    fun findByTenantId(tenantId: String): List<Member>

    // ===== Branch-Level Queries =====

    /**
     * Find member by email and branch.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        AND m.email = :email
    """)
    fun findByBranchIdAndEmail(
        @Param("branchId") branchId: UUID,
        @Param("email") email: String
    ): Member?

    /**
     * Find member by member number and branch.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        AND m.memberNumber = :memberNumber
    """)
    fun findByBranchIdAndMemberNumber(
        @Param("branchId") branchId: UUID,
        @Param("memberNumber") memberNumber: String
    ): Member?

    /**
     * Check if email exists for branch.
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
        FROM Member m
        WHERE m.branch.id = :branchId
        AND m.email = :email
    """)
    fun existsByBranchIdAndEmail(
        @Param("branchId") branchId: UUID,
        @Param("email") email: String
    ): Boolean

    /**
     * Check if member number exists for branch.
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
        FROM Member m
        WHERE m.branch.id = :branchId
        AND m.memberNumber = :memberNumber
    """)
    fun existsByBranchIdAndMemberNumber(
        @Param("branchId") branchId: UUID,
        @Param("memberNumber") memberNumber: String
    ): Boolean

    /**
     * Find members by branch.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun findByBranchId(@Param("branchId") branchId: UUID): List<Member>

    /**
     * Find active members by branch.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        AND m.status = 'ACTIVE'
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun findActiveByBranchId(@Param("branchId") branchId: UUID): List<Member>

    /**
     * Find members by branch and status.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        AND m.status = :status
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun findByBranchIdAndStatus(
        @Param("branchId") branchId: UUID,
        @Param("status") status: MemberStatus
    ): List<Member>

    /**
     * Search members within a branch with filters.
     */
    @Query("""
        SELECT m FROM Member m
        WHERE m.branch.id = :branchId
        AND (:searchTerm IS NULL
            OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.memberNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR m.status = :status)
        ORDER BY m.lastName ASC, m.firstName ASC
    """)
    fun searchMembersByBranch(
        @Param("branchId") branchId: UUID,
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: MemberStatus?,
        pageable: Pageable
    ): Page<Member>

    /**
     * Count members by branch.
     */
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.branch.id = :branchId
    """)
    fun countByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Count active members by branch.
     */
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.branch.id = :branchId
        AND m.status = 'ACTIVE'
    """)
    fun countActiveByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Count members by branch and status.
     */
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.branch.id = :branchId
        AND m.status = :status
    """)
    fun countByBranchIdAndStatus(
        @Param("branchId") branchId: UUID,
        @Param("status") status: MemberStatus
    ): Long
}
