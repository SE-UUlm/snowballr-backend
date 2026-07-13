package se.uulm.snowballr.backend.model.dto.projectpaper

import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toGrpcPaper
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

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

/**
 * Converts a [ProjectPaperWithPaper] instance into a gRPC [GrpcProjectPaper] object.
 *
 * @param backwardReferencedIds A list of strings representing the IDs of [Paper]s referenced by the current [Paper].
 * @param reviews A list of reviews represented as [GrpcReview], associated with the [Paper].
 * @return A [GrpcProjectPaper] object constructed with data from the [ProjectPaperWithPaper] instance and the provided
 * backward references and [Review]s.
 */
fun ProjectPaperWithPaper.toGrpcProjectPaper(
    backwardReferencedIds: List<String>,
    reviews: List<GrpcReview>,
): GrpcProjectPaper = GrpcProjectPaper
    .newBuilder()
    .setId(projectPaper.id.toString())
    .setPaper(paper.toGrpcPaper(backwardReferencedIds))
    .setStage(projectPaper.stage.toLong())
    .setDecision(projectPaper.decision.toGrpc())
    .addAllReviews(reviews)
    .setLocalId(projectPaper.localPaperId.toString())
    .build()

/**
 * Converts a list of [ProjectPaperWithPaper] objects into a gRPC list of [ProjectPaper]s.
 *
 * @return A [GrpcProjectPaper.List] containing the gRPC representation of the [ProjectPaper]s.
 */
fun List<ProjectPaperWithPaper>.toGrpcProjectPapers(
    paperBackwardReferencesMap: Map<Paper, List<String>>,
    paperReviewsMap: Map<ProjectPaper, List<GrpcReview>>,
): GrpcProjectPaper.List = GrpcProjectPaper.List
    .newBuilder()
    .addAllProjectPapers(
        this.map { projectPaper ->
            val backwardRefs = paperBackwardReferencesMap[projectPaper.paper].orEmpty()
            val reviews = paperReviewsMap[projectPaper.projectPaper].orEmpty()

            projectPaper.toGrpcProjectPaper(backwardRefs, reviews)
        },
    )
    .build()

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
