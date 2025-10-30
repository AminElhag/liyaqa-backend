package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Security configuration implementing our zero-trust architecture.
 *
 * This configuration embodies our security philosophy: assume breach,
 * verify everything, fail securely. Every request is authenticated,
 * every action is authorized, and every failure is logged.
 *
 * From a business perspective, this protects our control plane which,
 * if compromised, could affect all our tenants. The investment in security
 * here pays dividends in customer trust and reduced incident costs.
 *
 * The main trade-off is between security and developer convenience. We've
 * chosen security, but mitigated the friction through good tooling and
 * clear documentation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val employeeRepository: EmployeeRepository
) {

    /**
     * Main security filter chain implementing our layered defense strategy.
     *
     * The order of filters matters here - each layer provides specific
     * protection and assumes previous layers have done their job. This
     * design ensures defense in depth rather than single points of failure.
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF for stateless API (JWT provides CSRF protection)
            .csrf { it.disable() }

            // Configure CORS for our admin UI
            .cors { it.configurationSource(corsConfigurationSource()) }

            // Stateless session management - no server-side sessions
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Exception handling - return JSON errors, not HTML
            .exceptionHandling {
                it.authenticationEntryPoint(CustomAuthenticationEntryPoint())
                it.accessDeniedHandler(CustomAccessDeniedHandler())
            }

            // Request authorization rules
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints - minimal surface area
                    .requestMatchers(
                        "/api/v1/internal/auth/login",
                        "/api/v1/internal/auth/password-reset/request",
                        "/api/v1/internal/auth/password-reset/complete"
                    ).permitAll()

                    // Health checks for monitoring - no sensitive data
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info"
                    ).permitAll()

                    // Everything else requires authentication
                    .anyRequest().authenticated()
            }

            // Add JWT filter before Spring Security's username/password filter
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider, employeeRepository),
                UsernamePasswordAuthenticationFilter::class.java
            )

            // Add request logging filter for audit trail
            .addFilterBefore(
                RequestAuditFilter(),
                JwtAuthenticationFilter::class.java
            )

        return http.build()
    }

    /**
     * CORS configuration for our admin UI.
     *
     * In production, these would be environment-specific. The strict
     * configuration here prevents unauthorized cross-origin requests
     * while enabling our legitimate admin UI.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // In production, replace with actual domain
            allowedOrigins = listOf(
                "http://localhost:3000",  // Local development
                "https://admin.liyaqa.com" // Production admin UI
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true // Required for cookies
            maxAge = 3600 // Cache preflight for 1 hour
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }

    /**
     * Password encoder using BCrypt with cost factor 12.
     *
     * BCrypt is designed to be slow (a feature, not a bug) to resist
     * brute force attacks. Cost factor 12 takes ~0.3s per hash,
     * acceptable for our login flow while providing strong security.
     */
    @Bean
    fun springPasswordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(
        authenticationConfiguration: AuthenticationConfiguration
    ): AuthenticationManager = authenticationConfiguration.authenticationManager
}
