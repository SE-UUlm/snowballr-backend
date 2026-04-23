package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.json.Json

/**
 * Lists all supported fetcher actions and their command/payload mapping.
 * Payload builder overloads are overridden per action; unsupported combinations fail fast.
 *
 * @property command CLI action string passed as the first argument to the Python fetcher.
 */
enum class FetcherAction(
    val command: String,
) {
    /** Returns the fetcher's available option definitions. */
    OPTIONS("options") {
        override fun payload(): String = ""
    },

    /** Searches for papers using a text query and options. */
    QUERY("query") {
        override fun payload(searchQuery: String, options: Map<String, String>): String =
            Json.encodeToString(QueryPayload(searchQuery = searchQuery, options = options))
    },

    /** Fetches papers that cite the provided source paper. */
    FORWARDS("forwards") {
        override fun payload(paper: FetcherPaper, options: Map<String, String>): String =
            Json.encodeToString(ReferencePayload(paper = paper, options = options))
    },

    /** Fetches papers cited by the provided source paper. */
    BACKWARDS("backwards") {
        override fun payload(paper: FetcherPaper, options: Map<String, String>): String =
            Json.encodeToString(ReferencePayload(paper = paper, options = options))
    },
    ;

    /** Builds a payload for actions that do not require additional input. */
    open fun payload(): String = unsupportedPayload("no payload")

    /** Builds a payload for query actions. */
    open fun payload(searchQuery: String, options: Map<String, String>): String = unsupportedPayload("query payload")

    /** Builds a payload for reference actions. */
    open fun payload(paper: FetcherPaper, options: Map<String, String>): String =
        unsupportedPayload("reference payload")

    private fun unsupportedPayload(payloadType: String): Nothing {
        error("Action '$command' does not support $payloadType.")
    }
}
