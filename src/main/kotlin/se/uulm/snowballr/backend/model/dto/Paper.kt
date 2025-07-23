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
fun Paper.toGrpcPaper(authors: List<PaperOuterClass.Author>, backwardReferencedIds: List<Int>): PaperOuterClass.Paper =
    PaperOuterClass.Paper
        .newBuilder()
        .setId(this.id.toString())
        .setTitle(this.title)
        .setExternalId(this.externalId)
        .setAbstrakt(this.abstract)
        .setPublisher(this.publisher)
        .setPublicationType(this.publicationType)
        .setPublicationName(this.publicationName)
        .setHasPdf(this.pdfId != null)
        .addAllAuthors(authors)
        .addAllBackwardReferencedIds(backwardReferencedIds.map { it.toString() })
        .build()
