package se.uulm.snowballr.backend.model

/**
 * Enum class representing various sources for scholarly document fetching.
 */
enum class FetcherApi {
    /**
     * [Google Scholar](https://scholar.google.com/)
     */
    GOOGLE_SCHOLAR,

    /**
     * [Semantic Scholar](https://www.semanticscholar.org/)
     */
    SEMANTIC_SCHOLAR,

    /**
     * [IEEE Xplore](https://ieeexplore.ieee.org/)
     */
    IEEE_XPLORE,
}
