package se.uulm.snowballr.backend.model.dto.project

enum class ProjectField {
    NAME,
    STATUS,
    SIMILARITY_THRESHOLD,
    SNOWBALLING_TYPE,
    REVIEW_MAYBE_ALLOWED,
    FETCHERS,
    NUMBER_OF_REVIEWERS,
    DECISION_MATRIX_PATTERNS,

    ;

    fun isDecisionMatrixField() = when (this) {
        NUMBER_OF_REVIEWERS, DECISION_MATRIX_PATTERNS -> true

        NAME,
        STATUS,
        SIMILARITY_THRESHOLD,
        SNOWBALLING_TYPE,
        REVIEW_MAYBE_ALLOWED,
        FETCHERS,
        -> false
    }

    fun isSettingsField() = when (this) {
        SIMILARITY_THRESHOLD,
        SNOWBALLING_TYPE,
        REVIEW_MAYBE_ALLOWED,
        FETCHERS,
        NUMBER_OF_REVIEWERS,
        DECISION_MATRIX_PATTERNS,
        -> true

        NAME,
        STATUS,
        -> false
    }
}
