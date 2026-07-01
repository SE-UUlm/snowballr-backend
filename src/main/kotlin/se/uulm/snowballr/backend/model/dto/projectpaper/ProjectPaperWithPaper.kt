package se.uulm.snowballr.backend.model.dto.projectpaper

import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import java.util.UUID

/**
 * Represents a relationship between a [ProjectPaper] and its associated [Paper].
 *
 * This data class combines information from both a [ProjectPaper] and a [Paper].
 * It is useful for scenarios where details of the [ProjectPaper] linked to a specific
 * [Paper] need to be accessed together.
 *
 * @property projectPaper The [ProjectPaper] instance containing project-specific data.
 * @property paper The [Paper] instance providing detailed information about the paper itself.
 */
data class ProjectPaperWithPaper(
    val projectPaper: ProjectPaper,
    val paper: Paper,
)

fun List<ProjectPaperWithPaper>.toProjectPaperResponses(
    paperBackwardReferencesMap: Map<Paper, List<UUID>>,
    paperReviewsMap: Map<ProjectPaper, List<ReviewResponse>>,
): List<ProjectPaperResponse> = this.map {
    ProjectPaperResponse.fromProjectPaperWithPaper(
        paper = it,
        backwardReferencedIds = paperBackwardReferencesMap[it.paper].orEmpty(),
        reviews = paperReviewsMap[it.projectPaper].orEmpty(),
    )
}

/**
 * Converts a [ProjectPaperWithPaper] instance into a [ProjectPaperFull] object.
 *
 * @param reviewsWithSelectedCriteriaIds A list of [ReviewWithSelectedCriteriaIds] associated with the project paper.
 * @return A [ProjectPaperFull] object containing the project paper, paper, and associated reviews with selected
 * criteria.
 */
fun ProjectPaperWithPaper.toProjectPaperFull(
    reviewsWithSelectedCriteriaIds: List<ReviewWithSelectedCriteriaIds>,
): ProjectPaperFull = ProjectPaperFull(
    projectPaper = this.projectPaper,
    paper = this.paper,
    reviewsWithSelectedCriteria = reviewsWithSelectedCriteriaIds,
)

/**
 * Checks if the [ProjectPaper] within this [ProjectPaperWithPaper] has no final decision.
 *
 * @see ProjectPaper.hasNoFinalDecision
 */
fun ProjectPaperWithPaper.hasNoFinalDecision() = this.projectPaper.hasNoFinalDecision()
