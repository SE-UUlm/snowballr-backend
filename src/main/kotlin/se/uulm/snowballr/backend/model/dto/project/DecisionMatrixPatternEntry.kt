package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import snowballr.ProjectOuterClass

/**
 * An entry in the [DecisionMatrixPattern] containing the decision and the number of occurrences of said decision.
 */
data class DecisionMatrixPatternEntry(
    val decision: ReviewDecision,
    val count: Int,
) {
    companion object {
        fun fromGrpc(entry: ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry) = DecisionMatrixPatternEntry(
            decision = ReviewDecision.fromGrpc(entry.reviewDecision),
            count = entry.count.toInt(),
        )
    }

    fun toGrpc(): ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry =
        ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry.newBuilder()
            .setReviewDecision(decision.toGrpc())
            .setCount(count.toLong())
            .build()
}
