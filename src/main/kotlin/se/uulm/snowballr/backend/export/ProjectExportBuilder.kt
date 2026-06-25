package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.export.CriterionExport
import se.uulm.snowballr.backend.model.export.PaperExport
import se.uulm.snowballr.backend.model.export.PaperReviewExport
import se.uulm.snowballr.backend.model.export.ProjectExport
import se.uulm.snowballr.backend.model.export.ProjectMemberExport
import se.uulm.snowballr.backend.model.export.ProjectStageExport

/**
 * Builder class responsible for constructing a [ProjectExport] from project data.
 *
 * @param project The project to be exported.
 * @param projectMembers The members of the project along with their user details.
 * @param projectPapers The papers associated with the project, including reviews and decisions.
 * @param projectCriteria The criteria defined for the project.
 */
class ProjectExportBuilder(
    private val project: Project,
    private val projectMembers: List<ProjectMemberWithUser>,
    private val projectPapers: List<ProjectPaperFull>,
    private val projectCriteria: List<Criterion>,
) {
    fun buildExport(): ProjectExport {
        val projectMembersExport = projectMembers.mapIndexed { id, member -> member.toProjectMemberExport(id) }
        val stageToPapersExport = projectPapers.toPapersExportByStage()
        val stages = stageToPapersExport.toProjectStagesExport()
        val criteria = projectCriteria.toCriteriaExport()

        return ProjectExport(project.name, projectMembersExport, stages, criteria, project.createdAt.toString())
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
            externalIds = externalIds,
            abstract = abstract,
            year = year,
            publisher = publisher,
            publicationType = publicationType,
            publicationName = publicationName,
            authors = authors.map { "${it.firstName} ${it.lastName}" },
            reviews = reviews,
            finalDecision = finalDecision,
            createdAt = createdAt.toString(),
            modifiedAt = modifiedAt?.toString().orEmpty(),
        )

    private fun List<ProjectPaperFull>.toPapersExportByStage(): Map<Int, List<PaperExport>> =
        this.groupBy { it.projectPaper.stage }.mapValues { entry ->
            entry.value.map { it.toPaperExport() }
        }

    private fun ProjectPaperFull.toPaperExport(): PaperExport = paper.toPaperExport(
        reviews = reviewsWithSelectedCriteria.map { it.toPaperReviewExport() },
        finalDecision = projectPaper.decision,
    )

    private fun ReviewWithSelectedCriteriaIds.toPaperReviewExport(): PaperReviewExport {
        val projectMemberId = projectMembers.indexOfFirst { it.user.id == review.userId }
        val reviewerId = if (projectMemberId == -1) "unknown" else projectMemberId.toString()
        val selectedCriteriaIdsAsStrings = selectedCriteriaIds.map { criterionId ->
            val criterionIndex = projectCriteria.indexOfFirst { it.id == criterionId }
            if (criterionIndex == -1) "unknown" else "$criterionIndex"
        }
        return PaperReviewExport(reviewerId, review.decision, selectedCriteriaIdsAsStrings)
    }

    private fun Map<Int, List<PaperExport>>.toProjectStagesExport(): List<ProjectStageExport> =
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
}
