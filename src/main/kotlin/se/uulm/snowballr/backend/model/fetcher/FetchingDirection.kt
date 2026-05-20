package se.uulm.snowballr.backend.model.fetcher

/**
 * Direction into which the fetcher should fetch referenced papers.
 */
enum class FetchingDirection(val displayName: String) {
    FORWARD("forward"),
    BACKWARD("backward"),
}
