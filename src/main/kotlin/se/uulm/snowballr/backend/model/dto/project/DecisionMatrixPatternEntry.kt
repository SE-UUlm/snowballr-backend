package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.dto.review.ReviewDecision

/**
 * An entry in the [DecisionMatrixPattern] containing the decision and the number of occurrences of said decision.
 */
data class DecisionMatrixPatternEntry(
    val decision: ReviewDecision,
    val count: Int,
)
