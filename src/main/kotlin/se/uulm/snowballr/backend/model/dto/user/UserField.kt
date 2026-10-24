package se.uulm.snowballr.backend.model.dto.user

/**
 * The updatable fields of a user.
 *
 * Each field declares its [scope] once, which determines through which call it can be updated.
 */
enum class UserField(val scope: Scope) {
    EMAIL(Scope.PROFILE),
    FIRST_NAME(Scope.PROFILE),
    LAST_NAME(Scope.PROFILE),
    ROLE(Scope.PROFILE),
    STATUS(Scope.INTERNAL),

    // Settings
    ARE_HOTKEYS_SHOWN(Scope.SETTINGS),
    IS_REVIEW_MODE_ENABLED(Scope.SETTINGS),
    CRITERIA_IDS(Scope.SETTINGS),
    SIMILARITY_THRESHOLD(Scope.SETTINGS),
    SNOWBALLING_TYPE(Scope.SETTINGS),
    REVIEW_MAYBE_ALLOWED(Scope.SETTINGS),
    FETCHERS(Scope.SETTINGS),
    NUMBER_OF_REVIEWERS(Scope.DECISION_MATRIX),
    DECISION_MATRIX_PATTERNS(Scope.DECISION_MATRIX),
    ;

    enum class Scope {
        /** Fields that can be updated via the update user call. */
        PROFILE,

        /** Fields that cannot be updated via any update call, but only via dedicated calls. */
        INTERNAL,

        /** Fields of the user settings that can be updated via the update user settings call. */
        SETTINGS,

        /** Like [SETTINGS], but part of the decision matrix of the default project settings. */
        DECISION_MATRIX,
    }

    val isUserUpdateField get() = scope == Scope.PROFILE

    val isSettingsField get() = scope == Scope.SETTINGS || scope == Scope.DECISION_MATRIX

    val isDecisionMatrixField get() = scope == Scope.DECISION_MATRIX
}
