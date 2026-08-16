package se.uulm.snowballr.backend.model.outgoing.paper

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import java.util.UUID

data class PaperResponse(
    val id: UUID,
    val externalIds: List<ExternalId>,
    val title: String,
    val abstract: String,
    val year: Int,
    val publisher: String,
    val publicationName: String,
    val publicationType: String,
    val authors: List<Author>,
    val fetcherMetadata: FetcherMetadata,
    val backwardReferencedIds: List<UUID>,
) {
    companion object {
        fun fromPaper(paper: Paper, backwardReferencedIds: List<UUID>) = PaperResponse(
            id = paper.id,
            externalIds = paper.externalIds,
            title = paper.title,
            abstract = paper.abstract,
            year = paper.year,
            publisher = paper.publisher,
            publicationName = paper.publicationName,
            publicationType = paper.publicationType,
            authors = paper.authors,
            fetcherMetadata = paper.fetcherMetadata,
            backwardReferencedIds = backwardReferencedIds,
        )
    }
}
