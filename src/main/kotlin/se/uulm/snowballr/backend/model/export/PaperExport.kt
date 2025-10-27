package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snowballr.ProjectOuterClass

@Serializable
data class PaperExport(
    @SerialName("title")
    val title: String,
    @SerialName("external_id")
    val externalId: String,
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
    val finalDecision: ProjectOuterClass.PaperDecision,
)
