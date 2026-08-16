package se.uulm.snowballr.backend.model.outgoing.review

import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import java.time.OffsetDateTime
import java.util.UUID

data class ReviewResponse(
    val id: UUID,
    val projectPaperId: UUID,
    val userId: UUID,
    val decision: ReviewDecision,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val selectedCriteriaIds: List<UUID>,
) {
    companion object {
        fun fromReviewAndIds(review: Review, selectedCriteriaIds: List<UUID>) = ReviewResponse(
            id = review.id,
            projectPaperId = review.projectPaperId,
            userId = review.userId,
            decision = review.decision,
            createdAt = review.createdAt,
            modifiedAt = review.modifiedAt,
            selectedCriteriaIds = selectedCriteriaIds,
        )

        fun fromReviewWithSelectedCriteriaIds(review: ReviewWithSelectedCriteriaIds) =
            fromReviewAndIds(review.review, review.selectedCriteriaIds)
    }
}
