package se.uulm.snowballr.backend.model.dto

import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass

/**
 * Represents a relationship between a project paper and its associated paper.
 *
 * This data class combines information from both a `ProjectPaper` and a `Paper`.
 * It is useful for scenarios where details of the `ProjectPaper` linked to a specific
 * `Paper` need to be accessed together.
 *
 * @property projectPaper The `ProjectPaper` instance containing project-specific data.
 * @property paper The `Paper` instance providing detailed information about the paper itself.
 */
data class ProjectPaperWithPaper(
    val projectPaper: ProjectPaper,
    val paper: Paper,
)

/**
 * Converts a `ProjectPaperWithPaper` instance into a gRPC `ProjectOuterClass.Project.Paper` object.
 *
 * @param authors A list of authors represented as `PaperOuterClass.Author`, associated with the paper.
 * @param backwardReferencedIds A list of strings representing the IDs of papers referenced by the current paper.
 * @param reviews A list of reviews represented as `ReviewOuterClass.Review`, associated with the paper.
 * @return A `ProjectOuterClass.Project.Paper` object constructed with data from the `ProjectPaperWithPaper` instance and the provided authors, backward references, and reviews.
 */
fun ProjectPaperWithPaper.toGrpcProjectPaper(
    authors: List<PaperOuterClass.Author>,
    backwardReferencedIds: List<String>,
    reviews: List<ReviewOuterClass.Review>,
): ProjectOuterClass.Project.Paper = ProjectOuterClass.Project.Paper
    .newBuilder()
    .setId(projectPaper.id.toString())
    .setPaper(paper.toGrpcPaper(authors, backwardReferencedIds))
    .setStage(projectPaper.stage)
    .setDecision(projectPaper.decision)
    .addAllReviews(reviews)
    .setLocalId(projectPaper.localPaperId.toString())
    .build()

/**
 * Converts a list of [ProjectPaperWithPaper] objects into a gRPC list of project papers.
 *
 * @return A [ProjectOuterClass.Project.Paper.List] containing the gRPC representation of the project papers.
 */
fun List<ProjectPaperWithPaper>.toGrpcProjectPapers(
    paperAuthorsMap: Map<Paper, List<PaperOuterClass.Author>>,
    paperBackwardReferencesMap: Map<Paper, List<String>>,
    paperReviewsMap: Map<ProjectPaper, List<ReviewOuterClass.Review>>,
): ProjectOuterClass.Project.Paper.List = ProjectOuterClass.Project.Paper.List
    .newBuilder()
    .addAllProjectPapers(
        this.map { projectPaper ->
            val authors = paperAuthorsMap[projectPaper.paper].orEmpty()
            val backwardRefs = paperBackwardReferencesMap[projectPaper.paper].orEmpty()
            val reviews = paperReviewsMap[projectPaper.projectPaper].orEmpty()

            projectPaper.toGrpcProjectPaper(
                authors = authors,
                backwardReferencedIds = backwardRefs,
                reviews = reviews,
            )
        },
    )
    .build()
