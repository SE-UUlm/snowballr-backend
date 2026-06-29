package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snowballr.Base

@Serializable
data class Link(
    @SerialName("label")
    val label: String,
    @SerialName("url")
    val url: String,
)

fun Link.toGrpc(): Base.Link = Base.Link.newBuilder()
    .setLabel(label)
    .setUrl(url)
    .build()
