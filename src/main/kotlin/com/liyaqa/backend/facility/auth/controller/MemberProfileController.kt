package com.liyaqa.backend.facility.auth.controller

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.domain.Member
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

/**
 * Controller for member profile management.
 *
 * Allows authenticated members to view and update their profile information.
 * Requires JWT authentication.
 */
@RestController
@RequestMapping("/api/member/profile")
class MemberProfileController(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get member profile.
     */
    @GetMapping
    fun getProfile(@RequestAttribute("memberId") memberIdStr: String): ResponseEntity<ApiResponse> {
        val memberId = UUID.fromString(memberIdStr)

        val member = memberRepository.findById(memberId).orElse(null)
            ?: return ResponseEntity.badRequest().body(
                ApiResponse.error("Member not found")
            )

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "Profile retrieved successfully",
                data = mapOf(
                    "id" to member.id.toString(),
                    "firstName" to member.firstName,
                    "lastName" to member.lastName,
                    "email" to member.email,
                    "phoneNumber" to member.phoneNumber,
                    "memberNumber" to member.memberNumber,
                    "dateOfBirth" to member.dateOfBirth?.toString(),
                    "gender" to member.gender,
                    "addressLine1" to member.addressLine1,
                    "addressLine2" to member.addressLine2,
                    "city" to member.city,
                    "postalCode" to member.postalCode,
                    "country" to member.country,
                    "emergencyContactName" to member.emergencyContactName,
                    "emergencyContactPhone" to member.emergencyContactPhone,
                    "emergencyContactRelationship" to member.emergencyContactRelationship,
                    "status" to member.status.name,
                    "emailVerified" to member.emailVerified,
                    "emailNotifications" to member.emailNotifications,
                    "smsNotifications" to member.smsNotifications,
                    "marketingConsent" to member.marketingConsent,
                    "preferredLanguage" to member.preferredLanguage,
                    "profilePictureUrl" to member.profilePictureUrl,
                    "facilityName" to member.facility.name,
                    "branchName" to member.branch.name
                )
            )
        )
    }

    /**
     * Update member profile.
     */
    @PutMapping
    fun updateProfile(
        @RequestAttribute("memberId") memberIdStr: String,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse> {
        val memberId = UUID.fromString(memberIdStr)

        val member = memberRepository.findById(memberId).orElse(null)
            ?: return ResponseEntity.badRequest().body(
                ApiResponse.error("Member not found")
            )

        // Update fields
        request.firstName?.let { member.firstName = it }
        request.lastName?.let { member.lastName = it }
        request.phoneNumber?.let { member.phoneNumber = it }
        request.dateOfBirth?.let { member.dateOfBirth = LocalDate.parse(it) }
        request.gender?.let { member.gender = it }
        request.addressLine1?.let { member.addressLine1 = it }
        request.addressLine2?.let { member.addressLine2 = it }
        request.city?.let { member.city = it }
        request.postalCode?.let { member.postalCode = it }
        request.country?.let { member.country = it }
        request.emergencyContactName?.let { member.emergencyContactName = it }
        request.emergencyContactPhone?.let { member.emergencyContactPhone = it }
        request.emergencyContactRelationship?.let { member.emergencyContactRelationship = it }

        memberRepository.save(member)

        logger.info("Profile updated for member: ${member.id}")

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "Profile updated successfully"
            )
        )
    }

    /**
     * Update member preferences.
     */
    @PutMapping("/preferences")
    fun updatePreferences(
        @RequestAttribute("memberId") memberIdStr: String,
        @Valid @RequestBody request: UpdatePreferencesRequest
    ): ResponseEntity<ApiResponse> {
        val memberId = UUID.fromString(memberIdStr)

        val member = memberRepository.findById(memberId).orElse(null)
            ?: return ResponseEntity.badRequest().body(
                ApiResponse.error("Member not found")
            )

        request.emailNotifications?.let { member.emailNotifications = it }
        request.smsNotifications?.let { member.smsNotifications = it }
        request.marketingConsent?.let { member.marketingConsent = it }
        request.preferredLanguage?.let { member.preferredLanguage = it }

        memberRepository.save(member)

        logger.info("Preferences updated for member: ${member.id}")

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "Preferences updated successfully"
            )
        )
    }

    /**
     * Change password.
     */
    @PostMapping("/change-password")
    fun changePassword(
        @RequestAttribute("memberId") memberIdStr: String,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse> {
        val memberId = UUID.fromString(memberIdStr)

        val member = memberRepository.findById(memberId).orElse(null)
            ?: return ResponseEntity.badRequest().body(
                ApiResponse.error("Member not found")
            )

        // Verify current password
        if (member.passwordHash == null || !passwordEncoder.matches(request.currentPassword, member.passwordHash)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Current password is incorrect")
            )
        }

        // Validate new password
        if (!isPasswordStrong(request.newPassword)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Password must be at least 8 characters and contain uppercase, lowercase, and numbers")
            )
        }

        // Update password
        member.passwordHash = passwordEncoder.encode(request.newPassword)
        memberRepository.save(member)

        logger.info("Password changed for member: ${member.id}")

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "Password changed successfully"
            )
        )
    }

    /**
     * Validate password strength.
     */
    private fun isPasswordStrong(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasMinLength && hasUppercase && hasLowercase && hasDigit
    }
}

// ========== Request DTOs ==========

data class UpdateProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val dateOfBirth: String?, // YYYY-MM-DD format
    val gender: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val emergencyContactRelationship: String?
)

data class UpdatePreferencesRequest(
    val emailNotifications: Boolean?,
    val smsNotifications: Boolean?,
    val marketingConsent: Boolean?,
    val preferredLanguage: String?
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    val newPassword: String
)
