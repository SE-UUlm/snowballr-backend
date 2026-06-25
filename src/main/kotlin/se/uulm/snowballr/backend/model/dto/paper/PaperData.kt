package se.uulm.snowballr.backend.model.dto.paper

import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata

interface PaperData {
    val title: String
    val externalIds: List<ExternalId>
    val abstract: String
    val year: Int
    val publisher: String
    val publicationType: String
    val publicationName: String
    val authors: List<Author>
    val fetcherMetadata: FetcherMetadata
}
