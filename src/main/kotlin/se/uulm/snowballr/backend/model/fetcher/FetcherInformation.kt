package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetcherInformation(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("links")
    val links: List<Link>,
    @SerialName("options_schema")
    val optionsSchema: Map<String, FetcherOptionsSchema>,
)
