package se.uulm.snowballr.backend.model.dto.criterion

import snowballr.CriterionOuterClass

/**
 * Category of a criterion.
 */
enum class CriterionCategory {
    /**
     * If this criterion is fulfilled, the paper should be accepted.
     */
    CRITERION_CATEGORY_INCLUSION,

    /**
     * If this criterion is fulfilled, the paper should be declined.
     */
    CRITERION_CATEGORY_EXCLUSION,

    /**
     * If this criterion is fulfilled, the paper is definitely declined.
     */
    CRITERION_CATEGORY_HARD_EXCLUSION,

    ;

    companion object {
        fun fromGrpc(category: CriterionOuterClass.CriterionCategory): CriterionCategory = when (category) {
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION -> CRITERION_CATEGORY_INCLUSION
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION -> CRITERION_CATEGORY_EXCLUSION
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION -> CRITERION_CATEGORY_HARD_EXCLUSION
            CriterionOuterClass.CriterionCategory.UNRECOGNIZED,
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        CRITERION_CATEGORY_INCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION
        CRITERION_CATEGORY_EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION
        CRITERION_CATEGORY_HARD_EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION
    }
}
