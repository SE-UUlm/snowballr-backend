package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snowballr.ReviewOuterClass

@Serializable
data class PaperReviewExport(
    @SerialName("reviewer_id")
    val reviewerId: String,
    @SerialName("decision")
    val decision: ReviewOuterClass.ReviewDecision,
    @SerialName("selected_criteria_ids")
    val selectedCriteriaIds: List<String>,
)
