package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.PaperExport
import se.uulm.snowballr.backend.model.export.PaperReviewExport
import se.uulm.snowballr.backend.model.export.ProjectExport
import se.uulm.snowballr.backend.model.export.ProjectMemberExport
import se.uulm.snowballr.backend.model.export.ProjectStageExport
import snowballr.ProjectOuterClass.PaperDecision

/**
 * Manager responsible for exporting projects in various formats.
 */
object ProjectExportManager {
    private val exporters = mapOf<ExportFormat, IExporter>(
        ExportFormat.JSON to JsonExporter(),
    )

    fun getSupportedFormats(): Set<ExportFormat> = exporters.keys

    fun exportProject(
        format: ExportFormat,
        project: Project,
        projectMembers: List<ProjectMemberWithUser>,
        projectPapers: List<ProjectPaperFull>,
    ): ByteArray {
        val exporter = exporters[format] ?: throw IllegalArgumentException("Unsupported export format: $format")

        val projectMembersExport = projectMembers.mapIndexed { id, member -> member.toProjectMemberExport(id) }
        val stageToPapersExport = projectPapers.toPapersExportByStage(projectMembers)
        val stages = stageToPapersExport.toProjectStagesExport()

        val projectExport = ProjectExport(name = project.name, members = projectMembersExport, stages = stages)

        return exporter.export(projectExport)
    }

    private fun ProjectMemberWithUser.toProjectMemberExport(id: Int): ProjectMemberExport = ProjectMemberExport(
        id = "$id",
        firstName = user.firstName,
        lastName = user.lastName,
        email = user.email,
        role = projectMember.role,
    )

    private fun Paper.toPaperExport(reviews: List<PaperReviewExport>, finalDecision: PaperDecision): PaperExport =
        PaperExport(
            title = title,
            externalId = externalId.orEmpty(),
            abstract = abstract,
            year = year,
            publisher = publisher,
            publicationType = publicationType,
            publicationName = publicationName,
            authors = authors.map { "${it.firstName} ${it.lastName}" },
            reviews = reviews,
            finalDecision = finalDecision,
        )

    private fun List<ProjectPaperFull>.toPapersExportByStage(
        projectMembers: List<ProjectMemberWithUser>,
    ): Map<Long, List<PaperExport>> = this.groupBy { it.projectPaper.stage }.mapValues { entry ->
        entry.value.map { it.toPaperExport(projectMembers) }
    }

    private fun ProjectPaperFull.toPaperExport(projectMembers: List<ProjectMemberWithUser>): PaperExport =
        paper.toPaperExport(
            reviews = reviews.map { it.toPaperReviewExport(projectMembers) },
            finalDecision = projectPaper.decision,
        )

    private fun Review.toPaperReviewExport(projectMembers: List<ProjectMemberWithUser>): PaperReviewExport {
        val projectMemberId = projectMembers.indexOfFirst { it.user.id == userId }
        val reviewerId = if (projectMemberId == -1) "unknown" else projectMemberId.toString()
        return PaperReviewExport(reviewerId, decision)
    }

    private fun Map<Long, List<PaperExport>>.toProjectStagesExport(): List<ProjectStageExport> =
        this.map { (stage, papers) ->
            ProjectStageExport(id = "$stage", papers = papers)
        }
}
