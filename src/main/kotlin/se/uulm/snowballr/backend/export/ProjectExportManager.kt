package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.export.CriterionExport
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import se.uulm.snowballr.backend.model.export.PaperExport
import se.uulm.snowballr.backend.model.export.PaperReviewExport
import se.uulm.snowballr.backend.model.export.ProjectExport
import se.uulm.snowballr.backend.model.export.ProjectMemberExport
import se.uulm.snowballr.backend.model.export.ProjectStageExport
import snowballr.ProjectOuterClass.PaperDecision
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Manager responsible for exporting projects in various formats.
 */
@Suppress("TooManyFunctions")
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
        projectCriteria: List<Criterion>,
    ): FileExport {
        val exporter = exporters[format] ?: throw IllegalArgumentException("Unsupported export format: $format")

        val projectMembersExport = projectMembers.mapIndexed { id, member -> member.toProjectMemberExport(id) }
        val stageToPapersExport = projectPapers.toPapersExportByStage(projectMembers)
        val stages = stageToPapersExport.toProjectStagesExport()
        val criteria = projectCriteria.toCriteriaExport()

        val projectExport = ProjectExport(project.name, projectMembersExport, stages, criteria)
        val data = exporter.export(projectExport)
        val filename = createFilename(project.name, exporter.getExtension())
        return FileExport(data, filename)
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
            reviews = reviewsWithSelectedCriteria.map { it.toPaperReviewExport(projectMembers) },
            finalDecision = projectPaper.decision,
        )

    private fun ReviewWithSelectedCriteriaIds.toPaperReviewExport(
        projectMembers: List<ProjectMemberWithUser>,
    ): PaperReviewExport {
        val projectMemberId = projectMembers.indexOfFirst { it.user.id == review.userId }
        val reviewerId = if (projectMemberId == -1) "unknown" else projectMemberId.toString()
        return PaperReviewExport(reviewerId, review.decision, selectedCriteriaIds.map { "$it" })
    }

    private fun Map<Long, List<PaperExport>>.toProjectStagesExport(): List<ProjectStageExport> =
        this.map { (stage, papers) ->
            ProjectStageExport(id = "$stage", papers = papers)
        }

    private fun List<Criterion>.toCriteriaExport(): List<CriterionExport> = this.mapIndexed { index, criterion ->
        CriterionExport(
            id = "$index",
            tag = criterion.tag,
            name = criterion.name,
            description = criterion.description,
            category = criterion.category,
        )
    }

    private fun createFilename(projectName: String, fileExtension: String): String {
        val timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        return sanitizeFilename("$projectName-$timestamp.$fileExtension")
    }

    private fun sanitizeFilename(input: String): String {
        // Define characters not allowed in filenames (Windows + UNIX safe set)
        val illegalChars = """[\\/:*?"<>|\p{Cntrl}]""".toRegex()

        var sanitized = input.replace(illegalChars, "_")

        // Trim spaces and dots (Windows doesn't like names ending with . or space)
        sanitized = sanitized.trim().trimEnd('.')

        // Fallback for empty filenames
        if (sanitized.isEmpty()) sanitized = "unnamed"

        return sanitized
    }
}
