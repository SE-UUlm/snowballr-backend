package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snowballr.Fetcher

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

fun FetcherOptionsSchema.toGrpc(): Fetcher.FetcherOptionSchema = Fetcher.FetcherOptionSchema.newBuilder()
    .setName(name)
    .setDescription(description)
    .setRequired(isRequired)
    .setIsSecret(isSecret)
    .apply { if (defaultValue != null) setDefaultValue(defaultValue) }
    .build()
