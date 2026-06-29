package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snowballr.Fetcher

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

fun FetcherInformation.toGrpc(fetcherId: String): Fetcher.FetcherInformation = Fetcher.FetcherInformation.newBuilder()
    .setId(fetcherId)
    .setName(name)
    .setDescription(description)
    .addAllLinks(links.map { it.toGrpc() })
    .putAllOptionsSchema(optionsSchema.mapValues { it.value.toGrpc() })
    .build()
