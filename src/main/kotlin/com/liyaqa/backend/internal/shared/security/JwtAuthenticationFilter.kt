package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.EmployeeStatus
import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.*

/**
 * JWT authentication filter - the security gateway for every request.
 * 
 * This filter embodies our zero-trust philosophy: every request must
 * prove its identity and authorization. The design balances security
 * with performance by caching employee data in the token to avoid
 * database hits on every request.
 * 
 * From an operational perspective, this filter also serves as a
 * critical observability point, logging authentication flows for
 * security monitoring and debugging.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val employeeRepository: EmployeeRepository
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "

        // Paths that bypass authentication - kept minimal for security
        private val EXCLUDED_PATHS = setOf(
            "/api/v1/internal/auth/login",
            "/api/v1/internal/auth/password-reset/request",
            "/api/v1/internal/auth/password-reset/complete",
            "/actuator/health",
            "/actuator/info"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip authentication for public endpoints
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = extractTokenFromRequest(request)

            if (token != null) {
                val claims = jwtTokenProvider.validateAccessToken(token)

                if (claims != null) {
                    // Extract employee ID and load current state
                    val employeeId = UUID.fromString(claims.subject)
                    val employee = employeeRepository.findById(employeeId).orElse(null)

                    if (employee != null && employee.status == EmployeeStatus.ACTIVE) {
                        // Create authentication with authorities from token
                        val authorities = extractAuthorities(claims)
                        val authentication = UsernamePasswordAuthenticationToken(
                            employee,  // Principal
                            null,      // Credentials (not needed after authentication)
                            authorities
                        )

                        // Add request details for audit logging
                        authentication.details = WebAuthenticationDetailsSource()
                            .buildDetails(request)

                        // Set authentication in Spring Security context
                        SecurityContextHolder.getContext().authentication = authentication

                        // Add request attributes for downstream use
                        request.setAttribute("employee", employee)
                        request.setAttribute("employeeId", employeeId)
                        request.setAttribute("sessionId", claims.id)

                        logger.debug("Successfully authenticated employee: ${employee.email}")
                    } else {
                        logger.warn("Employee not found or inactive: $employeeId")
                        sendUnauthorizedResponse(response, "Invalid or inactive account")
                        return
                    }
                } else {
                    logger.debug("Invalid or expired token")
                    sendUnauthorizedResponse(response, "Invalid or expired token")
                    return
                }
            } else {
                logger.debug("No JWT token found in request")
                sendUnauthorizedResponse(response, "Authentication required")
                return
            }
        } catch (ex: Exception) {
            logger.error("Cannot set user authentication", ex)
            sendUnauthorizedResponse(response, "Authentication failed")
            return
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Extracts JWT token from request header or cookie.
     *
     * We support both header and cookie authentication to handle
     * different client scenarios while maintaining security.
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        // First, try Authorization header (preferred)
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length)
        }

        // Fallback to cookie for web clients
        request.cookies?.find { it.name == "liyaqa_access_token" }?.let {
            return it.value
        }

        return null
    }

    /**
     * Converts JWT claims to Spring Security authorities.
     *
     * This mapping allows us to use Spring Security's method-level
     * authorization while maintaining our custom permission model.
     */
    private fun extractAuthorities(claims: Claims): List<SimpleGrantedAuthority> {
        val authorities = mutableListOf<SimpleGrantedAuthority>()

        // Add permissions as authorities
        @Suppress("UNCHECKED_CAST")
        val permissions = claims.get("permissions", List::class.java) as? List<String>
        permissions?.forEach { permission ->
            authorities.add(SimpleGrantedAuthority("PERMISSION_$permission"))
        }

        // Add groups as authorities (for role-based checks)
        @Suppress("UNCHECKED_CAST")
        val groups = claims.get("groups", List::class.java) as? List<String>
        groups?.forEach { group ->
            authorities.add(SimpleGrantedAuthority("GROUP_$group"))
        }

        return authorities
    }

    /**
     * Determines if the request should bypass authentication.
     *
     * We keep this list minimal and explicit to reduce attack surface.
     * The principle is: authenticate everything except what absolutely
     * must be public.
     */
    private fun shouldSkipAuthentication(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }

    /**
     * Sends standardized unauthorized response.
     *
     * We provide minimal information in error messages to avoid
     * information leakage while still being helpful for legitimate
     * users debugging issues.
     */
    private fun sendUnauthorizedResponse(
        response: HttpServletResponse,
        message: String
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""
            {
                "error": "UNAUTHORIZED",
                "message": "$message",
                "timestamp": "${Instant.now()}"
            }
        """.trimIndent())
    }
}

/**
 * Request audit filter for comprehensive activity logging.
 *
 * This filter captures request metadata before authentication,
 * ensuring we log all attempts, successful or not. This is critical
 * for security monitoring and forensics.
 */
@Component
class RequestAuditFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()

        // Add request ID for tracing
        request.setAttribute("requestId", requestId)
        response.setHeader("X-Request-Id", requestId)

        // Log request details
        logger.info(
            "Request: {} {} from {} - Request ID: {}",
            request.method,
            request.servletPath,
            getClientIp(request),
            requestId
        )

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime

            // Log response details
            logger.info(
                "Response: {} {} - Status: {} - Duration: {}ms - Request ID: {}",
                request.method,
                request.servletPath,
                response.status,
                duration,
                requestId
            )

            // Alert on slow requests (potential performance issues)
            if (duration > 1000) {
                logger.warn(
                    "Slow request detected: {} {} took {}ms",
                    request.method,
                    request.servletPath,
                    duration
                )
            }
        }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // Handle proxied requests
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
