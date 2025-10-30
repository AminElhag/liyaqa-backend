package com.liyaqa.backend.internal.auth.service

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.EmployeeStatus
import com.liyaqa.backend.internal.employee.domain.Permission
import com.liyaqa.backend.internal.auth.dto.AuthenticationResponse
import com.liyaqa.backend.internal.auth.dto.LoginRequest
import com.liyaqa.backend.internal.auth.dto.RefreshTokenRequest
import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import com.liyaqa.backend.internal.shared.security.JwtTokenProvider
import com.liyaqa.backend.internal.shared.security.PasswordEncoder
import com.liyaqa.backend.internal.shared.security.validatePasswordStrength
import com.liyaqa.backend.internal.audit.service.AuditService
import com.liyaqa.backend.internal.shared.config.EmailService
import com.liyaqa.backend.internal.shared.exception.*
import com.liyaqa.backend.internal.shared.util.toBasicResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Orchestrates authentication with defense-in-depth security.
 *
 * This design reflects our zero-trust approach - we assume breach and
 * implement multiple layers of defense. JWT tokens provide stateless auth
 * for scalability, while refresh tokens and session tracking add revocation
 * capabilities when needed.
 *
 * The trade-off here is between pure stateless auth (simpler) and the ability
 * to revoke access immediately (more complex but necessary for employee systems).
 */
