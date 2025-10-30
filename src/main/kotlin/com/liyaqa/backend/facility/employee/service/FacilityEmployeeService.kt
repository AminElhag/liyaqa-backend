package com.liyaqa.backend.facility.employee.service

import com.liyaqa.backend.facility.employee.domain.FacilityEmployee
import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeStatus
import com.liyaqa.backend.facility.employee.dto.*
import com.liyaqa.backend.facility.employee.repository.FacilityEmployeeGroupRepository
import com.liyaqa.backend.facility.employee.repository.FacilityEmployeeRepository
import com.liyaqa.backend.internal.repository.SportFacilityRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Service for managing facility employees.
 *
 * Handles employee lifecycle, authentication preparation, and group management.
 */
@Service
@Transactional
class FacilityEmployeeService(
    private val employeeRepository: FacilityEmployeeRepository,
    private val groupRepository: FacilityEmployeeGroupRepository,
    private val facilityRepository: SportFacilityRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new facility employee.
     */
    fun createEmployee(request: FacilityEmployeeCreateRequest): FacilityEmployeeResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Check for duplicate email
        if (employeeRepository.existsByFacilityIdAndEmail(request.facilityId, request.email)) {
            throw IllegalArgumentException("Email '${request.email}' already exists for this facility")
        }

        // Check for duplicate employee number
        request.employeeNumber?.let {
            if (employeeRepository.existsByFacilityIdAndEmployeeNumber(request.facilityId, it)) {
                throw IllegalArgumentException("Employee number '$it' already exists for this facility")
            }
        }

        // Load groups
        val groups = request.groupIds?.let { groupIds ->
            groupIds.mapNotNull { groupRepository.findById(it).orElse(null) }.toMutableSet()
        } ?: mutableSetOf()

        // Create employee
        val employee = FacilityEmployee(
            facility = facility,
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            employeeNumber = request.employeeNumber,
            jobTitle = request.jobTitle,
            department = request.department,
            hireDate = request.hireDate,
            phoneNumber = request.phoneNumber,
            emergencyContactName = request.emergencyContactName,
            emergencyContactPhone = request.emergencyContactPhone,
            groups = groups,
            timezone = request.timezone,
            locale = request.locale
        )

        // Set tenant ID for multi-tenancy
        employee.tenantId = facility.tenantId

        val savedEmployee = employeeRepository.save(employee)

        logger.info("Employee created: ${savedEmployee.getFullName()} (${savedEmployee.email}) for facility ${facility.name}")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Get employee by ID.
     */
    @Transactional(readOnly = true)
    fun getEmployeeById(id: UUID): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        return FacilityEmployeeResponse.from(employee)
    }

    /**
     * Update employee.
     */
    fun updateEmployee(id: UUID, request: FacilityEmployeeUpdateRequest): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        // Update basic info
        request.firstName?.let { employee.firstName = it }
        request.lastName?.let { employee.lastName = it }
        request.email?.let {
            // Check if new email is already in use
            if (it != employee.email && employeeRepository.existsByFacilityIdAndEmail(employee.facility.id!!, it)) {
                throw IllegalArgumentException("Email '$it' already exists for this facility")
            }
            employee.email = it
        }
        request.employeeNumber?.let {
            // Check if new employee number is already in use
            if (it != employee.employeeNumber && employeeRepository.existsByFacilityIdAndEmployeeNumber(employee.facility.id!!, it)) {
                throw IllegalArgumentException("Employee number '$it' already exists for this facility")
            }
            employee.employeeNumber = it
        }
        request.jobTitle?.let { employee.jobTitle = it }
        request.department?.let { employee.department = it }
        request.hireDate?.let { employee.hireDate = it }
        request.phoneNumber?.let { employee.phoneNumber = it }
        request.emergencyContactName?.let { employee.emergencyContactName = it }
        request.emergencyContactPhone?.let { employee.emergencyContactPhone = it }
        request.status?.let { employee.status = it }
        request.timezone?.let { employee.timezone = it }
        request.locale?.let { employee.locale = it }

        // Update groups
        request.groupIds?.let { groupIds ->
            val groups = groupIds.mapNotNull { groupRepository.findById(it).orElse(null) }.toMutableSet()
            employee.groups = groups
        }

        val savedEmployee = employeeRepository.save(employee)

        logger.info("Employee updated: ${savedEmployee.getFullName()} (${savedEmployee.email})")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Delete employee.
     */
    fun deleteEmployee(id: UUID) {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        employeeRepository.delete(employee)

        logger.warn("Employee deleted: ${employee.getFullName()} (${employee.email})")
    }

    /**
     * Suspend employee.
     */
    fun suspendEmployee(id: UUID, request: SuspendFacilityEmployeeRequest): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        if (employee.status == FacilityEmployeeStatus.SUSPENDED) {
            throw IllegalStateException("Employee is already suspended")
        }

        employee.suspend(request.reason)
        val savedEmployee = employeeRepository.save(employee)

        logger.warn("Employee suspended: ${savedEmployee.getFullName()} - Reason: ${request.reason}")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Reactivate employee.
     */
    fun reactivateEmployee(id: UUID): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        if (employee.status != FacilityEmployeeStatus.SUSPENDED) {
            throw IllegalStateException("Only suspended employees can be reactivated")
        }

        employee.reactivate()
        val savedEmployee = employeeRepository.save(employee)

        logger.info("Employee reactivated: ${savedEmployee.getFullName()}")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Terminate employee.
     */
    fun terminateEmployee(id: UUID, request: TerminateFacilityEmployeeRequest): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        if (employee.status == FacilityEmployeeStatus.TERMINATED) {
            throw IllegalStateException("Employee is already terminated")
        }

        employee.terminate(request.reason)
        val savedEmployee = employeeRepository.save(employee)

        logger.warn("Employee terminated: ${savedEmployee.getFullName()} - Reason: ${request.reason}")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Change employee password.
     */
    fun changePassword(id: UUID, request: FacilityEmployeeChangePasswordRequest): FacilityEmployeeResponse {
        val employee = employeeRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Employee not found: $id") }

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword, employee.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Update password
        employee.passwordHash = passwordEncoder.encode(request.newPassword)
        val savedEmployee = employeeRepository.save(employee)

        logger.info("Password changed for employee: ${savedEmployee.getFullName()}")

        return FacilityEmployeeResponse.from(savedEmployee)
    }

    /**
     * Search employees.
     */
    @Transactional(readOnly = true)
    fun searchEmployees(
        searchTerm: String?,
        status: FacilityEmployeeStatus?,
        facilityId: UUID?,
        department: String?,
        pageable: Pageable
    ): Page<FacilityEmployeeBasicResponse> {
        val page = employeeRepository.searchEmployees(searchTerm, status, facilityId, department, pageable)
        return page.map { FacilityEmployeeBasicResponse.from(it) }
    }

    /**
     * Get employees by facility.
     */
    @Transactional(readOnly = true)
    fun getEmployeesByFacility(facilityId: UUID): List<FacilityEmployeeResponse> {
        return employeeRepository.findByFacilityId(facilityId)
            .map { FacilityEmployeeResponse.from(it) }
    }

    /**
     * Get active employees by facility.
     */
    @Transactional(readOnly = true)
    fun getActiveEmployeesByFacility(facilityId: UUID): List<FacilityEmployeeResponse> {
        return employeeRepository.findActiveByFacility(facilityId)
            .map { FacilityEmployeeResponse.from(it) }
    }

    /**
     * Record login for employee (called after successful authentication).
     */
    fun recordLogin(employeeId: UUID, ipAddress: String) {
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EntityNotFoundException("Employee not found: $employeeId") }

        employee.recordLogin(ipAddress)
        employeeRepository.save(employee)

        logger.info("Login recorded for employee: ${employee.getFullName()} from IP: $ipAddress")
    }

    /**
     * Record failed login attempt.
     */
    fun recordFailedLogin(email: String, facilityId: UUID) {
        val employee = employeeRepository.findByFacilityIdAndEmail(facilityId, email)
        employee?.let {
            it.recordFailedLogin()
            employeeRepository.save(it)
            logger.warn("Failed login attempt for employee: ${it.getFullName()} (Attempt ${it.failedLoginAttempts}/5)")
        }
    }
}
