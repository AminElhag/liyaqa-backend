package com.liyaqa.backend.internal.employee.controller

import com.liyaqa.backend.internal.employee.dto.*
import com.liyaqa.backend.internal.employee.service.BootstrapService
import com.liyaqa.backend.internal.shared.security.PublicEndpoint
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for system initialization and setup.
 *
 * This controller provides endpoints for bootstrapping the system on first run.
 * The initialization endpoint is public (no authentication required) but can only
 * be called once - when the system has no employees.
 *
 * Base URL: /api/v1/internal/system
 */
@RestController
@RequestMapping("/api/v1/internal/system")
class SystemInitializationController(
    private val bootstrapService: BootstrapService
) {

    /**
     * Check system initialization status.
     * GET /api/v1/internal/system/init-status
     *
     * Public endpoint - allows checking if system needs initialization.
     */
    @GetMapping("/init-status")
    @PublicEndpoint
    fun getInitializationStatus(): ResponseEntity<InitializationStatusResponse> {
        val status = bootstrapService.getInitializationStatus()

        val message = when {
            !status.isInitialized -> "System not initialized. Create administrator account to begin."
            !status.hasAdministrator -> "Warning: No administrator account found!"
            !status.predefinedGroupsPresent -> "Warning: Some predefined groups are missing."
            else -> "System initialized and ready."
        }

        val response = InitializationStatusResponse(
            isInitialized = status.isInitialized,
            employeeCount = status.employeeCount,
            groupCount = status.groupCount,
            hasAdministrator = status.hasAdministrator,
            predefinedGroupsPresent = status.predefinedGroupsPresent,
            message = message
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Initialize the system with administrator account.
     * POST /api/v1/internal/system/initialize
     *
     * Public endpoint - can only be called once (when system has no employees).
     * Creates:
     * - Predefined employee groups (Super Admin, Support, Sales, Finance)
     * - Initial administrator employee with full permissions
     *
     * Security: After initialization, this endpoint will reject all requests
     * to prevent unauthorized account creation.
     */
    @PostMapping("/initialize")
    @PublicEndpoint
    fun initializeSystem(
        @Valid @RequestBody request: SystemInitializationRequest
    ): ResponseEntity<SystemInitializationResponse> {
        // Check if already initialized
        if (bootstrapService.isSystemInitialized()) {
            throw IllegalStateException("System is already initialized. Cannot create additional administrator accounts through this endpoint.")
        }

        // Initialize system
        val administrator = bootstrapService.initializeSystem(
            adminEmail = request.adminEmail,
            adminPassword = request.adminPassword,
            adminFirstName = request.adminFirstName,
            adminLastName = request.adminLastName
        )

        // Get created groups
        val groups = bootstrapService.getInitializationStatus().let {
            listOf("Super Admin", "Support Agent", "Support Manager", "Sales", "Finance")
        }

        val response = SystemInitializationResponse(
            success = true,
            message = "System initialized successfully. Administrator account created.",
            administrator = EmployeeResponse.from(administrator),
            groupsCreated = groups,
            warningMessage = "IMPORTANT: Change the administrator password immediately after first login!"
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Ensure predefined groups exist.
     * POST /api/v1/internal/system/ensure-groups
     *
     * Requires authentication and SYSTEM_CONFIGURE permission.
     * This endpoint can be called to add new predefined groups after system initialization.
     */
    @PostMapping("/ensure-groups")
    fun ensurePredefinedGroups(): ResponseEntity<MessageResponse> {
        bootstrapService.ensurePredefinedGroupsExist()

        return ResponseEntity.ok(
            MessageResponse("Predefined groups verified and created if missing.")
        )
    }
}
