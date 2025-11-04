package com.liyaqa.backend.facility.membership.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.SportFacility
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate

/**
 * Member (customer) who books and uses the sports facility.
 *
 * Members are the customers of the facility - they can book courts,
 * join sessions, purchase memberships, etc.
 *
 * Key Features:
 * - Belongs to a specific facility (multi-tenancy)
 * - Personal information for contact and identification
 * - Emergency contact details
 * - Medical information for safety
 * - Status management (active, suspended, banned)
 * - Membership relationship tracking
 */
@Entity
@Table(
    name = "members",
    indexes = [
        Index(name = "idx_member_facility", columnList = "facility_id"),
        Index(name = "idx_member_branch", columnList = "branch_id"),
        Index(name = "idx_member_email", columnList = "email"),
        Index(name = "idx_member_phone", columnList = "phone_number"),
        Index(name = "idx_member_status", columnList = "status"),
        Index(name = "idx_member_member_number", columnList = "member_number"),
        Index(name = "idx_member_tenant", columnList = "tenant_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_branch_member_email", columnNames = ["branch_id", "email"]),
        UniqueConstraint(name = "uk_branch_member_number", columnNames = ["branch_id", "member_number"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Member(
    // Facility this member belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // Branch this member belongs to (branch-level scoping)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // === Basic Information ===
    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(nullable = false)
    var email: String,

    @Column(name = "phone_number", length = 50, nullable = false)
    var phoneNumber: String,

    // === Identification ===
    @Column(name = "member_number", length = 50, unique = true)
    var memberNumber: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,

    @Column(length = 10)
    var gender: String? = null, // MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY

    @Column(name = "national_id", length = 100)
    var nationalId: String? = null,

    // === Address ===
    @Column(name = "address_line1")
    var addressLine1: String? = null,

    @Column(name = "address_line2")
    var addressLine2: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null,

    @Column(length = 100)
    var country: String? = null,

    // === Emergency Contact ===
    @Column(name = "emergency_contact_name")
    var emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone", length = 50)
    var emergencyContactPhone: String? = null,

    @Column(name = "emergency_contact_relationship", length = 50)
    var emergencyContactRelationship: String? = null,

    // === Medical Information ===
    @Column(name = "blood_type", length = 10)
    var bloodType: String? = null,

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    var medicalConditions: String? = null,

    @Column(name = "allergies", columnDefinition = "TEXT")
    var allergies: String? = null,

    @Column(name = "medications", columnDefinition = "TEXT")
    var medications: String? = null,

    // === Status ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: MemberStatus = MemberStatus.ACTIVE,

    @Column(name = "status_reason", columnDefinition = "TEXT")
    var statusReason: String? = null,

    @Column(name = "status_changed_at")
    var statusChangedAt: Instant? = null,

    // === Preferences ===
    @Column(name = "preferred_language", length = 10)
    var preferredLanguage: String = "en",

    @Column(name = "marketing_consent")
    var marketingConsent: Boolean = false,

    @Column(name = "sms_notifications")
    var smsNotifications: Boolean = true,

    @Column(name = "email_notifications")
    var emailNotifications: Boolean = true,

    // === Notes ===
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    // === Profile Picture ===
    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null,

    // === Authentication ===
    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(name = "email_verified")
    var emailVerified: Boolean = false,

    @Column(name = "email_verification_token", length = 255)
    var emailVerificationToken: String? = null,

    @Column(name = "email_verification_sent_at")
    var emailVerificationSentAt: Instant? = null,

    @Column(name = "password_reset_token", length = 255)
    var passwordResetToken: String? = null,

    @Column(name = "password_reset_expires_at")
    var passwordResetExpiresAt: Instant? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "failed_login_attempts")
    var failedLoginAttempts: Int = 0,

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Get full name.
     */
    fun getFullName(): String {
        return "$firstName $lastName"
    }

    /**
     * Check if member is active.
     */
    fun isActive(): Boolean {
        return status == MemberStatus.ACTIVE
    }

    /**
     * Calculate age from date of birth.
     */
    fun calculateAge(): Int? {
        return dateOfBirth?.let {
            val today = LocalDate.now()
            var age = today.year - it.year
            if (today.monthValue < it.monthValue ||
                (today.monthValue == it.monthValue && today.dayOfMonth < it.dayOfMonth)) {
                age--
            }
            age
        }
    }

    /**
     * Suspend member.
     */
    fun suspend(reason: String) {
        this.status = MemberStatus.SUSPENDED
        this.statusReason = reason
        this.statusChangedAt = Instant.now()
    }

    /**
     * Reactivate member.
     */
    fun reactivate() {
        if (status == MemberStatus.SUSPENDED) {
            this.status = MemberStatus.ACTIVE
            this.statusReason = null
            this.statusChangedAt = Instant.now()
        }
    }

    /**
     * Ban member.
     */
    fun ban(reason: String) {
        this.status = MemberStatus.BANNED
        this.statusReason = reason
        this.statusChangedAt = Instant.now()
    }

    // === Authentication Methods ===

    /**
     * Check if account is locked.
     */
    fun isLocked(): Boolean {
        return lockedUntil != null && lockedUntil!!.isAfter(Instant.now())
    }

    /**
     * Record failed login attempt.
     */
    fun recordFailedLogin() {
        failedLoginAttempts++
        if (failedLoginAttempts >= 5) {
            // Lock account for 30 minutes after 5 failed attempts
            lockedUntil = Instant.now().plusSeconds(30 * 60)
        }
    }

    /**
     * Record successful login.
     */
    fun recordSuccessfulLogin() {
        lastLoginAt = Instant.now()
        failedLoginAttempts = 0
        lockedUntil = null
    }

    /**
     * Check if email verification is required.
     */
    fun requiresEmailVerification(): Boolean {
        return !emailVerified
    }

    /**
     * Generate email verification token.
     */
    fun generateEmailVerificationToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        emailVerificationToken = token
        emailVerificationSentAt = Instant.now()
        return token
    }

    /**
     * Verify email with token.
     */
    fun verifyEmail(token: String): Boolean {
        if (emailVerificationToken == token) {
            emailVerified = true
            emailVerificationToken = null
            emailVerificationSentAt = null
            return true
        }
        return false
    }

    /**
     * Generate password reset token.
     */
    fun generatePasswordResetToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        passwordResetToken = token
        passwordResetExpiresAt = Instant.now().plusSeconds(60 * 60) // 1 hour
        return token
    }

    /**
     * Check if password reset token is valid.
     */
    fun isPasswordResetTokenValid(token: String): Boolean {
        return passwordResetToken == token &&
                passwordResetExpiresAt != null &&
                passwordResetExpiresAt!!.isAfter(Instant.now())
    }

    /**
     * Clear password reset token.
     */
    fun clearPasswordResetToken() {
        passwordResetToken = null
        passwordResetExpiresAt = null
    }

    override fun toString(): String {
        return "Member(id=$id, name='${getFullName()}', email='$email', facility=${facility.name}, status=$status)"
    }
}

/**
 * Member status enum.
 */
enum class MemberStatus {
    ACTIVE,      // Member is active and can use services
    SUSPENDED,   // Temporarily suspended (e.g., payment issues)
    BANNED,      // Permanently banned (e.g., misconduct)
    INACTIVE     // Voluntarily inactive (e.g., left the facility)
}
