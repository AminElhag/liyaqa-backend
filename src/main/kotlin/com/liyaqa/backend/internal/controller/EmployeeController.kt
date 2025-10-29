package com.liyaqa.backend.internal.controller

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.domain.employee.Permission
import com.liyaqa.backend.internal.dto.employee.*
import com.liyaqa.backend.internal.security.CurrentEmployee
import com.liyaqa.backend.internal.security.RequirePermission
import com.liyaqa.backend.internal.service.EmployeeAlreadyExistsException
import com.liyaqa.backend.internal.service.EmployeeNotFoundException
import com.liyaqa.backend.internal.service.EmployeeService
import com.liyaqa.backend.internal.service.UnauthorizedException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

/**
 * REST API for internal team management.
 *
 * This controller embodies our API design philosophy: explicit over implicit,
 * security by default, and comprehensive observability. Every endpoint is
 * designed with the assumption that it will be called thousands of times
 * per day and must provide clear feedback when things go wrong.
 *
 * The layered permission model here reflects our organizational structure -
 * different roles need different levels of access, and we enforce this at
 * the API gateway level for defense in depth.
 */
@RestController
@RequestMapping("/api/v1/internal/employees")
@CrossOrigin(
    origins = ["http://localhost:3000"],
    allowCredentials = true.toString()
) // Configure properly in production
class EmployeeController(
    private val employeeService: EmployeeService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Creates a new employee account.
     *
     * From a business perspective, this endpoint enables rapid team scaling
     * while maintaining security standards. The permission requirement ensures
     * only authorized personnel (HR, team leads) can onboard new members.
     *
     * The @Valid annotation triggers our validation pipeline, ensuring data
     * integrity before it reaches the business layer - fail fast principle.
     */
    @PostMapping
    @RequirePermission(Permission.EMPLOYEE_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    fun createEmployee(
        @Valid @RequestBody request: CreateEmployeeRequest,
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        logger.info("Employee creation requested by ${currentEmployee.email} for ${request.email}")

        return employeeService.createEmployee(request, currentEmployee)
    }

    /**
     * Retrieves paginated list of employees with filtering.
     *
     * This design supports our operational need to quickly find team members
     * across different departments and roles. The pagination ensures the UI
     * remains responsive even as our team grows to hundreds of employees.
     *
     * We default to sorting by name for human-friendly results, but allow
     * clients to override based on their specific needs (e.g., sort by
     * last login for security audits).
     */
    @GetMapping
    @RequirePermission(Permission.EMPLOYEE_VIEW)
    fun getEmployees(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "false") includeTerminated: Boolean,
        @PageableDefault(size = 20, sort = ["firstName", "lastName"], direction = Sort.Direction.ASC)
        pageable: Pageable,
        @CurrentEmployee currentEmployee: Employee
    ): Page<EmployeeResponse> {
        val filter = EmployeeSearchFilter(
            searchTerm = search,
            departmentFilter = department,
            status = status,
            includeTerminated = includeTerminated,
        )

        return employeeService.searchEmployees(filter, pageable, currentEmployee)
    }

    /**
     * Retrieves a specific employee by ID.
     *
     * The granular response here supports different use cases:
     * - HR needs full details for record keeping
     * - Support managers need contact info and permissions
     * - Regular employees might only see basic info
     *
     * The service layer handles these permission-based filtering rules.
     */
    @GetMapping("/{id}")
    @RequirePermission(Permission.EMPLOYEE_VIEW)
    fun getEmployee(
        @PathVariable id: UUID,
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        logger.debug("Employee $id details requested by ${currentEmployee.email}")

        return employeeService.getEmployeeById(id, currentEmployee)
    }

    /**
     * Updates employee information.
     *
     * This endpoint uses PATCH semantics (partial updates) rather than PUT
     * because employee records have many fields and we don't want to risk
     * accidentally overwriting data. This design choice trades some REST
     * purity for operational safety.
     */
    @PatchMapping("/{id}")
    @RequirePermission(Permission.EMPLOYEE_UPDATE)
    fun updateEmployee(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateEmployeeRequest,
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        logger.info("Employee $id update requested by ${currentEmployee.email}")

        return employeeService.updateEmployee(id, request, currentEmployee)
    }

    /**
     * Updates employee group assignments.
     *
     * We separate group management from general updates because permission
     * changes are high-risk operations that warrant special attention.
     * This endpoint will trigger additional audit logging and notifications.
     */
    @PutMapping("/{id}/groups")
    @RequirePermission(Permission.GROUP_ASSIGN_PERMISSIONS)
    fun updateEmployeeGroups(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateGroupsRequest,
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        logger.warn("Permission change for employee $id initiated by ${currentEmployee.email}")

        return employeeService.updateEmployeeGroups(id, request.groupIds, currentEmployee)
    }

    /**
     * Soft deletes an employee.
     *
     * We never hard delete for audit trail purposes. This operation:
     * 1. Marks the account as terminated
     * 2. Revokes all permissions immediately
     * 3. Terminates all active sessions
     * 4. Preserves all historical data for compliance
     */
    @DeleteMapping("/{id}")
    @RequirePermission(Permission.EMPLOYEE_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEmployee(
        @PathVariable id: UUID,
        @CurrentEmployee currentEmployee: Employee
    ): Unit {
        logger.warn("Employee termination for $id initiated by ${currentEmployee.email}")

        employeeService.deleteEmployee(id, currentEmployee)
    }

    /**
     * Changes an employee's password (admin action).
     *
     * This differs from self-service password change - it's for when
     * HR or IT needs to reset someone's password. It forces a password
     * change on next login for security.
     */
    @PostMapping("/{id}/reset-password")
    @RequirePermission(Permission.EMPLOYEE_UPDATE)
    fun resetEmployeePassword(
        @PathVariable id: UUID,
        @CurrentEmployee currentEmployee: Employee
    ): PasswordResetResponse {
        logger.info("Password reset for employee $id initiated by ${currentEmployee.email}")

        val temporaryPassword = employeeService.resetEmployeePassword(id, currentEmployee)

        return PasswordResetResponse(
            message = "Password has been reset. The employee must change it on next login.",
            temporaryPassword = temporaryPassword, // Only shown to authorized admin
            mustChangeOnNextLogin = true
        )
    }

    /**
     * Retrieves current user's own profile.
     *
     * This endpoint bypasses permission checks since employees can
     * always view their own information. It's the foundation for
     * the user menu and profile settings in our admin UI.
     */
    @GetMapping("/me")
    fun getCurrentEmployee(
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        return employeeService.getEmployeeById(currentEmployee.id!!, currentEmployee)
    }

    /**
     * Updates current user's own profile.
     *
     * Limited self-service updates for profile maintenance.
     * Sensitive fields like permissions can't be self-modified.
     */
    @PatchMapping("/me")
    fun updateCurrentEmployee(
        @Valid @RequestBody request: UpdateProfileRequest,
        @CurrentEmployee currentEmployee: Employee
    ): EmployeeResponse {
        logger.info("Self-profile update by ${currentEmployee.email}")

        return employeeService.updateProfile(currentEmployee.id!!, request, currentEmployee)
    }

    /**
     * Changes current user's own password.
     *
     * Self-service password change with additional validation.
     * Requires current password for security.
     */
    @PostMapping("/me/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        @CurrentEmployee currentEmployee: Employee
    ): ResponseEntity<MessageResponse> {
        employeeService.changePassword(
            currentEmployee.id!!,
            request.currentPassword,
            request.newPassword
        )

        return ResponseEntity.ok(
            MessageResponse("Password changed successfully. Please login with your new password.")
        )
    }

    /**
     * Exception handler for employee-specific errors.
     *
     * Provides consistent error responses while avoiding information
     * leakage that could aid attackers.
     */
    @ExceptionHandler(EmployeeNotFoundException::class)
    fun handleEmployeeNotFound(ex: EmployeeNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    error = "EMPLOYEE_NOT_FOUND",
                    message = "The requested employee was not found",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(EmployeeAlreadyExistsException::class)
    fun handleEmployeeExists(ex: EmployeeAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    error = "EMPLOYEE_EXISTS",
                    message = "An employee with this email already exists",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    error = "INSUFFICIENT_PERMISSIONS",
                    message = "You don't have permission to perform this action",
                    timestamp = Instant.now()
                )
            )
    }
}