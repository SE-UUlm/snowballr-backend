package se.uulm.snowballr.backend.model.fetcher

/**
 * Map of a fetcher name to its options map.
 *
 * The option map maps the key to its value.
 */
typealias FetcherMap = Map<String, FetcherOptions>

/**
 * Options of a single fetcher.
 */
typealias FetcherOptions = Map<String, String>
