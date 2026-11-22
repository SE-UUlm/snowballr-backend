package se.uulm.snowballr.backend.env

/**
 * Determines which fetcher manager implementation is used to fetch papers.
 *
 * - **PYTHON**: Uses the Python plugin fetchers.
 * - **DUMMY**: Uses hard-coded dummy fetchers that return generated papers. Not allowed in production.
 */
enum class FetcherImplementationType {
    PYTHON,
    DUMMY,
}
