package com.liyaqa.backend.facility.booking.service

import com.liyaqa.backend.facility.booking.data.CourtRepository
import com.liyaqa.backend.facility.booking.domain.Court
import com.liyaqa.backend.facility.booking.domain.CourtStatus
import com.liyaqa.backend.facility.booking.domain.CourtType
import com.liyaqa.backend.facility.booking.dto.*
import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for managing courts.
 */
@Service
@Transactional
class CourtService(
    private val courtRepository: CourtRepository,
    private val facilityRepository: SportFacilityRepository,
    private val branchRepository: FacilityBranchRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new court.
     */
    fun createCourt(request: CourtCreateRequest): CourtResponse {
        // Validate facility exists
        val facility = facilityRepository.findById(request.facilityId)
            .orElseThrow { EntityNotFoundException("Facility not found: ${request.facilityId}") }

        // Validate branch exists and belongs to facility
        val branch = branchRepository.findById(request.branchId)
            .orElseThrow { EntityNotFoundException("Branch not found: ${request.branchId}") }

        if (branch.facility.id != facility.id) {
            throw IllegalArgumentException("Branch does not belong to the specified facility")
        }

        // Check for duplicate court name in branch
        if (courtRepository.existsByBranchIdAndName(request.branchId, request.name)) {
            throw IllegalArgumentException("Court name '${request.name}' already exists for this branch")
        }

        val court = Court(
            facility = facility,
            branch = branch,
            name = request.name,
            description = request.description,
            courtType = request.courtType,
            surfaceType = request.surfaceType,
            isIndoor = request.isIndoor,
            hasLighting = request.hasLighting,
            maxPlayers = request.maxPlayers,
            hourlyRate = request.hourlyRate,
            currency = request.currency,
            peakHourRate = request.peakHourRate,
            minBookingDuration = request.minBookingDuration,
            maxBookingDuration = request.maxBookingDuration,
            bookingInterval = request.bookingInterval,
            advanceBookingDays = request.advanceBookingDays,
            cancellationHours = request.cancellationHours,
            amenities = request.amenities,
            displayOrder = request.displayOrder,
            imageUrl = request.imageUrl
        )

        court.tenantId = facility.tenantId

        val savedCourt = courtRepository.save(court)

        logger.info("Court created: ${savedCourt.name} (${savedCourt.courtType}) at branch ${branch.name}")

        return CourtResponse.from(savedCourt)
    }

    /**
     * Get court by ID.
     */
    @Transactional(readOnly = true)
    fun getCourtById(id: UUID): CourtResponse {
        val court = courtRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Court not found: $id") }

        return CourtResponse.from(court)
    }

    /**
     * Update court information.
     */
    fun updateCourt(id: UUID, request: CourtUpdateRequest): CourtResponse {
        val court = courtRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Court not found: $id") }

        request.name?.let {
            if (it != court.name && courtRepository.existsByBranchIdAndName(court.branch.id!!, it)) {
                throw IllegalArgumentException("Court name '$it' already exists for this branch")
            }
            court.name = it
        }

        request.description?.let { court.description = it }
        request.surfaceType?.let { court.surfaceType = it }
        request.isIndoor?.let { court.isIndoor = it }
        request.hasLighting?.let { court.hasLighting = it }
        request.maxPlayers?.let { court.maxPlayers = it }
        request.hourlyRate?.let { court.hourlyRate = it }
        request.currency?.let { court.currency = it }
        request.peakHourRate?.let { court.peakHourRate = it }
        request.minBookingDuration?.let { court.minBookingDuration = it }
        request.maxBookingDuration?.let { court.maxBookingDuration = it }
        request.bookingInterval?.let { court.bookingInterval = it }
        request.advanceBookingDays?.let { court.advanceBookingDays = it }
        request.cancellationHours?.let { court.cancellationHours = it }
        request.status?.let { court.status = it }
        request.maintenanceNotes?.let { court.maintenanceNotes = it }
        request.amenities?.let { court.amenities = it }
        request.displayOrder?.let { court.displayOrder = it }
        request.imageUrl?.let { court.imageUrl = it }

        val savedCourt = courtRepository.save(court)

        logger.info("Court updated: ${savedCourt.name}")

        return CourtResponse.from(savedCourt)
    }

    /**
     * Delete court.
     */
    fun deleteCourt(id: UUID) {
        val court = courtRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Court not found: $id") }

        courtRepository.delete(court)

        logger.info("Court deleted: ${court.name}")
    }

    /**
     * Get courts by facility.
     */
    @Transactional(readOnly = true)
    fun getCourtsByFacility(facilityId: UUID): List<CourtResponse> {
        return courtRepository.findByFacilityId(facilityId)
            .map { CourtResponse.from(it) }
    }

    /**
     * Get courts by branch.
     */
    @Transactional(readOnly = true)
    fun getCourtsByBranch(branchId: UUID): List<CourtResponse> {
        return courtRepository.findByBranchId(branchId)
            .map { CourtResponse.from(it) }
    }

    /**
     * Get active courts by branch.
     */
    @Transactional(readOnly = true)
    fun getActiveCourtsByBranch(branchId: UUID): List<CourtBasicResponse> {
        return courtRepository.findActiveByBranchId(branchId)
            .map { CourtBasicResponse.from(it) }
    }

    /**
     * Get courts by branch and type.
     */
    @Transactional(readOnly = true)
    fun getCourtsByBranchAndType(branchId: UUID, courtType: CourtType): List<CourtBasicResponse> {
        return courtRepository.findByBranchIdAndCourtType(branchId, courtType)
            .map { CourtBasicResponse.from(it) }
    }

    /**
     * Get courts by branch and status.
     */
    @Transactional(readOnly = true)
    fun getCourtsByBranchAndStatus(branchId: UUID, status: CourtStatus): List<CourtResponse> {
        return courtRepository.findByBranchIdAndStatus(branchId, status)
            .map { CourtResponse.from(it) }
    }

    /**
     * Get indoor courts by branch.
     */
    @Transactional(readOnly = true)
    fun getIndoorCourtsByBranch(branchId: UUID): List<CourtBasicResponse> {
        return courtRepository.findIndoorByBranchId(branchId)
            .map { CourtBasicResponse.from(it) }
    }

    /**
     * Get courts with lighting by branch.
     */
    @Transactional(readOnly = true)
    fun getCourtsWithLightingByBranch(branchId: UUID): List<CourtBasicResponse> {
        return courtRepository.findWithLightingByBranchId(branchId)
            .map { CourtBasicResponse.from(it) }
    }

    /**
     * Count courts by branch.
     */
    @Transactional(readOnly = true)
    fun countCourtsByBranch(branchId: UUID): Long {
        return courtRepository.countByBranchId(branchId)
    }

    /**
     * Count active courts by branch.
     */
    @Transactional(readOnly = true)
    fun countActiveCourtsByBranch(branchId: UUID): Long {
        return courtRepository.countActiveByBranchId(branchId)
    }
}
