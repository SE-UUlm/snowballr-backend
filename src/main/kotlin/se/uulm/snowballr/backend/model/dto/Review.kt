package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.ReviewTable
import snowballr.ReviewOuterClass
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.orEmpty

/**
 * DTO of [ReviewTable].
 */
data class Review(
    val id: UUID,
    val projectPaperId: UUID,
    val userId: UUID,
    val decision: ReviewOuterClass.ReviewDecision,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)

fun Review.toGrpcReview(selectedCriteriaIds: List<String>): ReviewOuterClass.Review = ReviewOuterClass.Review
    .newBuilder()
    .setId(id.toString())
    .setUserId(userId.toString())
    .setDecision(decision)
    .addAllSelectedCriteriaIds(selectedCriteriaIds)
    .build()

fun List<Review>.toGrpcReviews(reviewSelectedCriteriaMap: Map<Review, List<String>>): ReviewOuterClass.Review.List =
    ReviewOuterClass.Review.List
        .newBuilder()
        .addAllReviews(
            this.map { review ->
                val selectedCriteria = reviewSelectedCriteriaMap[review].orEmpty()

                review.toGrpcReview(
                    selectedCriteria,
                )
            },
        )
        .build()
