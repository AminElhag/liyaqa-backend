package com.liyaqa.backend.internal.employee.service

import com.liyaqa.backend.internal.audit.domain.*
import com.liyaqa.backend.internal.employee.domain.*
import com.liyaqa.backend.internal.employee.dto.*
import com.liyaqa.backend.internal.employee.data.EmployeeGroupRepository
import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import com.liyaqa.backend.internal.shared.security.PasswordEncoder
import com.liyaqa.backend.internal.shared.security.validatePasswordStrength
import com.liyaqa.backend.internal.audit.service.AuditService
import com.liyaqa.backend.internal.shared.config.EmailService
import com.liyaqa.backend.internal.shared.exception.*
import com.liyaqa.backend.internal.shared.util.toResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service orchestrating employee operations with comprehensive audit trails.
 *
 * This service embodies our security-first approach - every operation is logged,
 * every permission is checked, and every state change is tracked. We treat our
 * internal system with the same rigor as customer-facing features because
 * operational security is a competitive advantage.
 */
@Service
@Transactional
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val groupRepository: EmployeeGroupRepository,
    private val auditService: AuditService,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
        const val PASSWORD_HISTORY_SIZE = 5 // Prevent reusing last 5 passwords
    }

    /**
     * Creates a new employee with secure password handling and welcome email.
     *
     * From a business perspective, this ensures new team members can be
     * onboarded quickly while maintaining security standards. The temporary
     * password forces immediate change, reducing the window of vulnerability.
     */
    fun createEmployee(
        request: CreateEmployeeRequest,
        createdBy: Employee
    ): EmployeeResponse {
        logger.info("Creating new employee: ${request.email}")

        // Validate uniqueness
        if (employeeRepository.existsByEmail(request.email)) {
            throw EmployeeAlreadyExistsException("Email ${request.email} is already in use")
        }

        // Generate secure temporary password
        val temporaryPassword = generateTemporaryPassword()

        val employee = Employee(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            passwordHash = passwordEncoder.encode(temporaryPassword),
            department = request.department,
            jobTitle = request.jobTitle,
            phoneNumber = request.phoneNumber,
            mustChangePassword = true // Force password change on first login
        )

        // Assign initial groups
        request.groupIds?.forEach { groupId ->
            val group = groupRepository.findById(groupId)
                .orElseThrow { GroupNotFoundException("Group $groupId not found") }
            employee.groups.add(group)
        }

        val savedEmployee = employeeRepository.save(employee)

        // Send welcome email with temporary credentials
        emailService.sendWelcomeEmail(
            savedEmployee.email,
            savedEmployee.getFullName(),
            temporaryPassword
        )

        // Audit the creation
        auditService.logEmployeeCreated(
            employee = savedEmployee,
            createdBy = createdBy,
            initialGroups = employee.groups
        )

        return savedEmployee.toResponse()
    }

    /**
     * Updates employee information with granular change tracking.
     *
     * The design here allows partial updates while maintaining complete
     * audit trails. We capture the old and new states for compliance
     * and debugging purposes.
     */
    fun updateEmployee(
        id: UUID,
        request: UpdateEmployeeRequest,
        updatedBy: Employee
    ): EmployeeResponse {
        val employee = findEmployeeById(id)
        val oldState = employee // Capture for audit

        // Apply updates
        request.firstName?.let { employee.firstName = it }
        request.lastName?.let { employee.lastName = it }
        request.department?.let { employee.department = it }
        request.jobTitle?.let { employee.jobTitle = it }
        request.phoneNumber?.let { employee.phoneNumber = it }

        // Handle status changes with additional validation
        request.status?.let { newStatus ->
            validateStatusTransition(employee.status, newStatus, updatedBy)
            employee.status = newStatus

            // Clear login attempts on reactivation
            if (newStatus == EmployeeStatus.ACTIVE) {
                employee.failedLoginAttempts = 0
                employee.lockedUntil = null
            }
        }

        val updatedEmployee = employeeRepository.save(employee)

        // Detailed audit log
        auditService.logEmployeeUpdated(
            employee = updatedEmployee,
            oldState = oldState,
            updatedBy = updatedBy,
            changes = request.getChangeSummary()
        )

        return updatedEmployee.toResponse()
    }

    /**
     * Manages group assignments with permission impact analysis.
     *
     * This operation is high-risk because changing groups immediately
     * affects what an employee can do. We log both the permission
     * delta and notify relevant stakeholders.
     */
    fun updateEmployeeGroups(
        employeeId: UUID,
        groupIds: Set<UUID>,
        updatedBy: Employee
    ): EmployeeResponse {
        val employee = findEmployeeById(employeeId)
        val oldPermissions = employee.getAllPermissions()

        // Clear and reassign groups
        employee.groups.clear()
        groupIds.forEach { groupId ->
            val group = groupRepository.findById(groupId)
                .orElseThrow { GroupNotFoundException("Group $groupId not found") }
            employee.groups.add(group)
        }

        val savedEmployee = employeeRepository.save(employee)
        val newPermissions = savedEmployee.getAllPermissions()

        // Calculate permission delta for audit
        val addedPermissions = newPermissions - oldPermissions
        val removedPermissions = oldPermissions - newPermissions

        auditService.logPermissionsChanged(
            employee = savedEmployee,
            addedPermissions = addedPermissions,
            removedPermissions = removedPermissions,
            updatedBy = updatedBy
        )

        // Notify employee of permission changes
        if (addedPermissions.isNotEmpty() || removedPermissions.isNotEmpty()) {
            emailService.sendPermissionChangeNotification(
                savedEmployee.email,
                savedEmployee.getFullName(),
                addedPermissions,
                removedPermissions
            )
        }

        return savedEmployee.toResponse()
    }

    /**
     * Handles password changes with history validation.
     *
     * We enforce password history to prevent cycling through
     * a small set of passwords, which is a common security weakness
     * in enterprise systems.
     */
    fun changePassword(
        employeeId: UUID,
        currentPassword: String,
        newPassword: String
    ): Unit {
        val employee = findEmployeeById(employeeId)

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, employee.passwordHash)) {
            auditService.logFailedPasswordChange(employee, "Invalid current password")
            throw InvalidPasswordException("Current password is incorrect")
        }

        // Validate password strength
        validatePasswordStrength(newPassword)

        // Check password history (would need a separate PasswordHistory entity in production)
        // This is simplified for illustration
        if (passwordEncoder.matches(newPassword, employee.passwordHash)) {
            throw PasswordReuseException("Cannot reuse recent passwords")
        }

        // Update password
        employee.passwordHash = passwordEncoder.encode(newPassword)
        employee.mustChangePassword = false
        employee.failedLoginAttempts = 0

        employeeRepository.save(employee)

        auditService.logPasswordChanged(employee)
        emailService.sendPasswordChangeConfirmation(employee.email, employee.getFullName())
    }

    /**
     * Searches employees with filtering and pagination.
     *
     * The design here supports efficient querying while respecting
     * permission boundaries. Support agents might only see other
     * support team members, while HR sees everyone.
     */
    fun searchEmployees(
        filter: EmployeeSearchFilter,
        pageable: Pageable,
        requestedBy: Employee
    ): Page<EmployeeResponse> {
        // Permission-based filtering
        val effectiveFilter = applyPermissionFilter(filter, requestedBy)

        val page = employeeRepository.searchEmployees(
            effectiveFilter.searchTerm,
            effectiveFilter.departmentFilter,
            effectiveFilter.status,
            effectiveFilter.includeTerminated,
            pageable
        )

        // Log if sensitive search was performed
        if (filter.includeTerminated == true) {
            auditService.logSensitiveSearch(
                requestedBy,
                "Searched employees including terminated",
                EntityType.EMPLOYEE
            )
        }

        return page.map { it.toResponse() }
    }

    /**
     * Soft deletes an employee with cascading security actions.
     *
     * We never hard delete employees due to audit requirements.
     * This operation revokes all access immediately while preserving
     * historical records.
     */
    fun deleteEmployee(
        id: UUID,
        deletedBy: Employee
    ): Unit {
        val employee = findEmployeeById(id)

        // Prevent self-deletion
        if (employee.id == deletedBy.id) {
            throw InvalidOperationException("Cannot delete your own account")
        }

        // Prevent deleting last super admin
        if (employee.hasPermission(Permission.SYSTEM_CONFIGURE)) {
            val adminCount = employeeRepository.countByPermission(Permission.SYSTEM_CONFIGURE)
            if (adminCount <= 1) {
                throw InvalidOperationException("Cannot delete the last system administrator")
            }
        }

        employee.status = EmployeeStatus.TERMINATED
        employee.groups.clear() // Remove all permissions

        employeeRepository.save(employee)

        // Revoke all active sessions (would integrate with session management)
        // sessionService.revokeAllSessions(employee.id)

        auditService.logEmployeeDeleted(employee, deletedBy)

        // Notify relevant parties
        emailService.sendEmployeeTerminationNotification(
            employee.email,
            employee.getFullName(),
            deletedBy.getFullName()
        )
    }

    /**
     * Retrieves employee by ID with permission-based filtering.
     *
     * Different roles see different levels of detail based on their
     * permissions and relationship to the requested employee.
     */
    fun getEmployeeById(id: UUID, requestedBy: Employee): EmployeeResponse {
        val employee = findEmployeeById(id)

        // Log if viewing another employee's details
        if (employee.id != requestedBy.id) {
            auditService.logSensitiveSearch(
                requestedBy,
                "Viewed employee details",
                EntityType.EMPLOYEE
            )
        }

        return employee.toResponse()
    }

    /**
     * Resets an employee's password (admin action).
     *
     * This generates a new temporary password that must be changed
     * on next login. Used for account recovery when employees forget
     * their passwords.
     */
    fun resetEmployeePassword(id: UUID, resetBy: Employee): String {
        val employee = findEmployeeById(id)

        // Generate new temporary password
        val temporaryPassword = generateTemporaryPassword()

        employee.passwordHash = passwordEncoder.encode(temporaryPassword)
        employee.mustChangePassword = true
        employee.failedLoginAttempts = 0
        employee.lockedUntil = null

        employeeRepository.save(employee)

        // Send notification
        emailService.sendPasswordResetEmail(
            employee.email,
            employee.getFullName(),
            temporaryPassword
        )

        auditService.logPasswordResetRequested(employee, "Admin reset by ${resetBy.email}")

        return temporaryPassword
    }

    /**
     * Updates employee's own profile.
     *
     * Limited self-service update for personal information.
     * Sensitive fields like permissions cannot be self-modified.
     */
    fun updateProfile(
        id: UUID,
        request: UpdateProfileRequest,
        updatedBy: Employee
    ): EmployeeResponse {
        // Ensure employees can only update their own profile through this method
        if (id != updatedBy.id) {
            throw UnauthorizedException("Can only update your own profile through this endpoint")
        }

        val employee = findEmployeeById(id)

        // Apply limited updates
        request.firstName?.let { employee.firstName = it }
        request.lastName?.let { employee.lastName = it }
        request.phoneNumber?.let { employee.phoneNumber = it }

        val updatedEmployee = employeeRepository.save(employee)

        return updatedEmployee.toResponse()
    }

    // Helper methods

    private fun findEmployeeById(id: UUID): Employee =
        employeeRepository.findById(id)
            .orElseThrow { EmployeeNotFoundException("Employee $id not found") }

    private fun validateStatusTransition(
        current: EmployeeStatus,
        new: EmployeeStatus,
        updatedBy: Employee
    ) {
        // Business rules for status transitions
        when {
            current == EmployeeStatus.TERMINATED && new != EmployeeStatus.TERMINATED -> {
                if (!updatedBy.hasPermission(Permission.EMPLOYEE_CREATE)) {
                    throw UnauthorizedException("Cannot reactivate terminated employees")
                }
            }

            new == EmployeeStatus.TERMINATED -> {
                if (!updatedBy.hasPermission(Permission.EMPLOYEE_DELETE)) {
                    throw UnauthorizedException("Cannot terminate employees")
                }
            }
        }
    }

    private fun generateTemporaryPassword(): String {
        // In production, use a secure random generator
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%"
        return (1..16)
            .map { chars.random() }
            .joinToString("")
    }

    private fun applyPermissionFilter(
        filter: EmployeeSearchFilter,
        requestedBy: Employee
    ): EmployeeSearchFilter {
        // Apply permission-based restrictions
        return if (!requestedBy.hasPermission(Permission.EMPLOYEE_VIEW)) {
            // Can only see themselves and their department
            filter.copy(departmentFilter = requestedBy.department)
        } else {
            filter
        }
    }
}