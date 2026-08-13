package se.uulm.snowballr.backend.model.dto.project

enum class ProjectField(val grpcPath: String) {
    NAME("project.name"),
    STATUS("project.status"),
    SIMILARITY_THRESHOLD("project.settings.similarity_threshold"),
    SNOWBALLING_TYPE("project.settings.snowballing_type"),
    REVIEW_MAYBE_ALLOWED("project.settings.review_maybe_allowed"),
    FETCHERS("project.settings.fetchers"),
    NUMBER_OF_REVIEWERS("project.settings.decision_matrix.number_of_reviewers"),
    DECISION_MATRIX_PATTERNS("project.settings.decision_matrix.patterns"),

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
