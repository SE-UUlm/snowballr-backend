package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.table.PaperTable
import snowballr.paper
import java.time.OffsetDateTime
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

/**
 * DTO of [PaperTable].
 */
data class Paper(
    val id: UUID,
    val title: String,
    val externalId: String?,
    val abstract: String,
    val year: Int,
    val publisher: String,
    val publicationType: String,
    val publicationName: String,
    val pdfId: UUID?,
    val authors: List<Author>,
    val fetcherMetadata: FetcherMetadata,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
)

/**
 * Creates a [GrpcPaper] from this [Paper].
 */
fun Paper.toGrpcPaper(backwardReferencedIdsList: List<String>): GrpcPaper {
    val paper = this
    return paper {
        id = paper.id.toString()
        title = paper.title
        if (paper.externalId != null) externalId = paper.externalId
        abstrakt = paper.abstract
        year = paper.year
        publisher = paper.publisher
        publicationType = paper.publicationType
        publicationName = paper.publicationName
        hasPdf = paper.pdfId != null
        authors.addAll(paper.authors.toGrpcAuthors())
        backwardReferencedIds.addAll(backwardReferencedIdsList)
    }
}

/**
 * Converts a list of [GrpcPaper] objects into a [GrpcPaper.List].
 */
fun List<GrpcPaper>.toGrpcPapers(): GrpcPaper.List = GrpcPaper.List
    .newBuilder()
    .addAllPapers(this)
    .build()

/**
 * Creates a [FetcherPaper] from this [paper]
 */
fun Paper.toFetcherPaper(): FetcherPaper = FetcherPaper(
    title = title,
    externalId = externalId,
    abstract = abstract,
    year = year,
    publisher = publisher,
    publicationType = publicationType,
    publicationName = publicationName,
    authors = authors,
    metadata = fetcherMetadata,
)
