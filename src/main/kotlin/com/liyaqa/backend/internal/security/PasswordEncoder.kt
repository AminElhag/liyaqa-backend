package com.liyaqa.backend.internal.security

import org.springframework.stereotype.Component

/**
 * Wrapper interface to maintain clean separation between
 * Spring Security's PasswordEncoder and our domain.
 */
@Component
class PasswordEncoder(
    private val encoder: org.springframework.security.crypto.password.PasswordEncoder
) {
    fun encode(rawPassword: String): String = encoder.encode(rawPassword)
    fun matches(rawPassword: String, encodedPassword: String): Boolean =
        encoder.matches(rawPassword, encodedPassword)
}