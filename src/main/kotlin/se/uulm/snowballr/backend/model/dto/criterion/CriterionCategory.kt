package se.uulm.snowballr.backend.model.dto.criterion

import snowballr.CriterionOuterClass

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

    ;

    companion object {
        fun fromGrpc(category: CriterionOuterClass.CriterionCategory): CriterionCategory = when (category) {
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION -> INCLUSION
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION -> EXCLUSION
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION -> HARD_EXCLUSION
            CriterionOuterClass.CriterionCategory.UNRECOGNIZED,
            CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        INCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION
        EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION
        HARD_EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION
    }
}
