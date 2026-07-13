package se.uulm.snowballr.backend.model.dto.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision

/**
 * A single pattern in the [ReviewDecisionMatrix].
 *
 * Defines the pattern of [ReviewDecision]s and in what final [PaperDecision] that results.
 */
@Serializable
data class DecisionMatrixPattern(
    @SerialName("decision")
    val decision: PaperDecision,
    @SerialName("entries")
    val entries: List<DecisionMatrixPatternEntry>,
)
