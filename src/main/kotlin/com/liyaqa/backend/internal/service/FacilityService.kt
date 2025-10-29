package com.liyaqa.backend.internal.service

import com.liyaqa.backend.internal.domain.audit.EntityType
import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.domain.employee.Permission
import com.liyaqa.backend.internal.domain.facility.*
import com.liyaqa.backend.internal.dto.facility.*
import com.liyaqa.backend.internal.repository.FacilityBranchRepository
import com.liyaqa.backend.internal.repository.SportFacilityRepository
import com.liyaqa.backend.internal.repository.TenantRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for managing sport facilities and their branches.
 *
 * Handles the complete lifecycle of sport facilities (clubs) and their
 * physical branch locations, with comprehensive audit logging.
 *
 * Business Rules:
 * - Facilities must belong to an active tenant
 * - Only FACILITY_CREATE permission can create facilities
 * - Only FACILITY_MANAGE_BRANCHES can create/edit branches
 * - Each facility can have one main branch
 * - All actions are audit logged for compliance
 */
@Service
@Transactional
class FacilityService(
    private val facilityRepository: SportFacilityRepository,
    private val branchRepository: FacilityBranchRepository,
    private val tenantRepository: TenantRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // ========== FACILITY OPERATIONS ==========

    /**
     * Create a new sport facility.
     */
    fun createFacility(request: FacilityCreateRequest, createdBy: Employee): FacilityResponse {
        checkPermission(createdBy, Permission.FACILITY_CREATE)

        // Validate tenant exists
        val tenant = tenantRepository.findById(request.ownerTenantId)
            .orElseThrow { EntityNotFoundException("Tenant not found: ${request.ownerTenantId}") }

        // Check for duplicate name
        if (facilityRepository.existsByOwnerAndName(tenant.id!!, request.name)) {
            throw IllegalArgumentException("Facility name '${request.name}' already exists for this tenant")
        }

        val facility = SportFacility(
            owner = tenant,
            name = request.name,
            description = request.description,
            facilityType = request.facilityType,
            contactEmail = request.contactEmail,
            contactPhone = request.contactPhone,
            website = request.website,
            socialFacebook = request.socialFacebook,
            socialInstagram = request.socialInstagram,
            socialTwitter = request.socialTwitter,
            establishedDate = request.establishedDate,
            registrationNumber = request.registrationNumber,
            amenities = request.amenities?.joinToString(","),
            operatingHours = request.operatingHours,
            timezone = request.timezone,
            locale = request.locale,
            currency = request.currency,
            createdBy = createdBy
        )

        // Set tenant ID for multi-tenancy
        facility.tenantId = tenant.tenantId

        val savedFacility = facilityRepository.save(facility)

        auditService.logCreate(
            createdBy,
            EntityType.FACILITY,
            savedFacility.id!!,
            mapOf(
                "name" to savedFacility.name,
                "type" to savedFacility.facilityType,
                "tenant" to tenant.name
            )
        )

        logger.info("Facility created: ${savedFacility.name} for tenant ${tenant.tenantId} by ${createdBy.email}")

        return FacilityResponse.from(savedFacility)
    }

    /**
     * Get facility by ID.
     */
    @Transactional(readOnly = true)
    fun getFacilityById(id: UUID, requestedBy: Employee): FacilityResponse {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        val facility = facilityRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Facility not found: $id") }

        return FacilityResponse.from(facility)
    }

    /**
     * Update facility.
     */
    fun updateFacility(id: UUID, request: FacilityUpdateRequest, updatedBy: Employee): FacilityResponse {
        checkPermission(updatedBy, Permission.FACILITY_UPDATE)

        val facility = facilityRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Facility not found: $id") }

        val changes = mutableMapOf<String, String>()

        request.name?.let {
            if (it != facility.name) {
                changes["name"] = "${facility.name} -> $it"
                facility.name = it
            }
        }
        request.description?.let { facility.description = it }
        request.facilityType?.let {
            if (it != facility.facilityType) {
                changes["type"] = "${facility.facilityType} -> $it"
                facility.facilityType = it
            }
        }
        request.contactEmail?.let { facility.contactEmail = it }
        request.contactPhone?.let { facility.contactPhone = it }
        request.website?.let { facility.website = it }
        request.socialFacebook?.let { facility.socialFacebook = it }
        request.socialInstagram?.let { facility.socialInstagram = it }
        request.socialTwitter?.let { facility.socialTwitter = it }
        request.establishedDate?.let { facility.establishedDate = it }
        request.registrationNumber?.let { facility.registrationNumber = it }
        request.amenities?.let { facility.setAmenitiesList(it) }
        request.operatingHours?.let { facility.operatingHours = it }
        request.status?.let {
            if (it != facility.status) {
                changes["status"] = "${facility.status} -> $it"
                facility.status = it
            }
        }
        request.timezone?.let { facility.timezone = it }
        request.locale?.let { facility.locale = it }
        request.currency?.let { facility.currency = it }

        val savedFacility = facilityRepository.save(facility)

        if (changes.isNotEmpty()) {
            auditService.logUpdate(updatedBy, EntityType.FACILITY, savedFacility.id!!, changes)
        }

        logger.info("Facility updated: ${facility.name} by ${updatedBy.email}")

        return FacilityResponse.from(savedFacility)
    }

    /**
     * Delete facility (cascade deletes branches).
     */
    fun deleteFacility(id: UUID, deletedBy: Employee) {
        checkPermission(deletedBy, Permission.FACILITY_DELETE)

        val facility = facilityRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Facility not found: $id") }

        val branchCount = branchRepository.countByFacilityId(id)

        facilityRepository.delete(facility)

        auditService.logDelete(
            deletedBy,
            EntityType.FACILITY,
            id,
            mapOf(
                "name" to facility.name,
                "type" to facility.facilityType,
                "branches_deleted" to branchCount.toString()
            )
        )

        logger.warn("Facility deleted: ${facility.name} with $branchCount branches by ${deletedBy.email}")
    }

    /**
     * Search facilities.
     */
    @Transactional(readOnly = true)
    fun searchFacilities(
        searchTerm: String?,
        status: FacilityStatus?,
        facilityType: String?,
        ownerTenantId: UUID?,
        pageable: Pageable,
        requestedBy: Employee
    ): Page<FacilityBasicResponse> {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        val page = facilityRepository.searchFacilities(
            searchTerm, status, facilityType, ownerTenantId, pageable
        )

        return page.map { FacilityBasicResponse.from(it) }
    }

    /**
     * Get facilities by tenant.
     */
    @Transactional(readOnly = true)
    fun getFacilitiesByTenant(tenantId: UUID, requestedBy: Employee): List<FacilityResponse> {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        return facilityRepository.findByOwnerTenantId(tenantId)
            .map { FacilityResponse.from(it) }
    }

    // ========== BRANCH OPERATIONS ==========

    /**
     * Create a new facility branch.
     */
    fun createBranch(request: BranchCreateRequest, createdBy: Employee): BranchResponse {
        checkPermission(createdBy, Permission.FACILITY_MANAGE_BRANCHES)

        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Check for duplicate name within facility
        if (branchRepository.existsByFacilityAndName(facility.id!!, request.name)) {
            throw IllegalArgumentException("Branch name '${request.name}' already exists for this facility")
        }

        // If setting as main branch, unset any existing main branch
        if (request.isMainBranch) {
            branchRepository.findMainBranchByFacilityId(facility.id!!)?.let { existingMain ->
                existingMain.isMainBranch = false
                branchRepository.save(existingMain)
            }
        }

        val branch = FacilityBranch(
            facility = facility,
            name = request.name,
            description = request.description,
            isMainBranch = request.isMainBranch,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            stateProvince = request.stateProvince,
            postalCode = request.postalCode,
            country = request.country,
            latitude = request.latitude,
            longitude = request.longitude,
            contactEmail = request.contactEmail,
            contactPhone = request.contactPhone,
            totalCourts = request.totalCourts,
            totalCapacity = request.totalCapacity,
            amenities = request.amenities?.joinToString(","),
            operatingHours = request.operatingHours,
            timezone = request.timezone,
            createdBy = createdBy
        )

        // Set tenant ID for multi-tenancy
        branch.tenantId = facility.tenantId

        val savedBranch = branchRepository.save(branch)

        auditService.logCreate(
            createdBy,
            EntityType.FACILITY_BRANCH,
            savedBranch.id!!,
            mapOf(
                "name" to savedBranch.name,
                "facility" to facility.name,
                "city" to savedBranch.city
            )
        )

        logger.info("Branch created: ${savedBranch.name} for facility ${facility.name} by ${createdBy.email}")

        return BranchResponse.from(savedBranch)
    }

    /**
     * Get branch by ID.
     */
    @Transactional(readOnly = true)
    fun getBranchById(id: UUID, requestedBy: Employee): BranchResponse {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        val branch = branchRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Branch not found: $id") }

        return BranchResponse.from(branch)
    }

    /**
     * Update branch.
     */
    fun updateBranch(id: UUID, request: BranchUpdateRequest, updatedBy: Employee): BranchResponse {
        checkPermission(updatedBy, Permission.FACILITY_MANAGE_BRANCHES)

        val branch = branchRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Branch not found: $id") }

        val changes = mutableMapOf<String, String>()

        request.name?.let {
            if (it != branch.name) {
                changes["name"] = "${branch.name} -> $it"
                branch.name = it
            }
        }
        request.description?.let { branch.description = it }
        request.isMainBranch?.let {
            if (it != branch.isMainBranch) {
                if (it) {
                    // Unset existing main branch
                    branchRepository.findMainBranchByFacilityId(branch.facility.id!!)?.let { existingMain ->
                        existingMain.isMainBranch = false
                        branchRepository.save(existingMain)
                    }
                }
                changes["is_main"] = "${branch.isMainBranch} -> $it"
                branch.isMainBranch = it
            }
        }
        request.addressLine1?.let { branch.addressLine1 = it }
        request.addressLine2?.let { branch.addressLine2 = it }
        request.city?.let { branch.city = it }
        request.stateProvince?.let { branch.stateProvince = it }
        request.postalCode?.let { branch.postalCode = it }
        request.country?.let { branch.country = it }
        request.latitude?.let { branch.latitude = it }
        request.longitude?.let { branch.longitude = it }
        request.contactEmail?.let { branch.contactEmail = it }
        request.contactPhone?.let { branch.contactPhone = it }
        request.totalCourts?.let { branch.totalCourts = it }
        request.totalCapacity?.let { branch.totalCapacity = it }
        request.amenities?.let { branch.setAmenitiesList(it) }
        request.operatingHours?.let { branch.operatingHours = it }
        request.status?.let {
            if (it != branch.status) {
                changes["status"] = "${branch.status} -> $it"
                branch.status = it
            }
        }
        request.timezone?.let { branch.timezone = it }

        val savedBranch = branchRepository.save(branch)

        if (changes.isNotEmpty()) {
            auditService.logUpdate(updatedBy, EntityType.FACILITY_BRANCH, savedBranch.id!!, changes)
        }

        logger.info("Branch updated: ${branch.name} by ${updatedBy.email}")

        return BranchResponse.from(savedBranch)
    }

    /**
     * Delete branch.
     */
    fun deleteBranch(id: UUID, deletedBy: Employee) {
        checkPermission(deletedBy, Permission.FACILITY_MANAGE_BRANCHES)

        val branch = branchRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Branch not found: $id") }

        branchRepository.delete(branch)

        auditService.logDelete(
            deletedBy,
            EntityType.FACILITY_BRANCH,
            id,
            mapOf(
                "name" to branch.name,
                "facility" to branch.facility.name,
                "city" to branch.city
            )
        )

        logger.warn("Branch deleted: ${branch.name} by ${deletedBy.email}")
    }

    /**
     * Get branches by facility.
     */
    @Transactional(readOnly = true)
    fun getBranchesByFacility(facilityId: UUID, requestedBy: Employee): List<BranchResponse> {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        return branchRepository.findByFacilityId(facilityId)
            .map { BranchResponse.from(it) }
    }

    /**
     * Search branches.
     */
    @Transactional(readOnly = true)
    fun searchBranches(
        searchTerm: String?,
        status: BranchStatus?,
        facilityId: UUID?,
        city: String?,
        country: String?,
        pageable: Pageable,
        requestedBy: Employee
    ): Page<BranchBasicResponse> {
        checkPermission(requestedBy, Permission.FACILITY_VIEW)

        val page = branchRepository.searchBranches(
            searchTerm, status, facilityId, city, country, pageable
        )

        return page.map { BranchBasicResponse.from(it) }
    }

    // ========== HELPER METHODS ==========

    private fun checkPermission(employee: Employee, permission: Permission) {
        if (!employee.hasPermission(permission)) {
            logger.warn("Employee ${employee.id} lacks permission: ${permission.name}")
            auditService.logUnauthorizedAccess(
                employee,
                "Attempted action requiring ${permission.name}",
                EntityType.FACILITY
            )
            throw SecurityException("Insufficient permissions: ${permission.name} required")
        }
    }
}
