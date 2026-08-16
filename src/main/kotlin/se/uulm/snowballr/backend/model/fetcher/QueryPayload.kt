package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for fetcher query actions.
 *
 * @property searchQuery Raw text query sent to the fetcher.
 * @property options Fetcher-specific option key/value pairs.
 */
@Serializable
data class QueryPayload(
    @SerialName("search_query")
    val searchQuery: String,
    val options: Map<String, String>,
)
