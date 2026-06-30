package se.uulm.snowballr.backend.model.outgoing.review

import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import snowballr.ReviewOuterClass
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
    }
}

fun ReviewResponse.toGrpc(): ReviewOuterClass.Review = ReviewOuterClass.Review
    .newBuilder()
    .setId(id.toString())
    .setUserId(userId.toString())
    .setDecision(decision.toGrpc())
    .addAllSelectedCriteriaIds(selectedCriteriaIds.map { it.toString() })
    .build()

fun List<ReviewResponse>.toGrpcReviews(): ReviewOuterClass.Review.List = ReviewOuterClass.Review.List
    .newBuilder()
    .addAllReviews(this.map { it.toGrpc() })
    .build()
