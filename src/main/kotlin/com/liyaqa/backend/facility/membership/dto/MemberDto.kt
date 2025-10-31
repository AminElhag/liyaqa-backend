package com.liyaqa.backend.facility.membership.dto

import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.MemberStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Request to create a new member.
 */
data class MemberCreateRequest(
    @field:NotBlank(message = "Facility ID is required")
    val facilityId: UUID,

    @field:NotBlank(message = "Branch ID is required")
    val branchId: UUID,

    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+?[0-9\\-\\s()]+$", message = "Invalid phone number format")
    val phoneNumber: String,

    val memberNumber: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val nationalId: String? = null,

    // Address
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    // Emergency contact
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelationship: String? = null,

    // Medical
    val bloodType: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val medications: String? = null,

    // Preferences
    val preferredLanguage: String = "en",
    val marketingConsent: Boolean = false,
    val smsNotifications: Boolean = true,
    val emailNotifications: Boolean = true,

    val notes: String? = null
)

/**
 * Request to update member information.
 */
data class MemberUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val memberNumber: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val nationalId: String? = null,

    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelationship: String? = null,

    val bloodType: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val medications: String? = null,

    val preferredLanguage: String? = null,
    val marketingConsent: Boolean? = null,
    val smsNotifications: Boolean? = null,
    val emailNotifications: Boolean? = null,

    val notes: String? = null,
    val profilePictureUrl: String? = null
)

/**
 * Response DTO for member with full details.
 */
data class MemberResponse(
    val id: UUID,
    val facilityId: UUID,
    val facilityName: String,
    val branchId: UUID,
    val branchName: String,
    val tenantId: String,

    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,

    val memberNumber: String?,
    val dateOfBirth: LocalDate?,
    val age: Int?,
    val gender: String?,
    val nationalId: String?,

    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,

    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val emergencyContactRelationship: String?,

    val bloodType: String?,
    val medicalConditions: String?,
    val allergies: String?,
    val medications: String?,

    val status: MemberStatus,
    val statusReason: String?,
    val statusChangedAt: Instant?,

    val preferredLanguage: String,
    val marketingConsent: Boolean,
    val smsNotifications: Boolean,
    val emailNotifications: Boolean,

    val notes: String?,
    val profilePictureUrl: String?,

    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                id = member.id!!,
                facilityId = member.facility.id!!,
                facilityName = member.facility.name,
                branchId = member.branch.id!!,
                branchName = member.branch.name,
                tenantId = member.tenantId,
                firstName = member.firstName,
                lastName = member.lastName,
                fullName = member.getFullName(),
                email = member.email,
                phoneNumber = member.phoneNumber,
                memberNumber = member.memberNumber,
                dateOfBirth = member.dateOfBirth,
                age = member.calculateAge(),
                gender = member.gender,
                nationalId = member.nationalId,
                addressLine1 = member.addressLine1,
                addressLine2 = member.addressLine2,
                city = member.city,
                postalCode = member.postalCode,
                country = member.country,
                emergencyContactName = member.emergencyContactName,
                emergencyContactPhone = member.emergencyContactPhone,
                emergencyContactRelationship = member.emergencyContactRelationship,
                bloodType = member.bloodType,
                medicalConditions = member.medicalConditions,
                allergies = member.allergies,
                medications = member.medications,
                status = member.status,
                statusReason = member.statusReason,
                statusChangedAt = member.statusChangedAt,
                preferredLanguage = member.preferredLanguage,
                marketingConsent = member.marketingConsent,
                smsNotifications = member.smsNotifications,
                emailNotifications = member.emailNotifications,
                notes = member.notes,
                profilePictureUrl = member.profilePictureUrl,
                createdAt = member.createdAt,
                updatedAt = member.updatedAt
            )
        }
    }
}

/**
 * Response DTO for member with basic information (for lists).
 */
data class MemberBasicResponse(
    val id: UUID,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val memberNumber: String?,
    val branchId: UUID,
    val branchName: String,
    val status: MemberStatus,
    val createdAt: Instant
) {
    companion object {
        fun from(member: Member): MemberBasicResponse {
            return MemberBasicResponse(
                id = member.id!!,
                fullName = member.getFullName(),
                email = member.email,
                phoneNumber = member.phoneNumber,
                memberNumber = member.memberNumber,
                branchId = member.branch.id!!,
                branchName = member.branch.name,
                status = member.status,
                createdAt = member.createdAt
            )
        }
    }
}

/**
 * Request to suspend member.
 */
data class SuspendMemberRequest(
    @field:NotBlank(message = "Suspension reason is required")
    val reason: String
)

/**
 * Request to ban member.
 */
data class BanMemberRequest(
    @field:NotBlank(message = "Ban reason is required")
    val reason: String
)
