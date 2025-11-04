package com.liyaqa.backend.facility.auth.service

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.MemberStatus
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.shared.config.EmailService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * Member authentication service for customer self-service.
 *
 * Handles member registration, login, password reset, and email verification.
 * Separate from internal employee authentication for better security and UX.
 *
 * Security Features:
 * - BCrypt password hashing
 * - JWT token generation
 * - Email verification
 * - Password reset flow
 * - Account lockout after failed attempts
 * - Token expiration
 */
@Service
@Transactional
class MemberAuthenticationService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val memberAuthEmailService: MemberAuthEmailService,
    @Value("\${liyaqa.security.jwt.secret}")
    private val jwtSecret: String,
    @Value("\${liyaqa.security.jwt.expiration}")
    private val jwtExpiration: Long,
    @Value("\${liyaqa.app.base-url}")
    private val baseUrl: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val jwtKey: SecretKey by lazy { Keys.hmacShaKeyFor(jwtSecret.toByteArray()) }

    /**
     * Register a new member account.
     */
    fun register(request: MemberRegistrationRequest): MemberRegistrationResult {
        logger.info("Registering new member: ${request.email} for branch ${request.branchId}")

        // Check if email already exists for this branch
        if (memberRepository.existsByBranchIdAndEmail(request.branchId, request.email)) {
            return MemberRegistrationResult.Failure("Email already registered")
        }

        // Validate password strength
        if (!isPasswordStrong(request.password)) {
            return MemberRegistrationResult.Failure(
                "Password must be at least 8 characters and contain uppercase, lowercase, and numbers"
            )
        }

        // Create member
        val member = Member(
            facility = request.branch.facility,
            branch = request.branch,
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            phoneNumber = request.phoneNumber,
            passwordHash = passwordEncoder.encode(request.password),
            emailVerified = false,
            status = MemberStatus.ACTIVE
        )
        member.tenantId = request.branch.tenantId

        // Generate email verification token
        val verificationToken = member.generateEmailVerificationToken()

        memberRepository.save(member)

        // Send verification email
        memberAuthEmailService.sendVerificationEmail(member, verificationToken)

        logger.info("Member registered successfully: ${member.id}")

        return MemberRegistrationResult.Success(
            memberId = member.id!!,
            email = member.email,
            requiresEmailVerification = true
        )
    }

    /**
     * Authenticate member and generate JWT token.
     */
    fun login(request: MemberLoginRequest): MemberLoginResult {
        logger.info("Member login attempt: ${request.email}")

        // Find member by email and branch
        val member = memberRepository.findByBranchIdAndEmail(request.branchId, request.email)
            ?: return MemberLoginResult.Failure("Invalid email or password")

        // Check if account is locked
        if (member.isLocked()) {
            return MemberLoginResult.Failure("Account is locked. Please try again later.")
        }

        // Check if account is active
        if (!member.isActive()) {
            return MemberLoginResult.Failure("Account is not active. Please contact support.")
        }

        // Verify password
        if (member.passwordHash == null || !passwordEncoder.matches(request.password, member.passwordHash)) {
            member.recordFailedLogin()
            memberRepository.save(member)
            return MemberLoginResult.Failure("Invalid email or password")
        }

        // Check if email verification is required
        if (member.requiresEmailVerification() && !request.allowUnverified) {
            return MemberLoginResult.Failure("Email verification required")
        }

        // Record successful login
        member.recordSuccessfulLogin()
        memberRepository.save(member)

        // Generate JWT token
        val token = generateToken(member)

        logger.info("Member logged in successfully: ${member.id}")

        return MemberLoginResult.Success(
            token = token,
            memberId = member.id!!,
            email = member.email,
            fullName = member.getFullName(),
            emailVerified = member.emailVerified
        )
    }

    /**
     * Verify email with token.
     */
    fun verifyEmail(token: String): Boolean {
        // Find member by verification token
        val members = memberRepository.findAll().filter {
            it.emailVerificationToken == token
        }

        if (members.isEmpty()) {
            logger.warn("Invalid email verification token")
            return false
        }

        val member = members.first()

        if (member.verifyEmail(token)) {
            memberRepository.save(member)
            logger.info("Email verified successfully for member: ${member.id}")
            return true
        }

        return false
    }

    /**
     * Request password reset.
     */
    fun requestPasswordReset(email: String, branchId: UUID): Boolean {
        val member = memberRepository.findByBranchIdAndEmail(branchId, email)
            ?: return false

        // Generate reset token
        val resetToken = member.generatePasswordResetToken()
        memberRepository.save(member)

        // Send reset email
        memberAuthEmailService.sendPasswordResetEmail(member, resetToken)

        logger.info("Password reset requested for member: ${member.id}")

        return true
    }

    /**
     * Reset password with token.
     */
    fun resetPassword(token: String, newPassword: String): Boolean {
        // Find member by reset token
        val members = memberRepository.findAll().filter {
            it.passwordResetToken == token
        }

        if (members.isEmpty()) {
            logger.warn("Invalid password reset token")
            return false
        }

        val member = members.first()

        // Validate token
        if (!member.isPasswordResetTokenValid(token)) {
            logger.warn("Expired password reset token for member: ${member.id}")
            return false
        }

        // Validate new password
        if (!isPasswordStrong(newPassword)) {
            return false
        }

        // Update password
        member.passwordHash = passwordEncoder.encode(newPassword)
        member.clearPasswordResetToken()
        memberRepository.save(member)

        logger.info("Password reset successfully for member: ${member.id}")

        return true
    }

    /**
     * Resend verification email.
     */
    fun resendVerificationEmail(email: String, branchId: UUID): Boolean {
        val member = memberRepository.findByBranchIdAndEmail(branchId, email)
            ?: return false

        if (member.emailVerified) {
            return false // Already verified
        }

        // Generate new token
        val verificationToken = member.generateEmailVerificationToken()
        memberRepository.save(member)

        // Send email
        memberAuthEmailService.sendVerificationEmail(member, verificationToken)

        return true
    }

    /**
     * Generate JWT token for member.
     */
    private fun generateToken(member: Member): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setSubject(member.id.toString())
            .claim("email", member.email)
            .claim("type", "MEMBER")
            .claim("facilityId", member.facility.id.toString())
            .claim("branchId", member.branch.id.toString())
            .claim("tenantId", member.tenantId)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(jwtKey, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Validate JWT token.
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get claims from JWT token.
     */
    fun getClaimsFromToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate password strength.
     */
    private fun isPasswordStrong(password: String): Boolean {
        // At least 8 characters, one uppercase, one lowercase, one digit
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasMinLength && hasUppercase && hasLowercase && hasDigit
    }
}

/**
 * Member registration request.
 */
data class MemberRegistrationRequest(
    val branch: FacilityBranch,
    val branchId: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String
)

/**
 * Member registration result.
 */
sealed class MemberRegistrationResult {
    data class Success(
        val memberId: UUID,
        val email: String,
        val requiresEmailVerification: Boolean
    ) : MemberRegistrationResult()

    data class Failure(val message: String) : MemberRegistrationResult()
}

/**
 * Member login request.
 */
data class MemberLoginRequest(
    val branchId: UUID,
    val email: String,
    val password: String,
    val allowUnverified: Boolean = false
)

/**
 * Member login result.
 */
sealed class MemberLoginResult {
    data class Success(
        val token: String,
        val memberId: UUID,
        val email: String,
        val fullName: String,
        val emailVerified: Boolean
    ) : MemberLoginResult()

    data class Failure(val message: String) : MemberLoginResult()
}
