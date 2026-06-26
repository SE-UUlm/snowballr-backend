package se.uulm.snowballr.backend.model.dto.projectpaper

import snowballr.ProjectOuterClass

/**
 * The decision on how the paper is treated in respect to the SLR.
 */
enum class PaperDecision {
    /**
     * The project paper has not been reviewed yet.
     */
    PAPER_DECISION_UNREVIEWED,

    /**
     * The project paper is currently in review.
     *
     * This means that there's at least one review for this project paper.
     */
    PAPER_DECISION_IN_REVIEW,

    /**
     * The paper was declined and is not considered part of the final SLR paper set.
     */
    PAPER_DECISION_DECLINED,

    /**
     * The paper was accepted and is considered part of the final SLR paper set.
     */
    PAPER_DECISION_ACCEPTED,

    ;

    companion object {
        fun fromGrpc(decision: ProjectOuterClass.PaperDecision): PaperDecision = when (decision) {
            ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED -> PAPER_DECISION_UNREVIEWED
            ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW -> PAPER_DECISION_IN_REVIEW
            ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED -> PAPER_DECISION_DECLINED
            ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED -> PAPER_DECISION_ACCEPTED
            ProjectOuterClass.PaperDecision.UNRECOGNIZED, ProjectOuterClass.PaperDecision.PAPER_DECISION_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc(): ProjectOuterClass.PaperDecision = when (this) {
        PAPER_DECISION_UNREVIEWED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED
        PAPER_DECISION_IN_REVIEW -> ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW
        PAPER_DECISION_DECLINED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED
        PAPER_DECISION_ACCEPTED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED
    }
}
