package se.uulm.snowballr.backend.model.dto.paper

import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.table.PaperTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [PaperTable].
 */
data class Paper(
    val id: UUID,
    override val title: String,
    override val externalId: String?,
    override val externalIds: List<ExternalId>,
    override val abstract: String,
    override val year: Int,
    override val publisher: String,
    override val publicationType: String,
    override val publicationName: String,
    val pdfId: UUID?,
    override val authors: List<Author>,
    override val fetcherMetadata: FetcherMetadata,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
) : PaperData

/**
 * Creates a [FetcherPaper] from this [Paper].
 */
fun Paper.toFetcherPaper(): FetcherPaper = FetcherPaper(
    title = title,
    externalId = externalId,
    externalIds = externalIds,
    abstract = abstract,
    year = year,
    publisher = publisher,
    publicationType = publicationType,
    publicationName = publicationName,
    authors = authors,
    fetcherMetadata = fetcherMetadata,
)
