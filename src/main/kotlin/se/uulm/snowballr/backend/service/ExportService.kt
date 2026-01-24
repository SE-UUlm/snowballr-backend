package se.uulm.snowballr.backend.service

import com.google.protobuf.kotlin.toByteString
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.toProjectPaperFull
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import snowballr.Export.AvailableExportFormatsResponse
import snowballr.Export.ExportRequest
import snowballr.Export.ExportResponse
import snowballr.exportResponse

interface IExportService {
    /**
     * Service implementation of [SnowballRService.getAvailableExportFormats].
     */
    suspend fun getAvailableExportFormats(): AvailableExportFormatsResponse

    /**
     * Service implementation of [SnowballRService.exportProject].
     */
    suspend fun exportProject(request: ExportRequest): ExportResponse
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
 */
class ExportService(
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val reviewRepo: IReviewTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
) : IExportService {
    override suspend fun getAvailableExportFormats(): AvailableExportFormatsResponse =
        AvailableExportFormatsResponse.newBuilder()
            .addAllFormats(ProjectExportManager.getSupportedFormats().map { it.toString() })
            .build()

    override suspend fun exportProject(request: ExportRequest): ExportResponse = withUser(userRepo) { currentUser ->
        val format = ProjectExportManager.getSupportedFormats().first { it.toString() == request.format }
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        isAllowedToReadProject(projectRepo, projectMemberRepo).checkFor(currentUser, projectId)

        val project = projectRepo.getProjectById(projectId).getOrThrow()
        val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
        val projectPapers = projectPaperRepo.getAllProjectPapersWithPapers(projectId)
            .map {
                val reviewsWithSelectedCriteriaIds =
                    reviewRepo.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(it.projectPaper.id)
                it.toProjectPaperFull(reviewsWithSelectedCriteriaIds)
            }
        val projectCriteria = criterionRepo.getAllProjectCriteria(projectId)

        val fileExport =
            ProjectExportManager.exportProject(format, project, projectMembers, projectPapers, projectCriteria)
        exportResponse {
            data = fileExport.data.toByteString()
            fileName = fileExport.filename
        }
    }
}
