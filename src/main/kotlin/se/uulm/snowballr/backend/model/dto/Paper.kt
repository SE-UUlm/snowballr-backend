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
): PaperOuterClass.Paper = PaperOuterClass.Paper
    .newBuilder()
    .setId(id.toString())
    .setTitle(title)
    .setExternalId(externalId)
    .setAbstrakt(abstract)
    .setPublisher(publisher)
    .setPublicationType(publicationType)
    .setPublicationName(publicationName)
    .setHasPdf(pdfId != null)
    .addAllAuthors(authors)
    .addAllBackwardReferencedIds(backwardReferencedIds)
    .build()

/**
 * Converts a list of [Paper] objects into a gRPC list of papers.
 *
 * @return A [PaperOuterClass.Paper.List] containing the gRPC representation of the papers.
 */
fun List<Paper>.toGrpcPapers(
    paperAuthorsMap: Map<Paper, List<PaperOuterClass.Author>>,
    paperBackwardReferencesMap: Map<Paper, List<String>>,
): PaperOuterClass.Paper.List = PaperOuterClass.Paper.List
    .newBuilder()
    .addAllPapers(
        this.map { paper ->
            val authors = paperAuthorsMap[paper].orEmpty()
            val backwardRefs = paperBackwardReferencesMap[paper].orEmpty()
            paper.toGrpcPaper(
                authors = authors,
                backwardReferencedIds = backwardRefs,
            )
        },
    )
    .build()
