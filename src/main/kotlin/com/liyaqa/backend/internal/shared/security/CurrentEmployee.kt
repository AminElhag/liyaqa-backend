package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.Permission
import org.springframework.core.MethodParameter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Marks a parameter to be injected with the currently authenticated employee.
 * 
 * This annotation simplifies controller methods by automatically providing
 * the authenticated user without manual extraction from SecurityContext.
 * It's both a convenience and a security feature - controllers can't
 * accidentally use the wrong user context.
 * 
 * Usage:
 * ```
 * @GetMapping("/profile")
 * fun getProfile(@CurrentEmployee employee: Employee): ProfileResponse
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentEmployee

