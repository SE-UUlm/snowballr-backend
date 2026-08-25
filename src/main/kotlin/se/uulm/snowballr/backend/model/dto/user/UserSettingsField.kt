package se.uulm.snowballr.backend.model.dto.user

enum class UserSettingsField {
    ARE_HOTKEYS_SHOWN,
    IS_REVIEW_MODE_ENABLED,
    CRITERIA_IDS,
    SIMILARITY_THRESHOLD,
    SNOWBALLING_TYPE,
    REVIEW_MAYBE_ALLOWED,
    FETCHERS,
    NUMBER_OF_REVIEWERS,
    DECISION_MATRIX_PATTERNS,
    ;

    fun isDecisionMatrixField() = when (this) {
        ARE_HOTKEYS_SHOWN,
        IS_REVIEW_MODE_ENABLED,
        CRITERIA_IDS,
        SIMILARITY_THRESHOLD,
        SNOWBALLING_TYPE,
        REVIEW_MAYBE_ALLOWED,
        FETCHERS,
        -> false

        NUMBER_OF_REVIEWERS,
        DECISION_MATRIX_PATTERNS,
        -> true
    }
}
