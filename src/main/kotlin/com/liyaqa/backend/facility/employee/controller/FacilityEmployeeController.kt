package com.liyaqa.backend.facility.employee.controller

import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeStatus
import com.liyaqa.backend.facility.employee.dto.*
import com.liyaqa.backend.facility.employee.service.FacilityEmployeeService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for managing facility employees.
 *
 * Base path: /api/v1/facility/employees
 *
 * Endpoints:
 * - Employee CRUD: create, read, update, delete
 * - Employee lifecycle: suspend, reactivate, terminate
 * - Password management
 * - Search and filtering
 */
@RestController
@RequestMapping("/api/v1/facility/employees")
class FacilityEmployeeController(
    private val employeeService: FacilityEmployeeService
) {

    /**
     * Create a new facility employee.
     * POST /api/v1/facility/employees
     */
    @PostMapping
    fun createEmployee(
        @Valid @RequestBody request: FacilityEmployeeCreateRequest
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.createEmployee(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(employee)
    }

    /**
     * Get employee by ID.
     * GET /api/v1/facility/employees/{id}
     */
    @GetMapping("/{id}")
    fun getEmployeeById(
        @PathVariable id: UUID
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.getEmployeeById(id)
        return ResponseEntity.ok(employee)
    }

    /**
     * Update employee.
     * PUT /api/v1/facility/employees/{id}
     */
    @PutMapping("/{id}")
    fun updateEmployee(
        @PathVariable id: UUID,
        @Valid @RequestBody request: FacilityEmployeeUpdateRequest
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.updateEmployee(id, request)
        return ResponseEntity.ok(employee)
    }

    /**
     * Delete employee.
     * DELETE /api/v1/facility/employees/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteEmployee(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        employeeService.deleteEmployee(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Search employees with filters.
     * GET /api/v1/facility/employees
     */
    @GetMapping
    fun searchEmployees(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: FacilityEmployeeStatus?,
        @RequestParam(required = false) facilityId: UUID?,
        @RequestParam(required = false) department: String?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<Page<FacilityEmployeeBasicResponse>> {
        val employees = employeeService.searchEmployees(searchTerm, status, facilityId, department, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get all employees for a facility.
     * GET /api/v1/facility/employees/by-facility/{facilityId}
     */
    @GetMapping("/by-facility/{facilityId}")
    fun getEmployeesByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<FacilityEmployeeResponse>> {
        val employees = employeeService.getEmployeesByFacility(facilityId)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get active employees for a facility.
     * GET /api/v1/facility/employees/by-facility/{facilityId}/active
     */
    @GetMapping("/by-facility/{facilityId}/active")
    fun getActiveEmployeesByFacility(
        @PathVariable facilityId: UUID
    ): ResponseEntity<List<FacilityEmployeeResponse>> {
        val employees = employeeService.getActiveEmployeesByFacility(facilityId)
        return ResponseEntity.ok(employees)
    }

    /**
     * Suspend employee.
     * POST /api/v1/facility/employees/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    fun suspendEmployee(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SuspendFacilityEmployeeRequest
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.suspendEmployee(id, request)
        return ResponseEntity.ok(employee)
    }

    /**
     * Reactivate employee.
     * POST /api/v1/facility/employees/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
    fun reactivateEmployee(
        @PathVariable id: UUID
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.reactivateEmployee(id)
        return ResponseEntity.ok(employee)
    }

    /**
     * Terminate employee.
     * POST /api/v1/facility/employees/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    fun terminateEmployee(
        @PathVariable id: UUID,
        @Valid @RequestBody request: TerminateFacilityEmployeeRequest
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.terminateEmployee(id, request)
        return ResponseEntity.ok(employee)
    }

    /**
     * Change employee password.
     * POST /api/v1/facility/employees/{id}/change-password
     */
    @PostMapping("/{id}/change-password")
    fun changePassword(
        @PathVariable id: UUID,
        @Valid @RequestBody request: FacilityEmployeeChangePasswordRequest
    ): ResponseEntity<FacilityEmployeeResponse> {
        val employee = employeeService.changePassword(id, request)
        return ResponseEntity.ok(employee)
    }
}
