package se.uulm.snowballr.backend.model.dto.projectpaper

import snowballr.ProjectOuterClass

/**
 * The decision on how the paper is treated in respect to the SLR.
 */
enum class PaperDecision {
    /**
     * The project paper has not been reviewed yet.
     */
    UNREVIEWED,

    /**
     * The project paper is currently in review.
     *
     * This means that there's at least one review for this project paper.
     */
    IN_REVIEW,

    /**
     * The paper was declined and is not considered part of the final SLR paper set.
     */
    DECLINED,

    /**
     * The paper was accepted and is considered part of the final SLR paper set.
     */
    ACCEPTED,

    ;

    companion object {
        fun fromGrpc(decision: ProjectOuterClass.PaperDecision): PaperDecision = when (decision) {
            ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED -> UNREVIEWED
            ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW -> IN_REVIEW
            ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED -> DECLINED
            ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED -> ACCEPTED
            ProjectOuterClass.PaperDecision.UNRECOGNIZED, ProjectOuterClass.PaperDecision.PAPER_DECISION_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc(): ProjectOuterClass.PaperDecision = when (this) {
        UNREVIEWED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED
        IN_REVIEW -> ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW
        DECLINED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED
        ACCEPTED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED
    }
}
