package se.uulm.snowballr.backend.model.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.PaperData

/**
 * Represents a paper fetched from an external source. Also, a serializable implementation of [PaperData].
 *
 * @property title The title of the paper.
 * @property externalIds A list of external identifiers that are associated with the paper.
 * @property abstract The abstract or summary of the paper.
 * @property year The year the paper was published in.
 * @property publisher The name of the publisher of the paper.
 * @property publicationType The type of publication, such as "journal", "conference", etc. (unconstrained)
 * @property publicationName The name of the publication where the paper appeared.
 * @property authors A list of authors associated with the paper, represented as [Author] objects.
 * @property fetcherMetadata A map of metadata used by fetchers. For example, API-related unique IDs for later use.
 */
@Serializable
data class FetcherPaper(
    @SerialName("title")
    override val title: String,
    @SerialName("external_ids")
    override val externalIds: List<ExternalId>,
    @SerialName("abstract")
    override val abstract: String,
    @SerialName("year")
    override val year: Int,
    @SerialName("publisher")
    override val publisher: String,
    @SerialName("publication_type")
    override val publicationType: String,
    @SerialName("publication_name")
    override val publicationName: String,
    @SerialName("authors")
    override val authors: List<Author>,
    @SerialName("fetcher_metadata")
    override val fetcherMetadata: Map<String, String>,
) : PaperData
