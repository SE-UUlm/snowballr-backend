package se.uulm.snowballr.backend.model.incoming.review

import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import java.util.UUID

data class CreateReviewRequest(
    val projectId: UUID,
    val paperId: UUID,
    val decision: ReviewDecision,
    val selectedCriteriaIds: List<UUID>,
)
