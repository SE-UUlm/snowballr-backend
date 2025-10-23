package se.uulm.snowballr.backend.model

/**
 * Represents the navigation direction within a collection of papers.
 *
 * @property displayName A human-readable representation of the navigation direction.
 */
enum class PaperNavigationDirection(val displayName: String) {
    NEXT("next"),
    PREVIOUS("previous"),
}
