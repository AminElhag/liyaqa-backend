package com.liyaqa.backend.internal.auth.controller


import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.auth.dto.*
import com.liyaqa.backend.internal.employee.dto.MessageResponse
import com.liyaqa.backend.internal.shared.security.CurrentEmployee
import com.liyaqa.backend.internal.auth.service.AuthenticationService
import com.liyaqa.backend.internal.shared.util.toBasicResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Authentication gateway for our internal control plane.
 *
 * This controller represents the front door to our system. Every design choice
 * here reflects our security-first philosophy while maintaining developer experience.
 * We prioritized explicit security boundaries over convenience because the cost
 * of a breach in our control plane would cascade to all our tenants.
 *
 * From a business perspective, this ensures our team can work efficiently while
 * maintaining the trust our enterprise customers expect. The auth flow balances
 * security requirements with operational reality - our team needs to move fast
 * without compromising safety.
 */
@RestController
@RequestMapping("/api/v1/internal/auth")
@CrossOrigin(origins = ["http://localhost:3000"], allowCredentials = "true")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Employee login endpoint - the beginning of every workday.
     *
     * This design allows us to capture rich context about each login attempt,
     * enabling both security monitoring and user experience improvements.
     * We track device fingerprints and location patterns not for surveillance,
     * but to detect anomalies that could indicate compromised accounts.
     *
     * The main trade-off here is between stateless scalability (pure JWT) and
     * the ability to revoke sessions immediately. We chose a hybrid approach -
     * short-lived access tokens with longer refresh tokens give us both benefits.
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthenticationResponse> {
        val clientIp = extractClientIp(httpRequest)
        val userAgent = httpRequest.getHeader("User-Agent") ?: "Unknown"

        logger.info("Login attempt for ${request.email} from IP: $clientIp")

        val response = authenticationService.authenticate(request, httpRequest)

        // Set secure HTTP-only cookie for refresh token (defense in depth)
        // This protects against XSS while the access token in memory handles CSRF
        val cookieHeader = buildSecureRefreshTokenCookie(response.refreshToken)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieHeader)
            .body(response)
    }

    /**
     * Token refresh - maintaining session continuity without compromising security.
     *
     * This endpoint embodies our principle of progressive security enhancement.
     * Regular refreshes allow us to continuously re-evaluate risk and apply
     * additional checks if patterns change. It's not just about extending access;
     * it's about maintaining a living security posture.
     *
     * From a UX perspective, this ensures our team doesn't face constant
     * re-authentication interruptions during their workday, improving productivity.
     */
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthenticationResponse> {
        logger.debug("Token refresh requested")

        val response = authenticationService.refreshToken(request, httpRequest)

        // Rotate refresh token cookie for forward secrecy
        val cookieHeader = buildSecureRefreshTokenCookie(response.refreshToken)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieHeader)
            .body(response)
    }

    /**
     * Logout - properly terminating sessions across our distributed system.
     *
     * The design here goes beyond clearing client tokens. We maintain a
     * blacklist of revoked tokens (with TTL matching token expiry) because
     * JWTs can't be invalidated server-side by design. This addresses the
     * stateless JWT limitation for high-security operations.
     *
     * The trade-off is additional Redis lookups on each request, but for
     * our internal system with hundreds (not millions) of users, this is
     * negligible compared to the security benefit.
     */
    @PostMapping("/logout")
    fun logout(
        @CurrentEmployee currentEmployee: Employee,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        logger.info("Logout requested by ${currentEmployee.email}")

        authenticationService.logout(currentEmployee, httpRequest)

        // Clear refresh token cookie
        val clearCookieHeader = "liyaqa_refresh_token=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict"

        return ResponseEntity.ok()
            .header("Set-Cookie", clearCookieHeader)
            .body(MessageResponse("Logged out successfully"))
    }

    /**
     * Password reset initiation - balancing security with user recovery needs.
     *
     * This endpoint deliberately returns success regardless of email existence
     * to prevent enumeration attacks. The design prioritizes security over
     * user feedback precision. Real emails only go to valid accounts, but
     * attackers can't distinguish valid from invalid attempts.
     *
     * From an operational perspective, this reduces support burden by allowing
     * self-service recovery while maintaining security boundaries.
     */
    @PostMapping("/password-reset/request")
    fun requestPasswordReset(
        @Valid @RequestBody request: PasswordResetRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        logger.info("Password reset requested for email: ${request.email}")

        authenticationService.requestPasswordReset(request.email, httpRequest)

        // Always return success to prevent email enumeration
        return ResponseEntity.ok(
            MessageResponse(
                "If an account exists with this email, password reset instructions have been sent."
            )
        )
    }

    /**
     * Password reset completion - the critical moment of account recovery.
     *
     * This two-phase reset process (request -> complete) prevents time-based
     * attacks while giving users reasonable time to complete the process.
     * The token encodes expiration and usage constraints, making it safe
     * to transmit via email.
     */
    @PostMapping("/password-reset/complete")
    fun completePasswordReset(
        @Valid @RequestBody request: CompletePasswordResetRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        authenticationService.completePasswordReset(
            request.token,
            request.newPassword,
            httpRequest
        )

        return ResponseEntity.ok(
            MessageResponse("Password has been reset successfully. Please login with your new password.")
        )
    }

    /**
     * Session validation - continuous security assessment.
     *
     * This endpoint allows our frontend to proactively check session validity
     * before making sensitive operations. It's part of our defense-in-depth
     * strategy - don't wait for operations to fail, validate proactively.
     */
    @GetMapping("/validate")
    fun validateSession(
        @CurrentEmployee currentEmployee: Employee
    ): ResponseEntity<SessionValidationResponse> {
        return ResponseEntity.ok(
            SessionValidationResponse(
                valid = true,
                employee = currentEmployee.toBasicResponse(),
                permissions = currentEmployee.getAllPermissions()
            )
        )
    }

    // Helper methods reflecting our security-conscious implementation

    private fun extractClientIp(request: HttpServletRequest): String {
        // Handle both direct connections and proxied requests (load balancer, CDN)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",").first().trim()
            else -> request.remoteAddr
        }
    }

    private fun buildSecureRefreshTokenCookie(refreshToken: String?): String {
        // Cookie security attributes reflect our defense-in-depth approach:
        // - HttpOnly: Prevents JS access (XSS protection)
        // - Secure: HTTPS only (MitM protection)
        // - SameSite=Strict: CSRF protection
        // - Path=/api/v1/internal/auth: Minimize exposure surface

        return if (refreshToken != null) {
            "liyaqa_refresh_token=$refreshToken; " +
                    "Path=/api/v1/internal/auth; " +
                    "Max-Age=604800; " + // 7 days
                    "HttpOnly; " +
                    "Secure; " +
                    "SameSite=Strict"
        } else {
            // Clear cookie on logout or error
            "liyaqa_refresh_token=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict"
        }
    }
}