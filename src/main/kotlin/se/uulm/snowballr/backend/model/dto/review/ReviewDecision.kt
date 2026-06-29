package se.uulm.snowballr.backend.model.dto.review

import snowballr.ReviewOuterClass

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

    ;

    companion object {
        fun fromGrpc(decision: ReviewOuterClass.ReviewDecision): ReviewDecision = when (decision) {
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED -> DECLINED
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE -> MAYBE
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED -> ACCEPTED
            ReviewOuterClass.ReviewDecision.UNRECOGNIZED,
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        DECLINED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED
        MAYBE -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE
        ACCEPTED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED
    }
}
