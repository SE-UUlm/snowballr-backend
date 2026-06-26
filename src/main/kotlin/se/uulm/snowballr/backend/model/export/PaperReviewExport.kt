package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision

@Serializable
data class PaperReviewExport(
    @SerialName("reviewer_id")
    val reviewerId: String,
    @SerialName("decision")
    val decision: ReviewDecision,
    @SerialName("selected_criteria_ids")
    val selectedCriteriaIds: List<String>,
)
