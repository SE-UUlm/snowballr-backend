package se.uulm.snowballr.backend.model.dto.review

import se.uulm.snowballr.backend.table.ReviewTable
import snowballr.ReviewOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ReviewTable].
 */
data class Review(
    val id: UUID,
    val projectPaperId: UUID,
    val userId: UUID,
    val decision: ReviewDecision,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)

fun Review.toGrpcReview(selectedCriteriaIds: List<String>): ReviewOuterClass.Review = ReviewOuterClass.Review
    .newBuilder()
    .setId(id.toString())
    .setUserId(userId.toString())
    .setDecision(decision.toGrpc())
    .addAllSelectedCriteriaIds(selectedCriteriaIds)
    .build()

/**
 * Checks whether the review accepts the paper.
 *
 * A review is considered to accept the paper if its decision is set to [ReviewDecision.ACCEPTED].
 */
fun Review.doesAcceptPaper() = this.decision == ReviewDecision.ACCEPTED

/**
 * Checks whether the review declines the paper.
 *
 * A review is considered to decline the paper if its decision is set to [ReviewDecision.DECLINED].
 */
fun Review.doesDeclinePaper() = this.decision == ReviewDecision.DECLINED
