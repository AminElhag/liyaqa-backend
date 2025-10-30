package com.liyaqa.backend.facility.membership.service

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.MemberStatus
import com.liyaqa.backend.facility.membership.dto.*
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
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
    private val facilityRepository: SportFacilityRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new member.
     */
    fun createMember(request: MemberCreateRequest): MemberResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Check for duplicate email
        if (memberRepository.existsByFacilityIdAndEmail(request.facilityId, request.email)) {
            throw IllegalArgumentException("Email '${request.email}' already exists for this facility")
        }

        // Check for duplicate member number if provided
        request.memberNumber?.let {
            if (memberRepository.existsByFacilityIdAndMemberNumber(request.facilityId, it)) {
                throw IllegalArgumentException("Member number '$it' already exists for this facility")
            }
        }

        // Generate member number if not provided
        val memberNumber = request.memberNumber ?: generateMemberNumber(facility.id!!)

        val member = Member(
            facility = facility,
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
            if (it != member.email && memberRepository.existsByFacilityIdAndEmail(member.facility.id!!, it)) {
                throw IllegalArgumentException("Email '$it' already exists for this facility")
            }
            member.email = it
        }

        request.phoneNumber?.let { member.phoneNumber = it }

        request.memberNumber?.let {
            if (it != member.memberNumber && memberRepository.existsByFacilityIdAndMemberNumber(member.facility.id!!, it)) {
                throw IllegalArgumentException("Member number '$it' already exists for this facility")
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

    /**
     * Generate unique member number.
     */
    private fun generateMemberNumber(facilityId: UUID): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "MEM-${timestamp}-${random}"
    }
}
