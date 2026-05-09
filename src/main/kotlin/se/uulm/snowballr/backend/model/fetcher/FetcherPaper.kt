package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcAuthors
import snowballr.paper
import java.time.OffsetDateTime
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

/**
 * Represents a paper fetched from an external source.
 *
 * @property title The title of the paper.
 * @property externalId A unique external identifier for the paper, such as a DOI or other unique ID.
 * @property abstract The abstract or summary of the paper.
 * @property year The year the paper was published in.
 * @property publisher The name of the publisher of the paper.
 * @property publicationType The type of publication, such as "journal", "conference", etc. (unconstrained)
 * @property publicationName The name of the publication where the paper appeared.
 * @property authors A list of authors associated with the paper, represented as [Author] objects.
 * @property metadata A map of metadata used by fetchers. For example, API-related unique IDs for later use.
 */
@Serializable
data class FetcherPaper(
    val title: String,
    @SerialName("external_id")
    val externalId: String? = null,
    val abstract: String,
    val year: Int,
    val publisher: String,
    @SerialName("publication_type")
    val publicationType: String,
    @SerialName("publication_name")
    val publicationName: String,
    val authors: List<Author> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Converts a [FetcherPaper] into a [Paper] object to be inserted into the database.
 *
 * @receiver The FetcherPaper instance to be converted.
 * @return The resulting [Paper] dto.
 */
fun FetcherPaper.toPaper(): Paper = Paper(
    id = UUID.randomUUID(),
    title = this.title,
    externalId = this.externalId,
    abstract = this.abstract,
    year = this.year,
    publisher = this.publisher,
    publicationType = this.publicationType,
    publicationName = this.publicationName,
    pdfId = null,
    fetcherMetadata = this.metadata,
    authors = this.authors,
    createdAt = OffsetDateTime.now(),
    modifiedAt = null,
    modifiedBy = null,
)

/**
 * Creates a [GrpcPaper] request from this [FetcherPaper].
 */
fun FetcherPaper.toGrpcPaperRequest(): GrpcPaper {
    val paper = this
    return paper {
        title = paper.title
        if (paper.externalId != null) externalId = paper.externalId
        abstrakt = paper.abstract
        year = paper.year
        publisher = paper.publisher
        publicationType = paper.publicationType
        publicationName = paper.publicationName
        authors.addAll(paper.authors.toGrpcAuthors())
    }
}
