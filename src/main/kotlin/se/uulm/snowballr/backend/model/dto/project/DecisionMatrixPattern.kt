package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision

/**
 * A single pattern in the [ReviewDecisionMatrix].
 *
 * Defines the pattern of [ReviewDecision]s and in what final [PaperDecision] that results.
 */
data class DecisionMatrixPattern(
    val decision: PaperDecision,
    val entries: List<DecisionMatrixPatternEntry>,
)
