package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.PaperTable
import snowballr.PaperOuterClass
import snowballr.paper
import java.time.OffsetDateTime
import java.util.UUID

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
    val fetcherMetadata: Map<String, String>,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
)

/**
 * Creates a [PaperOuterClass.Paper] from this [Paper].
 */
fun Paper.toGrpcPaper(
    authorList: List<PaperOuterClass.Author>,
    backwardReferencedIdsList: List<String>,
): PaperOuterClass.Paper {
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
        authors.addAll(authorList)
        backwardReferencedIds.addAll(backwardReferencedIdsList)
    }
}

/**
 * Converts a list of [PaperOuterClass.Paper] objects into a [PaperOuterClass.Paper.List].
 */
fun List<PaperOuterClass.Paper>.toGrpcPapers(): PaperOuterClass.Paper.List = PaperOuterClass.Paper.List
    .newBuilder()
    .addAllPapers(this)
    .build()
