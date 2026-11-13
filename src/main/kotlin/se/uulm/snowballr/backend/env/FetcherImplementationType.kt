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
    ;

    companion object {
        /**
         * Parses a string to a [FetcherImplementationType]. The matching is case-insensitive.
         *
         * @param value The string value to parse.
         * @return The corresponding [FetcherImplementationType], or null if the [value] doesn't match any of them.
         */
        fun fromString(value: String): FetcherImplementationType? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
