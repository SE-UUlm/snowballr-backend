package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetcherOptionsSchema(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("required")
    val isRequired: Boolean,
    @SerialName("is_secret")
    val isSecret: Boolean,
    @SerialName("default_value")
    val defaultValue: String?,
)
