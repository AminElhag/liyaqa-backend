package com.liyaqa.backend.facility.trainer.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.facility.membership.domain.Member
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Trainer review and rating.
 *
 * Design Philosophy:
 * "Feedback is the breakfast of champions."
 *
 * Member reviews help build trainer reputation and provide valuable feedback.
 */
@Entity
@Table(
    name = "trainer_reviews",
    indexes = [
        Index(name = "idx_trainer_review_trainer", columnList = "trainer_id"),
        Index(name = "idx_trainer_review_member", columnList = "member_id"),
        Index(name = "idx_trainer_review_booking", columnList = "booking_id"),
        Index(name = "idx_trainer_review_rating", columnList = "rating"),
        Index(name = "idx_trainer_review_tenant", columnList = "tenant_id")
    ]
)
class TrainerReview(
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    var trainer: Trainer,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    var booking: TrainerBooking? = null,

    // Rating (1-5 stars)
    @Column(name = "rating", precision = 3, scale = 2, nullable = false)
    var rating: BigDecimal,

    // Review Content
    @Column(name = "title", length = 255)
    var title: String? = null,

    @Column(name = "comment", columnDefinition = "TEXT")
    var comment: String? = null,

    // Specific Ratings
    @Column(name = "professionalism_rating")
    var professionalismRating: Int? = null, // 1-5

    @Column(name = "knowledge_rating")
    var knowledgeRating: Int? = null, // 1-5

    @Column(name = "communication_rating")
    var communicationRating: Int? = null, // 1-5

    @Column(name = "motivation_rating")
    var motivationRating: Int? = null, // 1-5

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: ReviewStatus = ReviewStatus.PENDING,

    @Column(name = "is_verified_booking")
    var isVerifiedBooking: Boolean = false,

    // Moderation
    @Column(name = "is_flagged")
    var isFlagged: Boolean = false,

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    var flagReason: String? = null,

    @Column(name = "moderated_at")
    var moderatedAt: Instant? = null,

    @Column(name = "moderated_by")
    var moderatedBy: UUID? = null,

    // Trainer Response
    @Column(name = "trainer_response", columnDefinition = "TEXT")
    var trainerResponse: String? = null,

    @Column(name = "trainer_responded_at")
    var trainerRespondedAt: Instant? = null,

    // Helpful Votes
    @Column(name = "helpful_count")
    var helpfulCount: Int = 0

) : BaseEntity() {

    /**
     * Approve review.
     */
    fun approve() {
        this.status = ReviewStatus.APPROVED
        this.moderatedAt = Instant.now()
    }

    /**
     * Reject review.
     */
    fun reject(reason: String) {
        this.status = ReviewStatus.REJECTED
        this.moderatedAt = Instant.now()
        this.flagReason = reason
    }

    /**
     * Add trainer response.
     */
    fun addTrainerResponse(response: String) {
        this.trainerResponse = response
        this.trainerRespondedAt = Instant.now()
    }
}

/**
 * Review status.
 */
enum class ReviewStatus {
    PENDING,    // Awaiting moderation
    APPROVED,   // Approved and visible
    REJECTED,   // Rejected by moderator
    HIDDEN      // Hidden by admin
}
