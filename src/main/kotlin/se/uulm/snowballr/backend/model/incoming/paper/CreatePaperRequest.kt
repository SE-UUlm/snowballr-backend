package se.uulm.snowballr.backend.model.incoming.paper

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.PaperData
import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper

data class CreatePaperRequest(
    val title: String,
    val externalIds: List<ExternalId>,
    val abstract: String,
    val year: Int,
    val publisher: String,
    val publicationName: String,
    val publicationType: String,
    val authors: List<Author>,
    val fetcherMetadata: FetcherMetadata,
) {
    companion object {
        fun fromPaper(paper: Paper) = fromPaperData(paper)

        fun fromFetcherPaper(paper: FetcherPaper) = fromPaperData(paper)

        private fun fromPaperData(paper: PaperData) = CreatePaperRequest(
            title = paper.title,
            externalIds = paper.externalIds,
            abstract = paper.abstract,
            year = paper.year,
            publisher = paper.publisher,
            publicationName = paper.publicationName,
            publicationType = paper.publicationType,
            authors = paper.authors,
            fetcherMetadata = paper.fetcherMetadata,
        )
    }
}
