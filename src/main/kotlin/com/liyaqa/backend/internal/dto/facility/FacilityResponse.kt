package com.liyaqa.backend.internal.dto.facility

import com.liyaqa.backend.internal.domain.facility.FacilityStatus
import com.liyaqa.backend.internal.domain.facility.SportFacility
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Response DTO for sport facility with full details.
 */
data class FacilityResponse(
    val id: UUID,
    val tenantId: String,
    val ownerTenantId: UUID,
    val ownerTenantName: String,

    val name: String,
    val description: String?,
    val facilityType: String,

    val contactEmail: String,
    val contactPhone: String?,
    val website: String?,

    val socialFacebook: String?,
    val socialInstagram: String?,
    val socialTwitter: String?,

    val establishedDate: LocalDate?,
    val registrationNumber: String?,

    val amenities: List<String>,
    val operatingHours: String?,

    val status: FacilityStatus,

    val timezone: String,
    val locale: String,
    val currency: String,

    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long
) {
    companion object {
        fun from(facility: SportFacility): FacilityResponse {
            return FacilityResponse(
                id = facility.id!!,
                tenantId = facility.tenantId,
                ownerTenantId = facility.owner.id!!,
                ownerTenantName = facility.owner.name,
                name = facility.name,
                description = facility.description,
                facilityType = facility.facilityType,
                contactEmail = facility.contactEmail,
                contactPhone = facility.contactPhone,
                website = facility.website,
                socialFacebook = facility.socialFacebook,
                socialInstagram = facility.socialInstagram,
                socialTwitter = facility.socialTwitter,
                establishedDate = facility.establishedDate,
                registrationNumber = facility.registrationNumber,
                amenities = facility.getAmenitiesList(),
                operatingHours = facility.operatingHours,
                status = facility.status,
                timezone = facility.timezone,
                locale = facility.locale,
                currency = facility.currency,
                createdAt = facility.createdAt,
                updatedAt = facility.updatedAt,
                version = facility.version
            )
        }
    }
}

/**
 * Response DTO for sport facility with basic information (for lists).
 */
data class FacilityBasicResponse(
    val id: UUID,
    val tenantId: String,
    val name: String,
    val facilityType: String,
    val status: FacilityStatus,
    val contactEmail: String,
    val city: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(facility: SportFacility): FacilityBasicResponse {
            return FacilityBasicResponse(
                id = facility.id!!,
                tenantId = facility.tenantId,
                name = facility.name,
                facilityType = facility.facilityType,
                status = facility.status,
                contactEmail = facility.contactEmail,
                city = null, // Will be populated from main branch if needed
                createdAt = facility.createdAt
            )
        }
    }
}
