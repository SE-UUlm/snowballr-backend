package se.uulm.snowballr.backend.model.dto

import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
import java.util.UUID

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
 * Converts a [ProjectPaperWithPaper] instance to a gRPC-compatible representation of
 * [ProjectOuterClass.Project.Paper].
 *
 * @param authors The list of gRPC authors to associate with the paper.
 * @param backwardReferencedIds The list of backward-referenced paper IDs.
 * @return A gRPC-compatible [ProjectOuterClass.Project.Paper] object containing the converted data.
 */
fun ProjectPaperWithPaper.toGrpcProjectPaper(
    authors: List<PaperOuterClass.Author>,
    backwardReferencedIds: List<String>,
): ProjectOuterClass.Project.Paper = ProjectOuterClass.Project.Paper
    .newBuilder()
    .setId(projectPaper.id.toString())
    .setPaper(paper.toGrpcPaper(authors, backwardReferencedIds))
    .setStage(projectPaper.stage)
    .setDecision(projectPaper.decision)
    .setLocalId(projectPaper.localPaperId.toString())
    .build()

/**
 * Converts a list of [ProjectPaperWithPaper] objects into a gRPC list of project papers.
 *
 * @return A [ProjectOuterClass.Project.Paper.List] containing the gRPC representation of the project papers.
 */
fun List<ProjectPaperWithPaper>.toGrpcProjectPapers(
    paperAuthorsMap: Map<Paper, List<Author>>,
    paperBackwardReferencesMap: Map<Paper, List<UUID>>,
): ProjectOuterClass.Project.Paper.List = ProjectOuterClass.Project.Paper.List
    .newBuilder()
    .addAllProjectPapers(
        this.map { projectPaper ->
            val authors = paperAuthorsMap[projectPaper.paper]
                ?.map { it.toGrpcAuthor() }.orEmpty()

            val backwardRefs = paperBackwardReferencesMap[projectPaper.paper]
                ?.map { it.toString() }.orEmpty()

            projectPaper.toGrpcProjectPaper(
                authors = authors,
                backwardReferencedIds = backwardRefs,
            )
        },
    )
    .build()
