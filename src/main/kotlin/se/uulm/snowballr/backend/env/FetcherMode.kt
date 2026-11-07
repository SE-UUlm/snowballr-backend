package se.uulm.snowballr.backend.env

/**
 * Determines which fetcher manager implementation is used to fetch papers.
 *
 * - **PYTHON**: Uses the Python plugin fetchers.
 * - **DUMMY**: Uses hard-coded dummy fetchers that return generated papers. Not allowed in production.
 */
enum class FetcherMode {
    PYTHON,
    DUMMY,
    ;

    companion object {
        /**
         * Parses a string to a [FetcherMode]. The matching is case-insensitive.
         *
         * @param value The string value to parse.
         * @return The corresponding [FetcherMode], or null if the [value] doesn't match a [FetcherMode].
         */
        fun fromString(value: String): FetcherMode? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
