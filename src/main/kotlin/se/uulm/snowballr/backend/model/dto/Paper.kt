package se.uulm.snowballr.backend.model.dto

import kotlinx.datetime.Instant
import se.uulm.snowballr.backend.table.PaperTable
import snowballr.PaperOuterClass
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
    val publishedAt: Instant?,
    val publisher: String?,
    val publicationType: String?,
    val publicationName: String?,
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
    authors: List<PaperOuterClass.Author>,
    backwardReferencedIds: List<String>,
): PaperOuterClass.Paper {
    val paper = this
    return with(PaperOuterClass.Paper.newBuilder()) {
        setId(paper.id.toString())
        setTitle(paper.title)
        paper.externalId?.let { setExternalId(it) }
        setAbstrakt(paper.abstract)
        paper.publisher?.let { setPublisher(it) }
        paper.publicationType?.let { setPublicationType(it) }
        paper.publicationName?.let { setPublicationName(it) }
        setHasPdf(paper.pdfId != null)
        addAllAuthors(authors)
        addAllBackwardReferencedIds(backwardReferencedIds)
        build()
    }
}
