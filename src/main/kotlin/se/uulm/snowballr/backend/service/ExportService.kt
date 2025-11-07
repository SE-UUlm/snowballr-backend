package se.uulm.snowballr.backend.service

import com.google.protobuf.kotlin.toByteString
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import snowballr.Base
import snowballr.Export.AvailableExportFormatsReply
import snowballr.Export.ExportRequest
import snowballr.blob

interface IExportService {
    /**
     * Service implementation of [SnowballRService.getAvailableExportFormats].
     */
    suspend fun getAvailableExportFormats(): AvailableExportFormatsReply

    /**
     * Service implementation of [SnowballRService.exportProject].
     */
    suspend fun exportProject(request: ExportRequest): Base.Blob
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
 */
class ExportService(
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val reviewRepo: IReviewTableRepo,
) : IExportService {
    override suspend fun getAvailableExportFormats(): AvailableExportFormatsReply =
        AvailableExportFormatsReply.newBuilder()
            .addAllFormats(ProjectExportManager.getSupportedFormats().map { it.toString() })
            .build()

    override suspend fun exportProject(request: ExportRequest): Base.Blob {
        val format = ProjectExportManager.getSupportedFormats().first { it.toString() == request.format }
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        val project = projectRepo.getProjectById(projectId).getOrThrow()
        val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
        val projectPapers = projectPaperRepo.getAllProjectPapersWithPapers(projectId)
            .map { it.toProjectPaperFull() }

        val bytes = ProjectExportManager.exportProject(format, project, projectMembers, projectPapers)
        return blob {
            data = bytes.toByteString()
        }
    }

    private suspend fun ProjectPaperWithPaper.toProjectPaperFull(): ProjectPaperFull {
        val reviews = reviewRepo.getAllReviewsForProjectPaper(this.projectPaper.id)
        return ProjectPaperFull(
            projectPaper = this.projectPaper,
            paper = this.paper,
            reviews = reviews,
        )
    }
}
