package com.liyaqa.backend.facility.auth.controller

import com.liyaqa.backend.facility.auth.service.*
import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Public API controller for member (customer) authentication.
 *
 * Provides endpoints for member self-service authentication including:
 * - Registration
 * - Login
 * - Email verification
 * - Password reset
 *
 * Note: These endpoints are publicly accessible and separate from internal employee auth.
 */
@RestController
@RequestMapping("/api/public/member/auth")
@CrossOrigin(origins = ["*"]) // Configure appropriately for production
class MemberAuthController(
    private val memberAuthenticationService: MemberAuthenticationService,
    private val memberRepository: MemberRepository,
    private val branchRepository: FacilityBranchRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Register a new member account.
     */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse> {
        logger.info("Member registration request for email: ${request.email}")

        // Find branch
        val branch = branchRepository.findById(UUID.fromString(request.branchId))
            .orElse(null)
            ?: return ResponseEntity.badRequest().body(
                ApiResponse.error("Branch not found")
            )

        val result = memberAuthenticationService.register(
            MemberRegistrationRequest(
                branch = branch,
                branchId = branch.id!!,
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phoneNumber = request.phoneNumber,
                password = request.password
            )
        )

        return when (result) {
            is MemberRegistrationResult.Success -> {
                ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(
                        message = "Registration successful. Please check your email to verify your account.",
                        data = mapOf(
                            "memberId" to result.memberId.toString(),
                            "email" to result.email,
                            "requiresEmailVerification" to result.requiresEmailVerification
                        )
                    )
                )
            }
            is MemberRegistrationResult.Failure -> {
                ResponseEntity.badRequest().body(
                    ApiResponse.error(result.message)
                )
            }
        }
    }

    /**
     * Login with email and password.
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse> {
        logger.info("Member login request for email: ${request.email}")

        val result = memberAuthenticationService.login(
            MemberLoginRequest(
                branchId = UUID.fromString(request.branchId),
                email = request.email,
                password = request.password,
                allowUnverified = false
            )
        )

        return when (result) {
            is MemberLoginResult.Success -> {
                ResponseEntity.ok(
                    ApiResponse.success(
                        message = "Login successful",
                        data = mapOf(
                            "token" to result.token,
                            "memberId" to result.memberId.toString(),
                            "email" to result.email,
                            "fullName" to result.fullName,
                            "emailVerified" to result.emailVerified
                        )
                    )
                )
            }
            is MemberLoginResult.Failure -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(result.message)
                )
            }
        }
    }

    /**
     * Verify email with token.
     */
    @GetMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<ApiResponse> {
        logger.info("Email verification request with token")

        val success = memberAuthenticationService.verifyEmail(token)

        return if (success) {
            ResponseEntity.ok(
                ApiResponse.success(
                    message = "Email verified successfully. You can now login to your account."
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid or expired verification token")
            )
        }
    }

    /**
     * Request password reset.
     */
    @PostMapping("/request-password-reset")
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<ApiResponse> {
        logger.info("Password reset request for email: ${request.email}")

        // Always return success to prevent email enumeration
        memberAuthenticationService.requestPasswordReset(
            email = request.email,
            branchId = UUID.fromString(request.branchId)
        )

        return ResponseEntity.ok(
            ApiResponse.success(
                message = "If an account exists with this email, a password reset link has been sent."
            )
        )
    }

    /**
     * Reset password with token.
     */
    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse> {
        logger.info("Password reset completion request")

        val success = memberAuthenticationService.resetPassword(
            token = request.token,
            newPassword = request.newPassword
        )

        return if (success) {
            ResponseEntity.ok(
                ApiResponse.success(
                    message = "Password reset successful. You can now login with your new password."
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid or expired reset token")
            )
        }
    }

    /**
     * Resend verification email.
     */
    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody request: ResendVerificationRequest): ResponseEntity<ApiResponse> {
        logger.info("Resend verification request for email: ${request.email}")

        val success = memberAuthenticationService.resendVerificationEmail(
            email = request.email,
            branchId = UUID.fromString(request.branchId)
        )

        return if (success) {
            ResponseEntity.ok(
                ApiResponse.success(
                    message = "Verification email sent. Please check your inbox."
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                ApiResponse.error("Unable to send verification email. Email may already be verified.")
            )
        }
    }

    /**
     * Validate JWT token (for frontend to check if token is still valid).
     */
    @GetMapping("/validate-token")
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<ApiResponse> {
        val token = authHeader.removePrefix("Bearer ").trim()

        val isValid = memberAuthenticationService.validateToken(token)

        return if (isValid) {
            val claims = memberAuthenticationService.getClaimsFromToken(token)
            ResponseEntity.ok(
                ApiResponse.success(
                    message = "Token is valid",
                    data = mapOf(
                        "memberId" to claims?.subject,
                        "email" to claims?.get("email"),
                        "facilityId" to claims?.get("facilityId"),
                        "branchId" to claims?.get("branchId")
                    )
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Invalid or expired token")
            )
        }
    }
}

// ========== Request DTOs ==========

data class RegisterRequest(
    @field:NotBlank(message = "Branch ID is required")
    val branchId: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

data class LoginRequest(
    @field:NotBlank(message = "Branch ID is required")
    val branchId: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class PasswordResetRequest(
    @field:NotBlank(message = "Branch ID is required")
    val branchId: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

data class ResendVerificationRequest(
    @field:NotBlank(message = "Branch ID is required")
    val branchId: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String
)

// ========== Response DTO ==========

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?>? = null,
    val error: String? = null
) {
    companion object {
        fun success(message: String, data: Map<String, Any?>? = null) =
            ApiResponse(success = true, message = message, data = data)

        fun error(message: String) =
            ApiResponse(success = false, message = "", error = message)
    }
}
