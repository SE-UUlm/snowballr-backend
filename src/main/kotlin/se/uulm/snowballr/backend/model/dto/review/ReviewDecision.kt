package se.uulm.snowballr.backend.model.dto.review

/**
 * The decision based on the review of a paper.
 */
enum class ReviewDecision {
    /**
     * The reviewer declined the paper.
     */
    DECLINED,

    /**
     * The reviewer was not sure in their decision.
     */
    MAYBE,

    /**
     * The reviewer accepted the paper.
     */
    ACCEPTED,
}