@Service
@Transactional
class AuthenticationService(
    private val employeeRepository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val auditService: AuditService,
    private val emailService: EmailService,
    private val sessionService: SessionService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
        const val SUSPICIOUS_LOGIN_THRESHOLD = 3 // Failed attempts before alerting
        const val SESSION_TIMEOUT_HOURS = 8L // Work day duration
        const val REFRESH_TOKEN_DAYS = 7L
    }

    /**
     * Authenticates employee with progressive security measures.
     *
     * From a business perspective, this balances security with usability.
     * We progressively increase friction (captcha, 2FA) based on risk signals
     * rather than applying it uniformly, which would hurt productivity.
     */
    fun authenticate(
        request: LoginRequest,
        httpRequest: HttpServletRequest
    ): AuthenticationResponse {
        val startTime = System.currentTimeMillis()

        val employee = employeeRepository.findByEmail(request.email)
            ?: run {
                // Log failed attempt without revealing if user exists
                auditService.logFailedLogin(
                    email = request.email,
                    reason = "Invalid credentials",
                    ipAddress = extractIpAddress(httpRequest)
                )
                throw InvalidCredentialsException("Invalid email or password")
            }

        // Check account status
        validateAccountStatus(employee)

        // Check lockout status
        if (employee.isAccountLocked()) {
            auditService.logFailedLogin(
                email = request.email,
                reason = "Account locked",
                ipAddress = extractIpAddress(httpRequest)
            )
            throw AccountLockedException(
                "Account is locked. Please contact support or wait ${LOCKOUT_DURATION_MINUTES} minutes."
            )
        }

        // Verify password
        if (!passwordEncoder.matches(request.password, employee.passwordHash)) {
            handleFailedLogin(employee, httpRequest)
            throw InvalidCredentialsException("Invalid email or password")
        }

        // Check if password change is required
        if (employee.mustChangePassword) {
            // Issue limited token only valid for password change
            val changeToken = jwtTokenProvider.generatePasswordChangeToken(employee)
            return AuthenticationResponse(
                accessToken = changeToken,
                tokenType = "Bearer",
                expiresIn = 3600, // 1 hour to change password
                requiresPasswordChange = true,
                employee = null
            )
        }

        // Detect suspicious login patterns
        val riskScore = calculateLoginRisk(employee, httpRequest)
        if (riskScore > 0.7) {
            // In production, would require 2FA here
            auditService.logSuspiciousActivity(
                employee = employee,
                activity = "High-risk login detected",
                riskScore = riskScore
            )

            // Send alert to security team
            emailService.sendSecurityAlert(
                "High-risk login detected for ${employee.email}",
                "Risk score: $riskScore, IP: ${extractIpAddress(httpRequest)}"
            )
        }

        // Generate tokens
        val accessToken = jwtTokenProvider.generateAccessToken(employee)
        val refreshToken = jwtTokenProvider.generateRefreshToken(employee)

        // Create session for tracking
        val session = sessionService.createSession(
            employee = employee,
            ipAddress = extractIpAddress(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent"),
            refreshToken = refreshToken
        )

        // Update login metadata
        employee.lastLoginAt = Instant.now()
        employee.failedLoginAttempts = 0
        employee.lockedUntil = null
        employeeRepository.save(employee)

        // Log successful login with context
        val duration = System.currentTimeMillis() - startTime
        auditService.logSuccessfulLogin(
            employee = employee,
            ipAddress = extractIpAddress(httpRequest),
            sessionId = session.id,
            duration = duration,
            riskScore = riskScore
        )

        // Send login notification for high-privilege accounts
        if (employee.hasAnyPermission(
                Permission.SYSTEM_CONFIGURE,
                Permission.PAYMENT_PROCESS,
                Permission.SUPPORT_IMPERSONATE_TENANT
            )
        ) {
            emailService.sendLoginNotification(
                email = employee.email,
                name = employee.getFullName(),
                ipAddress = extractIpAddress(httpRequest),
                timestamp = Instant.now()
            )
        }

        return AuthenticationResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.getAccessTokenExpiration(),
            requiresPasswordChange = false,
            employee = employee.toBasicResponse()
        )
    }

    /**
     * Refreshes access token while validating session integrity.
     *
     * This design allows us to maintain long-lived sessions without
     * keeping access tokens valid for extended periods. The trade-off
     * is additional complexity in token management versus security benefits.
     */
    fun refreshToken(
        request: RefreshTokenRequest,
        httpRequest: HttpServletRequest
    ): AuthenticationResponse {
        // Validate refresh token
        val claims = jwtTokenProvider.validateRefreshToken(request.refreshToken)
            ?: throw InvalidTokenException("Invalid refresh token")

        val employeeId = UUID.fromString(claims.subject)
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { InvalidTokenException("Employee not found") }

        // Validate session
        val session = sessionService.findByRefreshToken(request.refreshToken)
            ?: throw InvalidTokenException("Session not found")

        if (!session.isActive()) {
            throw SessionExpiredException("Session has expired")
        }

        // Check for token reuse (possible attack)
        if (session.refreshTokenUsed) {
            // This refresh token was already used - possible theft
            sessionService.revokeAllSessions(employee.id!!)

            auditService.logSuspiciousActivity(
                employee = employee,
                activity = "Refresh token reuse detected - possible token theft",
                riskScore = 1.0
            )

            emailService.sendSecurityAlert(
                "Potential security breach for ${employee.email}",
                "Refresh token was reused. All sessions have been terminated."
            )

            throw SecurityBreachException("Security breach detected. All sessions terminated.")
        }

        // Generate new tokens
        val newAccessToken = jwtTokenProvider.generateAccessToken(employee)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(employee)

        // Update session with new refresh token
        sessionService.rotateRefreshToken(
            sessionId = session.id,
            oldToken = request.refreshToken,
            newToken = newRefreshToken,
        )

        auditService.logTokenRefresh(employee, session.id)

        return AuthenticationResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.getAccessTokenExpiration(),
            requiresPasswordChange = false,
            employee = employee.toBasicResponse()
        )
    }

    /**
     * Handles logout with comprehensive session cleanup.
     *
     * We treat logout seriously - it's not just clearing client tokens.
     * This ensures compromised tokens can't be used after logout,
     * addressing the stateless JWT limitation.
     */
    fun logout(
        employee: Employee,
        httpRequest: HttpServletRequest
    ): Unit {
        val sessionId = httpRequest.getAttribute("sessionId") as? String

        if (sessionId != null) {
            sessionService.terminateSession(sessionId)
        }

        // Blacklist current access token (if using token blacklist)
        val token = extractBearerToken(httpRequest)
        if (token != null) {
            jwtTokenProvider.blacklistToken(token)
        }

        auditService.logLogout(
            employee = employee,
            ipAddress = extractIpAddress(httpRequest),
            sessionId = sessionId
        )
    }

    /**
     * Initiates password reset with rate limiting and verification.
     *
     * The design here prevents enumeration attacks by always returning
     * success, while implementing rate limiting to prevent abuse.
     * Real notifications only go to valid accounts.
     */
    fun requestPasswordReset(
        email: String,
        httpRequest: HttpServletRequest
    ): Unit {
        // Always return success to prevent email enumeration
        val employee = employeeRepository.findByEmail(email)

        if (employee != null) {
            // Check rate limiting (simplified - would use Redis in production)
            val recentRequests = auditService.countRecentPasswordResets(email, 1)
            if (recentRequests > 3) {
                logger.warn("Rate limit exceeded for password reset: $email")
                return // Silently ignore but log
            }

            val resetToken = jwtTokenProvider.generatePasswordResetToken(employee)

            emailService.sendPasswordResetEmail(
                email = employee.email,
                name = employee.getFullName(),
                resetToken = resetToken
            )

            auditService.logPasswordResetRequested(
                employee = employee,
                ipAddress = extractIpAddress(httpRequest)
            )
        } else {
            // Log attempt for security monitoring
            logger.info("Password reset requested for non-existent email: $email")
        }
    }

    // Helper methods

    private fun validateAccountStatus(employee: Employee) {
        when (employee.status) {
            EmployeeStatus.SUSPENDED ->
                throw AccountSuspendedException("Account is suspended. Please contact HR.")

            EmployeeStatus.TERMINATED ->
                throw AccountTerminatedException("Account has been terminated.")

            EmployeeStatus.INACTIVE ->
                throw AccountInactiveException("Account is inactive. Please contact support.")

            EmployeeStatus.ACTIVE -> {} // OK
        }
    }

    private fun handleFailedLogin(employee: Employee, httpRequest: HttpServletRequest) {
        employee.failedLoginAttempts++

        // Alert on suspicious activity before lockout
        if (employee.failedLoginAttempts == SUSPICIOUS_LOGIN_THRESHOLD) {
            emailService.sendSecurityWarning(
                email = employee.email,
                message = "Multiple failed login attempts detected on your account"
            )
        }

        // Lock account after max attempts
        if (employee.failedLoginAttempts >= MAX_LOGIN_ATTEMPTS) {
            employee.lockedUntil = Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)

            emailService.sendAccountLockNotification(
                email = employee.email,
                name = employee.getFullName(),
                unlockTime = employee.lockedUntil
            )
        }

        employeeRepository.save(employee)

        auditService.logFailedLogin(
            email = employee.email,
            reason = "Invalid password",
            ipAddress = extractIpAddress(httpRequest),
            attemptNumber = employee.failedLoginAttempts
        )
    }

    /**
     * Calculates login risk based on behavioral patterns.
     *
     * This simple implementation demonstrates the concept.
     * Production systems would use ML models and more signals.
     */
    private fun calculateLoginRisk(
        employee: Employee,
        httpRequest: HttpServletRequest
    ): Double {
        var riskScore = 0.0

        // New IP address
        val currentIp = extractIpAddress(httpRequest)
        val isKnownIp = sessionService.hasLoginFromIp(employee.id!!, currentIp)
        if (!isKnownIp) {
            riskScore += 0.3
        }

        // Unusual login time
        val hour = Instant.now().atZone(ZoneId.systemDefault()).hour
        if (hour < 6 || hour > 22) {
            riskScore += 0.2
        }

        // Recent password change
        val daysSincePasswordChange = ChronoUnit.DAYS.between(
            employee.updatedAt,
            Instant.now()
        )
        if (daysSincePasswordChange < 1) {
            riskScore += 0.3
        }

        // High privilege account
        if (employee.hasPermission(Permission.SYSTEM_CONFIGURE)) {
            riskScore += 0.2
        }

        return minOf(riskScore, 1.0)
    }

    private fun extractIpAddress(request: HttpServletRequest): String {
        // Check for proxied requests
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (xForwardedFor != null && xForwardedFor.isNotEmpty()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            request.remoteAddr
        }
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }

    fun completePasswordReset(token: String, newPassword: String, httpRequest: HttpServletRequest) {
        val startTime = System.currentTimeMillis()

        // Validate reset token
        val claims = jwtTokenProvider.validatePasswordResetToken(token)
            ?: throw InvalidTokenException("Invalid or expired password reset token")

        // Extract employee ID from token
        val employeeId = try {
            UUID.fromString(claims.subject)
        } catch (ex: IllegalArgumentException) {
            throw InvalidTokenException("Invalid token format")
        }

        // Find employee
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { InvalidTokenException("Employee not found") }

        // Validate account is still active
        if (employee.status == EmployeeStatus.TERMINATED) {
            throw AccountTerminatedException("Cannot reset password for terminated account")
        }

        // Validate password strength
        try {
            validatePasswordStrength(newPassword)
        } catch (ex: IllegalArgumentException) {
            auditService.logFailedPasswordChange(employee, ex.message ?: "Invalid password")
            throw InvalidPasswordException(ex.message ?: "Password does not meet requirements")
        }

        // Check if password is being reused (simplified - production would check history)
        if (passwordEncoder.matches(newPassword, employee.passwordHash)) {
            auditService.logFailedPasswordChange(employee, "Password reuse attempt")
            throw PasswordReuseException("Cannot reuse your current password")
        }

        // Update password
        employee.passwordHash = passwordEncoder.encode(newPassword)

        // Reset security flags
        employee.failedLoginAttempts = 0
        employee.lockedUntil = null
        employee.mustChangePassword = false

        // Save changes
        employeeRepository.save(employee)

        // Blacklist the reset token to prevent reuse
        jwtTokenProvider.blacklistToken(token)

        // Terminate all active sessions for security
        sessionService.revokeAllSessions(employee.id!!)

        // Calculate duration for monitoring
        val duration = System.currentTimeMillis() - startTime

        // Log password reset completion
        auditService.logPasswordChanged(employee)
        logger.info("Password reset completed for ${employee.email} from ${extractIpAddress(httpRequest)} in ${duration}ms")

        // Send confirmation email
        emailService.sendPasswordChangeConfirmation(
            email = employee.email,
            name = employee.getFullName()
        )
    }

}