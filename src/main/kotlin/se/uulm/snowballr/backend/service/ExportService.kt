package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.projectpaper.toProjectPaperFull
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import java.util.UUID

interface IExportService {
    /**
     * Service implementation of [SnowballRService.getAvailableExportFormats].
     */
    suspend fun getAvailableExportFormats(): Set<ExportFormat>

    /**
     * Service implementation of [SnowballRService.exportProject].
     */
    suspend fun exportProject(projectId: UUID, format: ExportFormat): FileExport
}

/**
 * Service responsible for exporting project data in various formats.
 *
 * This class provides functionalities to retrieve available export formats and handle the
 * process of exporting a project along with its members, papers, and reviews. The exported data
 * can be formatted in supported output types, e.g., JSON.
 *
 * @param projectRepo Repository interface to manage operations related to project data.
 * @param projectMemberRepo Repository interface to manage operations related to project members.
 * @param projectPaperRepo Repository interface to manage operations related to project papers.
 * @param reviewRepo Repository interface to manage operations related to reviews of project papers.
 * @param criterionRepo Repository interface to manage operations related to project criteria.
 * @param userRepo Repository interface to manage operations related to user data.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
@Suppress("LongParameterList")
class ExportService(
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val reviewRepo: IReviewTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectAccessChecker: IProjectAccessChecker,
) : IExportService {
    override suspend fun getAvailableExportFormats(): Set<ExportFormat> = ProjectExportManager.getSupportedFormats()

    override suspend fun exportProject(projectId: UUID, format: ExportFormat): FileExport =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val project = projectRepo.getProjectById(projectId).getOrThrow()
            val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
            val projectPapers = projectPaperRepo.getAllProjectPapersWithPapers(projectId)
                .map {
                    val reviewsWithSelectedCriteriaIds =
                        reviewRepo.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(it.projectPaper.id)
                    it.toProjectPaperFull(reviewsWithSelectedCriteriaIds)
                }
            val projectCriteria = criterionRepo.getAllProjectCriteria(projectId)

            ProjectExportManager.exportProject(format, project, projectMembers, projectPapers, projectCriteria)
        }
}
