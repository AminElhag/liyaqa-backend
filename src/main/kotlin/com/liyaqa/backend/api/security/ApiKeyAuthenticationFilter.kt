package com.liyaqa.backend.api.security

import com.liyaqa.backend.api.domain.ApiKey
import com.liyaqa.backend.api.service.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authentication filter for Public API using API keys.
 *
 * Extracts API key from Authorization header and validates it.
 * Header format: "Bearer lyk_live_..."
 */
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Only process public API paths
        if (!request.requestURI.startsWith("/api/v1/public")) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            val apiKey = authHeader.substring(BEARER_PREFIX.length)

            try {
                val validatedKey = apiKeyService.validateApiKey(apiKey)

                if (validatedKey != null) {
                    // Create authentication object
                    val principal = ApiKeyPrincipal(validatedKey)
                    val authorities = validatedKey.scopes
                        .split(",")
                        .map { SimpleGrantedAuthority("SCOPE_$it") }

                    val authentication = UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                    )

                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    // Invalid API key - will be caught by security config
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key")
                    return
                }
            } catch (e: Exception) {
                logger.error("Error validating API key", e)
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed")
                return
            }
        } else {
            // No API key provided
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key required")
            return
        }

        filterChain.doFilter(request, response)
    }
}

/**
 * Principal object representing an authenticated API key.
 */
data class ApiKeyPrincipal(
    val apiKey: ApiKey
) {
    val tenantId: String get() = apiKey.tenantId
    val facilityId: java.util.UUID get() = apiKey.facilityId
    val apiKeyId: java.util.UUID get() = apiKey.id!!
    val scopes: List<String> get() = apiKey.scopes.split(",")

    fun hasScope(scope: String): Boolean = apiKey.hasScope(scope)
}
