package se.uulm.snowballr.backend.model.outgoing.paper

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toGrpc
import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import snowballr.PaperOuterClass
import java.util.UUID

data class PaperResponse(
    val id: UUID,
    val externalId: String?,
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
            externalId = paper.externalId,
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

fun PaperResponse.toGrpc(): PaperOuterClass.Paper = PaperOuterClass.Paper.newBuilder()
    .setId(id.toString())
    .setExternalId(externalId.orEmpty())
    .setTitle(title)
    .setAbstrakt(abstract)
    .setYear(year)
    .setPublisher(publisher)
    .setPublicationName(publicationName)
    .setPublicationType(publicationType)
    .addAllAuthors(authors.toGrpc())
    .putAllFetcherMetadata(fetcherMetadata)
    .addAllBackwardReferencedIds(backwardReferencedIds.map { it.toString() })
    .build()

fun List<PaperResponse>.toGrpc(): PaperOuterClass.Paper.List = PaperOuterClass.Paper.List.newBuilder()
    .addAllPapers(this.map { it.toGrpc() })
    .build()
