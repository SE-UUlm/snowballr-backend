package se.uulm.snowballr.backend.model.dto.criterion

/**
 * Category of a criterion.
 */
enum class CriterionCategory {
    /**
     * If this criterion is fulfilled, the paper should be accepted.
     */
    INCLUSION,

    /**
     * If this criterion is fulfilled, the paper should be declined.
     */
    EXCLUSION,

    /**
     * If this criterion is fulfilled, the paper is definitely declined.
     */
    HARD_EXCLUSION,
}
