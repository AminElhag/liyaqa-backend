package com.liyaqa.backend.internal.security

import com.liyaqa.backend.internal.domain.employee.Employee
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Resolver for @CurrentEmployee annotation.
 * 
 * This component integrates with Spring MVC to automatically inject
 * the authenticated employee into controller methods. It's a critical
 * security component that ensures controllers always work with the
 * correct user context.
 */
@Component
class CurrentEmployeeArgumentResolver : HandlerMethodArgumentResolver {
    
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentEmployee::class.java) &&
               parameter.parameterType == Employee::class.java
    }
    
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val authentication = SecurityContextHolder.getContext().authentication
        
        return if (authentication?.principal is Employee) {
            authentication.principal as Employee
        } else {
            // This should not happen if security is configured correctly
            throw SecurityException("No authenticated employee found")
        }
    }
}
