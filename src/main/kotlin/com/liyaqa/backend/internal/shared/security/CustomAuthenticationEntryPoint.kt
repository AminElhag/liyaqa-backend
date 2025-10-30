package com.liyaqa.backend.internal.shared.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Custom authentication entry point for unauthorized requests.
 * 
 * This component handles the initial challenge when an unauthenticated
 * request attempts to access a protected resource. Instead of redirecting
 * to a login page (traditional web app behavior), we return a JSON error
 * suitable for API clients.
 * 
 * From a security perspective, we provide minimal information to avoid
 * helping attackers, while still being useful for legitimate developers
 * debugging integration issues.
 */
@Component
class CustomAuthenticationEntryPoint : AuthenticationEntryPoint {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val requestId = request.getAttribute("requestId") as? String ?: "unknown"
        val path = request.servletPath
        val method = request.method
        
        // Log the attempt for security monitoring
        logger.warn(
            "Unauthenticated access attempt - Method: {}, Path: {}, IP: {}, Request ID: {}",
            method,
            path,
            getClientIp(request),
            requestId
        )
        
        // Prepare error response
        val error = mapOf(
            "error" to "AUTHENTICATION_REQUIRED",
            "message" to "Authentication is required to access this resource",
            "path" to path,
            "timestamp" to Instant.now().toString(),
            "requestId" to requestId
        )
        
        // Set response properties
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"
        
        // Set security headers
        response.setHeader("WWW-Authenticate", "Bearer realm=\"Liyaqa Internal API\"")
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        
        // Write JSON response
        response.writer.write(objectMapper.writeValueAsString(error))
    }
    
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}

/**
 * Custom access denied handler for authorization failures.
 * 
 * This handles cases where a user is authenticated but lacks the required
 * permissions for an operation. The distinction between authentication
 * (who you are) and authorization (what you can do) is critical for
 * security monitoring and debugging.
 * 
 * From an operational perspective, these events often indicate either
 * misconfigured permissions or potential privilege escalation attempts,
 * both of which need investigation.
 */
@Component
class CustomAccessDeniedHandler : AccessDeniedHandler {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val requestId = request.getAttribute("requestId") as? String ?: "unknown"
        val employeeId = request.getAttribute("employeeId") as? String ?: "unknown"
        val path = request.servletPath
        val method = request.method
        
        // Log the authorization failure - this could indicate an attack
        logger.warn(
            "Access denied - Employee: {}, Method: {}, Path: {}, IP: {}, Request ID: {}, Reason: {}",
            employeeId,
            method,
            path,
            getClientIp(request),
            requestId,
            accessDeniedException.message
        )
        
        // For repeated failures, we might want to trigger additional security measures
        // securityEventService.recordAuthorizationFailure(employeeId, path)
        
        // Prepare error response
        val error = mapOf(
            "error" to "ACCESS_DENIED",
            "message" to "You do not have permission to access this resource",
            "path" to path,
            "timestamp" to Instant.now().toString(),
            "requestId" to requestId
        )
        
        // Set response properties
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json;charset=UTF-8"
        
        // Set security headers
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        
        // Write JSON response
        response.writer.write(objectMapper.writeValueAsString(error))
    }
    
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
