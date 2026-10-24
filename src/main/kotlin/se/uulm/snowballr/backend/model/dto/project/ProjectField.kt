package se.uulm.snowballr.backend.model.dto.project

/**
 * The updatable fields of a project.
 *
 * Each field declares its [scope] once, which determines which part of the project it belongs to.
 */
enum class ProjectField(val scope: Scope) {
    NAME(Scope.PROJECT),
    STATUS(Scope.PROJECT),

    // Settings
    SIMILARITY_THRESHOLD(Scope.SETTINGS),
    SNOWBALLING_TYPE(Scope.SETTINGS),
    REVIEW_MAYBE_ALLOWED(Scope.SETTINGS),
    FETCHERS(Scope.SETTINGS),
    NUMBER_OF_REVIEWERS(Scope.DECISION_MATRIX),
    DECISION_MATRIX_PATTERNS(Scope.DECISION_MATRIX),
    ;

    enum class Scope {
        /** Fields of the project itself. */
        PROJECT,

        /** Fields of the project settings. */
        SETTINGS,

        /** Like [SETTINGS], but part of the decision matrix of the project settings. */
        DECISION_MATRIX,
    }

    val isSettingsField get() = scope == Scope.SETTINGS || scope == Scope.DECISION_MATRIX

    val isDecisionMatrixField get() = scope == Scope.DECISION_MATRIX
}
