package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.Permission
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Security utility functions for common authorization checks.
 * 
 * These functions centralize security logic that's used across
 * multiple services, ensuring consistent enforcement of our
 * security policies.
 */
@Component
class SecurityUtils {
    
    /**
     * Gets the currently authenticated employee.
     * 
     * This is the programmatic equivalent of @CurrentEmployee annotation
     * for use in services where injection isn't available.
     */
    fun getCurrentEmployee(): Employee? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal is Employee) {
            authentication.principal as Employee
        } else {
            null
        }
    }
    
    /**
     * Checks if the current user has a specific permission.
     * 
     * This supports dynamic permission checks in business logic where
     * compile-time annotations aren't flexible enough.
     */
    fun hasPermission(permission: Permission): Boolean {
        val employee = getCurrentEmployee()
        return employee?.hasPermission(permission) ?: false
    }
    
    /**
     * Checks if the current user has any of the specified permissions.
     */
    fun hasAnyPermission(vararg permissions: Permission): Boolean {
        val employee = getCurrentEmployee()
        return employee?.hasAnyPermission(*permissions) ?: false
    }
    
    /**
     * Ensures the current user has required permission or throws.
     * 
     * This is for imperative permission checks in service layer where
     * we need to enforce authorization programmatically.
     */
    fun requirePermission(permission: Permission) {
        if (!hasPermission(permission)) {
            throw UnauthorizedException("Missing required permission: $permission")
        }
    }
    
    /**
     * Checks if the current user can access data for a specific tenant.
     * 
     * This is critical for multi-tenant security, ensuring employees
     * can only access tenant data they're authorized for.
     */
    fun canAccessTenant(tenantId: String): Boolean {
        val employee = getCurrentEmployee() ?: return false
        
        // Super admins can access all tenants
        if (employee.hasPermission(Permission.SYSTEM_CONFIGURE)) {
            return true
        }
        
        // Support staff need explicit permission
        if (employee.hasPermission(Permission.TENANT_ACCESS_DATA)) {
            // Additional checks could verify assignment to specific tenant
            return true
        }
        
        return false
    }
}
