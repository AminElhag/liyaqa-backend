package com.liyaqa.backend.internal.dto.facility

import com.liyaqa.backend.internal.domain.facility.BranchStatus
import com.liyaqa.backend.internal.domain.facility.FacilityBranch
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Response DTO for facility branch with full details.
 */
data class BranchResponse(
    val id: UUID,
    val tenantId: String,
    val facilityId: UUID,
    val facilityName: String,

    val name: String,
    val description: String?,
    val isMainBranch: Boolean,

    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val stateProvince: String?,
    val postalCode: String,
    val country: String,
    val fullAddress: String,

    val latitude: BigDecimal?,
    val longitude: BigDecimal?,

    val contactEmail: String?,
    val contactPhone: String?,

    val totalCourts: Int,
    val totalCapacity: Int,

    val amenities: List<String>,
    val operatingHours: String?,

    val status: BranchStatus,

    val timezone: String,

    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long
) {
    companion object {
        fun from(branch: FacilityBranch): BranchResponse {
            return BranchResponse(
                id = branch.id!!,
                tenantId = branch.tenantId,
                facilityId = branch.facility.id!!,
                facilityName = branch.facility.name,
                name = branch.name,
                description = branch.description,
                isMainBranch = branch.isMainBranch,
                addressLine1 = branch.addressLine1,
                addressLine2 = branch.addressLine2,
                city = branch.city,
                stateProvince = branch.stateProvince,
                postalCode = branch.postalCode,
                country = branch.country,
                fullAddress = branch.getFullAddress(),
                latitude = branch.latitude,
                longitude = branch.longitude,
                contactEmail = branch.contactEmail,
                contactPhone = branch.contactPhone,
                totalCourts = branch.totalCourts,
                totalCapacity = branch.totalCapacity,
                amenities = branch.getAmenitiesList(),
                operatingHours = branch.operatingHours,
                status = branch.status,
                timezone = branch.timezone,
                createdAt = branch.createdAt,
                updatedAt = branch.updatedAt,
                version = branch.version
            )
        }
    }
}

/**
 * Response DTO for facility branch with basic information (for lists).
 */
data class BranchBasicResponse(
    val id: UUID,
    val tenantId: String,
    val facilityId: UUID,
    val name: String,
    val city: String,
    val isMainBranch: Boolean,
    val status: BranchStatus,
    val totalCourts: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(branch: FacilityBranch): BranchBasicResponse {
            return BranchBasicResponse(
                id = branch.id!!,
                tenantId = branch.tenantId,
                facilityId = branch.facility.id!!,
                name = branch.name,
                city = branch.city,
                isMainBranch = branch.isMainBranch,
                status = branch.status,
                totalCourts = branch.totalCourts,
                createdAt = branch.createdAt
            )
        }
    }
}
