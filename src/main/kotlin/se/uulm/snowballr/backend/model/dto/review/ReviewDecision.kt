package se.uulm.snowballr.backend.model.dto.review

import snowballr.ReviewOuterClass

/**
 * The decision based on the review of a paper.
 */
enum class ReviewDecision {
    /**
     * The reviewer declined the paper.
     */
    REVIEW_DECISION_DECLINED,

    /**
     * The reviewer was not sure in their decision.
     */
    REVIEW_DECISION_MAYBE,

    /**
     * The reviewer accepted the paper.
     */
    REVIEW_DECISION_ACCEPTED,

    ;

    companion object {
        fun fromGrpc(decision: ReviewOuterClass.ReviewDecision): ReviewDecision = when (decision) {
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED -> REVIEW_DECISION_DECLINED
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE -> REVIEW_DECISION_MAYBE
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED -> REVIEW_DECISION_ACCEPTED
            ReviewOuterClass.ReviewDecision.UNRECOGNIZED,
            ReviewOuterClass.ReviewDecision.REVIEW_DECISION_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        REVIEW_DECISION_DECLINED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED
        REVIEW_DECISION_MAYBE -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE
        REVIEW_DECISION_ACCEPTED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED
    }
}
