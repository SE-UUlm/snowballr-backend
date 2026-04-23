package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.Serializable

/**
 * Request payload for fetcher reference actions.
 *
 * @property paper Source paper for which references are requested.
 * @property options Fetcher-specific option key/value pairs.
 */
@Serializable
data class ReferencePayload(
    val paper: FetcherPaper,
    val options: Map<String, String>,
)
