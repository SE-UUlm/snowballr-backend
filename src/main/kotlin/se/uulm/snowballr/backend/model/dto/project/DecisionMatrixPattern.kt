package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import snowballr.ProjectOuterClass

/**
 * A single pattern in the [ReviewDecisionMatrix].
 *
 * Defines the pattern of [ReviewDecision]s and in what final [PaperDecision] that results.
 */
data class DecisionMatrixPattern(
    val decision: PaperDecision,
    val entries: List<DecisionMatrixPatternEntry>,
) {
    companion object {
        fun fromGrpc(pattern: ProjectOuterClass.ReviewDecisionMatrix.Pattern) = DecisionMatrixPattern(
            decision = PaperDecision.fromGrpc(pattern.decision),
            entries = pattern.entriesList.map { DecisionMatrixPatternEntry.fromGrpc(it) },
        )
    }

    fun toGrpc(): ProjectOuterClass.ReviewDecisionMatrix.Pattern =
        ProjectOuterClass.ReviewDecisionMatrix.Pattern.newBuilder()
            .setDecision(decision.toGrpc())
            .addAllEntries(entries.map { it.toGrpc() })
            .build()
}
