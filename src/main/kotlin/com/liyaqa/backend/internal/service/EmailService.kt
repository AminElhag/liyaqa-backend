package com.liyaqa.backend.internal.service

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.domain.employee.Permission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Email service for transactional notifications.
 * 
 * This service handles all email communications for our internal team.
 * In production, this would integrate with SendGrid or AWS SES.
 * 
 * TODO: Implement with proper email provider integration and templates
 */
@Service
class EmailService {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun sendWelcomeEmail(email: String, name: String, temporaryPassword: String) {
        // TODO: Implement with proper email template
        logger.info("Email: Welcome email sent to $email")
    }
    
    fun sendPermissionChangeNotification(
        email: String,
        name: String,
        added: Set<Permission>,
        removed: Set<Permission>
    ) {
        // TODO: Implement
        logger.info("Email: Permission change notification sent to $email")
    }
    
    fun sendPasswordChangeConfirmation(email: String, name: String) {
        // TODO: Implement
        logger.info("Email: Password change confirmation sent to $email")
    }
    
    fun sendEmployeeTerminationNotification(email: String, name: String, terminatedBy: String) {
        // TODO: Implement
        logger.info("Email: Termination notification sent to $email")
    }
    
    fun sendLoginNotification(email: String, name: String, ipAddress: String, timestamp: Instant) {
        // TODO: Implement for high-privilege accounts
        logger.info("Email: Login notification sent to $email from $ipAddress")
    }
    
    fun sendAccountLockNotification(email: String, name: String, unlockTime: Instant?) {
        // TODO: Implement
        logger.warn("Email: Account lock notification sent to $email")
    }
    
    fun sendSecurityWarning(email: String, message: String) {
        // TODO: Implement
        logger.warn("Email: Security warning sent to $email: $message")
    }
    
    fun sendSecurityAlert(subject: String, message: String) {
        // TODO: Send to security team distribution list
        logger.error("Email: SECURITY ALERT - $subject: $message")
    }
    
    fun sendPasswordResetEmail(email: String, name: String, resetToken: String) {
        // TODO: Implement with secure link generation
        logger.info("Email: Password reset sent to $email")
    }
}

/**
 * Session management service for tracking active sessions.
 * 
 * This service manages user sessions beyond simple JWT validation,
 * enabling features like session revocation and concurrent session limits.
 * 
 * TODO: Implement with Redis for distributed session storage
 */
@Service
class SessionService {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // In-memory storage for development - use Redis in production
    private val sessions = mutableMapOf<String, Session>()
    
    data class Session(
        val id: String,
        val employeeId: UUID,
        val refreshToken: String,
        val ipAddress: String,
        val userAgent: String?,
        val createdAt: Instant,
        val lastActivityAt: Instant,
        var refreshTokenUsed: Boolean = false
    ) {
        fun isActive(): Boolean = lastActivityAt.isAfter(
            Instant.now().minus(8, ChronoUnit.HOURS)
        )
    }
    
    fun createSession(
        employee: Employee,
        ipAddress: String,
        userAgent: String?,
        refreshToken: String
    ): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            employeeId = employee.id!!,
            refreshToken = refreshToken,
            ipAddress = ipAddress,
            userAgent = userAgent,
            createdAt = Instant.now(),
            lastActivityAt = Instant.now()
        )
        
        sessions[session.id] = session
        logger.info("Session created for ${employee.email}: ${session.id}")
        
        return session
    }
    
    fun findByRefreshToken(refreshToken: String): Session? {
        return sessions.values.find { it.refreshToken == refreshToken }
    }
    
    fun rotateRefreshToken(sessionId: String, oldToken: String, newToken: String) {
        sessions[sessionId]?.let { session ->
            session.refreshTokenUsed = true
            val newSession = session.copy(
                refreshToken = newToken,
                refreshTokenUsed = false,
                lastActivityAt = Instant.now()
            )
            sessions[sessionId] = newSession
            logger.debug("Refresh token rotated for session $sessionId")
        }
    }
    
    fun terminateSession(sessionId: String) {
        sessions.remove(sessionId)
        logger.info("Session terminated: $sessionId")
    }
    
    fun revokeAllSessions(employeeId: UUID) {
        val revoked = sessions.values
            .filter { it.employeeId == employeeId }
            .map { it.id }
        
        revoked.forEach { sessions.remove(it) }
        logger.warn("All sessions revoked for employee $employeeId (${revoked.size} sessions)")
    }
    
    fun hasLoginFromIp(employeeId: UUID, ipAddress: String): Boolean {
        return sessions.values.any { 
            it.employeeId == employeeId && it.ipAddress == ipAddress 
        }
    }
}