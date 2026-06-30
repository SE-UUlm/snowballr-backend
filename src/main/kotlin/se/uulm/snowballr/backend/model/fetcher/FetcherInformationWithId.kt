package se.uulm.snowballr.backend.model.fetcher

import snowballr.Fetcher

data class FetcherInformationWithId(
    val id: String,
    val information: FetcherInformation,
)

fun FetcherInformationWithId.toGrpc(): Fetcher.FetcherInformation = information.toGrpc(id)
