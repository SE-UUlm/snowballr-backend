package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    @SerialName("label")
    val label: String,
    @SerialName("url")
    val url: String,
)
