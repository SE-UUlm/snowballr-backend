package se.uulm.snowballr.backend.model.dto

import snowballr.PaperOuterClass.Author as GrpcAuthor
import snowballr.ReviewOuterClass.Review as GrpcReview
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

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
 * @param authors A list of authors represented as [GrpcAuthor], associated with the [Paper].
 * @param backwardReferencedIds A list of strings representing the IDs of [Paper]s referenced by the current [Paper].
 * @param reviews A list of reviews represented as [GrpcReview], associated with the [Paper].
 * @return A [GrpcProjectPaper] object constructed with data from the [ProjectPaperWithPaper] instance and the provided [Author]s, backward references, and [Review]s.
 */
fun ProjectPaperWithPaper.toGrpcProjectPaper(
    authors: List<GrpcAuthor>,
    backwardReferencedIds: List<String>,
    reviews: List<GrpcReview>,
): GrpcProjectPaper = GrpcProjectPaper
    .newBuilder()
    .setId(projectPaper.id.toString())
    .setPaper(paper.toGrpcPaper(authors, backwardReferencedIds))
    .setStage(projectPaper.stage)
    .setDecision(projectPaper.decision)
    .addAllReviews(reviews)
    .setLocalId(projectPaper.localPaperId.toString())
    .build()

/**
 * Converts a list of [ProjectPaperWithPaper] objects into a gRPC list of [ProjectPaper]s.
 *
 * @return A [GrpcProjectPaper.List] containing the gRPC representation of the [ProjectPaper]s.
 */
fun List<ProjectPaperWithPaper>.toGrpcProjectPapers(
    paperAuthorsMap: Map<Paper, List<GrpcAuthor>>,
    paperBackwardReferencesMap: Map<Paper, List<String>>,
    paperReviewsMap: Map<ProjectPaper, List<GrpcReview>>,
): GrpcProjectPaper.List = GrpcProjectPaper.List
    .newBuilder()
    .addAllProjectPapers(
        this.map { projectPaper ->
            val authors = paperAuthorsMap[projectPaper.paper].orEmpty()
            val backwardRefs = paperBackwardReferencesMap[projectPaper.paper].orEmpty()
            val reviews = paperReviewsMap[projectPaper.projectPaper].orEmpty()

            projectPaper.toGrpcProjectPaper(
                authors,
                backwardRefs,
                reviews,
            )
        },
    )
    .build()
