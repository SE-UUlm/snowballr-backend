package se.uulm.snowballr.backend.model.dto.review

import java.util.UUID

/**
 * DTO representing a review along with its selected criteria IDs.
 *
 * @property review The review information.
 * @property selectedCriteriaIds A list of selected criteria IDs associated with the review.
 */
data class ReviewWithSelectedCriteriaIds(
    val review: Review,
    val selectedCriteriaIds: List<UUID>,
)
