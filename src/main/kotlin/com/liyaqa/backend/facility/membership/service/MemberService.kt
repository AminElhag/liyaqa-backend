package com.liyaqa.backend.facility.membership.service

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.MemberStatus
import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Service for managing members (customers).
 */
@Service
@Transactional
class MemberService(
    private val memberRepository: MemberRepository,
    private val facilityRepository: SportFacilityRepository,
    private val branchRepository: FacilityBranchRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new member.
     */
    fun createMember(request: MemberCreateRequest): MemberResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Validate branch exists and belongs to facility
        val branch = branchRepository.findById(request.branchId)
            .orElseThrow { EntityNotFoundException("Branch not found: ${request.branchId}") }

        if (branch.facility.id != facility.id) {
            throw IllegalArgumentException("Branch does not belong to the specified facility")
        }

        // Check for duplicate email (branch-scoped)
        if (memberRepository.existsByBranchIdAndEmail(request.branchId, request.email)) {
            throw IllegalArgumentException("Email '${request.email}' already exists for this branch")
        }

        // Check for duplicate member number if provided (branch-scoped)
        request.memberNumber?.let {
            if (memberRepository.existsByBranchIdAndMemberNumber(request.branchId, it)) {
                throw IllegalArgumentException("Member number '$it' already exists for this branch")
            }
        }

        // Generate member number if not provided
        val memberNumber = request.memberNumber ?: generateMemberNumber(facility.id!!)

        val member = Member(
            facility = facility,
            branch = branch,
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            phoneNumber = request.phoneNumber,
            memberNumber = memberNumber,
            dateOfBirth = request.dateOfBirth,
            gender = request.gender,
            nationalId = request.nationalId,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            postalCode = request.postalCode,
            country = request.country,
            emergencyContactName = request.emergencyContactName,
            emergencyContactPhone = request.emergencyContactPhone,
            emergencyContactRelationship = request.emergencyContactRelationship,
            bloodType = request.bloodType,
            medicalConditions = request.medicalConditions,
            allergies = request.allergies,
            medications = request.medications,
            preferredLanguage = request.preferredLanguage,
            marketingConsent = request.marketingConsent,
            smsNotifications = request.smsNotifications,
            emailNotifications = request.emailNotifications,
            notes = request.notes
        )

        member.tenantId = facility.tenantId

        val savedMember = memberRepository.save(member)

        logger.info("Member created: ${savedMember.getFullName()} (${savedMember.email}) for facility ${facility.name}")

        return MemberResponse.from(savedMember)
    }

    /**
     * Get member by ID.
     */
    @Transactional(readOnly = true)
    fun getMemberById(id: UUID): MemberResponse {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        return MemberResponse.from(member)
    }

    /**
     * Update member information.
     */
    fun updateMember(id: UUID, request: MemberUpdateRequest): MemberResponse {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        request.firstName?.let { member.firstName = it }
        request.lastName?.let { member.lastName = it }

        request.email?.let {
            if (it != member.email && memberRepository.existsByBranchIdAndEmail(member.branch.id!!, it)) {
                throw IllegalArgumentException("Email '$it' already exists for this branch")
            }
            member.email = it
        }

        request.phoneNumber?.let { member.phoneNumber = it }

        request.memberNumber?.let {
            if (it != member.memberNumber && memberRepository.existsByBranchIdAndMemberNumber(member.branch.id!!, it)) {
                throw IllegalArgumentException("Member number '$it' already exists for this branch")
            }
            member.memberNumber = it
        }

        request.dateOfBirth?.let { member.dateOfBirth = it }
        request.gender?.let { member.gender = it }
        request.nationalId?.let { member.nationalId = it }
        request.addressLine1?.let { member.addressLine1 = it }
        request.addressLine2?.let { member.addressLine2 = it }
        request.city?.let { member.city = it }
        request.postalCode?.let { member.postalCode = it }
        request.country?.let { member.country = it }
        request.emergencyContactName?.let { member.emergencyContactName = it }
        request.emergencyContactPhone?.let { member.emergencyContactPhone = it }
        request.emergencyContactRelationship?.let { member.emergencyContactRelationship = it }
        request.bloodType?.let { member.bloodType = it }
        request.medicalConditions?.let { member.medicalConditions = it }
        request.allergies?.let { member.allergies = it }
        request.medications?.let { member.medications = it }
        request.preferredLanguage?.let { member.preferredLanguage = it }
        request.marketingConsent?.let { member.marketingConsent = it }
        request.smsNotifications?.let { member.smsNotifications = it }
        request.emailNotifications?.let { member.emailNotifications = it }
        request.notes?.let { member.notes = it }
        request.profilePictureUrl?.let { member.profilePictureUrl = it }

        val savedMember = memberRepository.save(member)

        logger.info("Member updated: ${savedMember.getFullName()}")

        return MemberResponse.from(savedMember)
    }

    /**
     * Delete member.
     */
    fun deleteMember(id: UUID) {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        memberRepository.delete(member)

        logger.warn("Member deleted: ${member.getFullName()} (${member.email})")
    }

    /**
     * Suspend member.
     */
    fun suspendMember(id: UUID, request: SuspendMemberRequest): MemberResponse {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        if (member.status == MemberStatus.SUSPENDED) {
            throw IllegalStateException("Member is already suspended")
        }

        member.suspend(request.reason)
        val savedMember = memberRepository.save(member)

        logger.warn("Member suspended: ${savedMember.getFullName()} - Reason: ${request.reason}")

        return MemberResponse.from(savedMember)
    }

    /**
     * Reactivate member.
     */
    fun reactivateMember(id: UUID): MemberResponse {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        if (member.status != MemberStatus.SUSPENDED) {
            throw IllegalStateException("Only suspended members can be reactivated")
        }

        member.reactivate()
        val savedMember = memberRepository.save(member)

        logger.info("Member reactivated: ${savedMember.getFullName()}")

        return MemberResponse.from(savedMember)
    }

    /**
     * Ban member.
     */
    fun banMember(id: UUID, request: BanMemberRequest): MemberResponse {
        val member = memberRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Member not found: $id") }

        if (member.status == MemberStatus.BANNED) {
            throw IllegalStateException("Member is already banned")
        }

        member.ban(request.reason)
        val savedMember = memberRepository.save(member)

        logger.warn("Member banned: ${savedMember.getFullName()} - Reason: ${request.reason}")

        return MemberResponse.from(savedMember)
    }

    /**
     * Search members.
     */
    @Transactional(readOnly = true)
    fun searchMembers(
        searchTerm: String?,
        status: MemberStatus?,
        facilityId: UUID?,
        pageable: Pageable
    ): Page<MemberBasicResponse> {
        val page = memberRepository.searchMembers(searchTerm, status, facilityId, pageable)
        return page.map { MemberBasicResponse.from(it) }
    }

    /**
     * Get members by facility.
     */
    @Transactional(readOnly = true)
    fun getMembersByFacility(facilityId: UUID): List<MemberResponse> {
        return memberRepository.findByFacilityId(facilityId)
            .map { MemberResponse.from(it) }
    }

    /**
     * Get active members by facility.
     */
    @Transactional(readOnly = true)
    fun getActiveMembersByFacility(facilityId: UUID): List<MemberResponse> {
        return memberRepository.findActiveByFacility(facilityId)
            .map { MemberResponse.from(it) }
    }

    /**
     * Find member by email.
     */
    @Transactional(readOnly = true)
    fun findMemberByEmail(facilityId: UUID, email: String): MemberResponse? {
        return memberRepository.findByFacilityIdAndEmail(facilityId, email)
            ?.let { MemberResponse.from(it) }
    }

    /**
     * Find member by member number.
     */
    @Transactional(readOnly = true)
    fun findMemberByMemberNumber(facilityId: UUID, memberNumber: String): MemberResponse? {
        return memberRepository.findByFacilityIdAndMemberNumber(facilityId, memberNumber)
            ?.let { MemberResponse.from(it) }
    }

    // ===== Branch-Level Operations =====

    /**
     * Get members by branch.
     */
    @Transactional(readOnly = true)
    fun getMembersByBranch(branchId: UUID): List<MemberResponse> {
        return memberRepository.findByBranchId(branchId)
            .map { MemberResponse.from(it) }
    }

    /**
     * Get active members by branch.
     */
    @Transactional(readOnly = true)
    fun getActiveMembersByBranch(branchId: UUID): List<MemberResponse> {
        return memberRepository.findActiveByBranchId(branchId)
            .map { MemberResponse.from(it) }
    }

    /**
     * Get members by branch and status.
     */
    @Transactional(readOnly = true)
    fun getMembersByBranchAndStatus(branchId: UUID, status: MemberStatus): List<MemberResponse> {
        return memberRepository.findByBranchIdAndStatus(branchId, status)
            .map { MemberResponse.from(it) }
    }

    /**
     * Search members within a branch.
     */
    @Transactional(readOnly = true)
    fun searchMembersByBranch(
        branchId: UUID,
        searchTerm: String?,
        status: MemberStatus?,
        pageable: Pageable
    ): Page<MemberBasicResponse> {
        val page = memberRepository.searchMembersByBranch(branchId, searchTerm, status, pageable)
        return page.map { MemberBasicResponse.from(it) }
    }

    /**
     * Find member by email in branch.
     */
    @Transactional(readOnly = true)
    fun findMemberByEmailInBranch(branchId: UUID, email: String): MemberResponse? {
        return memberRepository.findByBranchIdAndEmail(branchId, email)
            ?.let { MemberResponse.from(it) }
    }

    /**
     * Find member by member number in branch.
     */
    @Transactional(readOnly = true)
    fun findMemberByMemberNumberInBranch(branchId: UUID, memberNumber: String): MemberResponse? {
        return memberRepository.findByBranchIdAndMemberNumber(branchId, memberNumber)
            ?.let { MemberResponse.from(it) }
    }

    /**
     * Count members by branch.
     */
    @Transactional(readOnly = true)
    fun countMembersByBranch(branchId: UUID): Long {
        return memberRepository.countByBranchId(branchId)
    }

    /**
     * Count active members by branch.
     */
    @Transactional(readOnly = true)
    fun countActiveMembersByBranch(branchId: UUID): Long {
        return memberRepository.countActiveByBranchId(branchId)
    }

    /**
     * Count members by branch and status.
     */
    @Transactional(readOnly = true)
    fun countMembersByBranchAndStatus(branchId: UUID, status: MemberStatus): Long {
        return memberRepository.countByBranchIdAndStatus(branchId, status)
    }

    /**
     * Generate unique member number.
     */
    private fun generateMemberNumber(facilityId: UUID): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "MEM-${timestamp}-${random}"
    }
}
