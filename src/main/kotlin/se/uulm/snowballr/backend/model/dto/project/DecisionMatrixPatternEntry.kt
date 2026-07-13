package se.uulm.snowballr.backend.model.dto.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision

/**
 * An entry in the [DecisionMatrixPattern] containing the decision and the number of occurrences of said decision.
 */
@Serializable
data class DecisionMatrixPatternEntry(
    @SerialName("decision")
    val decision: ReviewDecision,
    @SerialName("count")
    val count: Int,
)
