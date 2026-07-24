package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision

@Serializable
data class PaperExport(
    @SerialName("title")
    val title: String,
    @SerialName("external_ids")
    val externalIds: List<ExternalId>,
    @SerialName("abstract")
    val abstract: String,
    @SerialName("year")
    val year: Int,
    @SerialName("publisher")
    val publisher: String,
    @SerialName("publication_type")
    val publicationType: String,
    @SerialName("publication_name")
    val publicationName: String,
    @SerialName("authors")
    val authors: List<String>,
    @SerialName("reviews")
    val reviews: List<PaperReviewExport>,
    @SerialName("final_decision")
    val finalDecision: PaperDecision,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("modified_at")
    val modifiedAt: String,
)
