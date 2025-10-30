package com.liyaqa.backend.internal.employee.service

import com.liyaqa.backend.internal.employee.domain.*
import com.liyaqa.backend.internal.employee.data.EmployeeGroupRepository
import com.liyaqa.backend.internal.employee.data.EmployeeRepository
import com.liyaqa.backend.internal.shared.security.PasswordEncoder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Bootstrap service for initializing the system on first run.
 *
 * Creates:
 * - Predefined employee groups (Super Admin, Support Agent, Support Manager, Sales, Finance)
 * - Initial administrator employee with full system access
 *
 * This service should be called once during system setup to create the
 * foundational user who can then manage other employees and permissions.
 */
@Service
@Transactional
class BootstrapService(
    private val employeeRepository: EmployeeRepository,
    private val groupRepository: EmployeeGroupRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Check if system has been initialized (has any employees).
     */
    fun isSystemInitialized(): Boolean {
        return employeeRepository.count() > 0
    }

    /**
     * Initialize the system with predefined groups and administrator.
     *
     * This method is idempotent - it will not create duplicates if called multiple times.
     *
     * @param adminEmail Email for the administrator account
     * @param adminPassword Initial password for the administrator
     * @param adminFirstName Administrator's first name
     * @param adminLastName Administrator's last name
     * @return The created administrator employee
     * @throws IllegalStateException if system is already initialized
     */
    fun initializeSystem(
        adminEmail: String,
        adminPassword: String,
        adminFirstName: String = "System",
        adminLastName: String = "Administrator"
    ): Employee {
        if (isSystemInitialized()) {
            throw IllegalStateException("System is already initialized. Cannot create duplicate administrator.")
        }

        logger.info("Starting system initialization...")

        // Create predefined groups
        val superAdminGroup = createOrGetGroup(PredefinedGroups.createSuperAdmin())
        val supportAgentGroup = createOrGetGroup(PredefinedGroups.createSupportAgent())
        val supportManagerGroup = createOrGetGroup(PredefinedGroups.createSupportManager())
        val salesGroup = createOrGetGroup(PredefinedGroups.createSalesTeam())
        val financeGroup = createOrGetGroup(PredefinedGroups.createFinanceTeam())

        logger.info("Created ${groupRepository.count()} predefined groups")

        // Create administrator employee
        val administrator = Employee(
            firstName = adminFirstName,
            lastName = adminLastName,
            email = adminEmail,
            passwordHash = passwordEncoder.encode(adminPassword),
            employeeNumber = "ADMIN-001",
            jobTitle = "System Administrator",
            department = "IT & Operations",
            status = EmployeeStatus.ACTIVE,
            groups = mutableSetOf(superAdminGroup),
            isSystemAccount = true // Mark as system account
        )

        administrator.tenantId = "SYSTEM" // System-level employee

        val savedAdmin = employeeRepository.save(administrator)

        logger.warn("IMPORTANT: System initialized with administrator account")
        logger.warn("  Email: ${savedAdmin.email}")
        logger.warn("  Employee Number: ${savedAdmin.employeeNumber}")
        logger.warn("  Groups: ${savedAdmin.groups.joinToString { it.name }}")
        logger.warn("  Permissions: ${savedAdmin.getAllPermissions().size} total permissions")
        logger.warn("  CHANGE THE DEFAULT PASSWORD IMMEDIATELY!")

        return savedAdmin
    }

    /**
     * Create predefined groups if they don't exist.
     * This allows re-running to add new groups without affecting existing data.
     */
    fun ensurePredefinedGroupsExist() {
        logger.info("Ensuring all predefined groups exist...")

        createOrGetGroup(PredefinedGroups.createSuperAdmin())
        createOrGetGroup(PredefinedGroups.createSupportAgent())
        createOrGetGroup(PredefinedGroups.createSupportManager())
        createOrGetGroup(PredefinedGroups.createSalesTeam())
        createOrGetGroup(PredefinedGroups.createFinanceTeam())

        logger.info("All predefined groups are present in database")
    }

    /**
     * Get system initialization status and details.
     */
    fun getInitializationStatus(): InitializationStatus {
        val employeeCount = employeeRepository.count()
        val groupCount = groupRepository.count()
        val hasAdmin = employeeRepository.findAll()
            .any { it.groups.any { group -> group.name == "Super Admin" } }

        return InitializationStatus(
            isInitialized = employeeCount > 0,
            employeeCount = employeeCount.toInt(),
            groupCount = groupCount.toInt(),
            hasAdministrator = hasAdmin,
            predefinedGroupsPresent = groupCount >= 5
        )
    }

    /**
     * Create a group if it doesn't exist, or return existing one.
     */
    private fun createOrGetGroup(groupTemplate: EmployeeGroup): EmployeeGroup {
        // Check if group with this name already exists
        val existing = groupRepository.findByName(groupTemplate.name)
        if (existing != null) {
            logger.debug("Group '${groupTemplate.name}' already exists, skipping creation")
            return existing
        }

        // Create new group
        groupTemplate.tenantId = "SYSTEM" // System-level groups
        val saved = groupRepository.save(groupTemplate)

        logger.info("Created predefined group: ${saved.name} with ${saved.permissions.size} permissions")

        return saved
    }
}

/**
 * Data class representing system initialization status.
 */
data class InitializationStatus(
    val isInitialized: Boolean,
    val employeeCount: Int,
    val groupCount: Int,
    val hasAdministrator: Boolean,
    val predefinedGroupsPresent: Boolean
)
