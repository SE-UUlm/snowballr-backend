package se.uulm.snowballr.backend.model.dto.projectpaper

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
}
