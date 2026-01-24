package se.uulm.snowballr.backend.service.export

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import snowballr.copy
import snowballr.exportRequest
import java.util.UUID

class ExportProjectTest : MainServiceTest() {
    fun getExampleRequest() = exportRequest {
        id = UUID.randomUUID().toString()
        format = ExportFormat.JSON.toString()
    }

    @Test
    fun `When exporting a project is successful, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }
        val projectMember = DataBuilder.createExampleProjectMember(projectId = projectId, userId = user.id)

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns listOf(projectMember)
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.success(DataBuilder.createExampleProject())
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(projectId) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(projectId)
        } returns listOf(DataBuilder.createExampleProjectPaperWithPaper())
        coEvery { reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(any()) } returns emptyList()
        coEvery { criterionRepoMock.getAllProjectCriteria(any()) } returns emptyList()
        every {
            ProjectExportManager.exportProject(any(), any(), any(), any(), any())
        } returns FileExport(ByteArray(0), "test.json")

        mainService.exportProject(request)
    }

    @Test
    fun `When the non-project-member exports a project, then an UnauthorizedReadException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()

        assertThrows<UnauthorizedReadException> {
            mainService.exportProject(request)
        }
    }

    @Test
    fun `When a server admin export a project, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
        coEvery {
            projectRepoMock.getProjectById(projectId)
        } returns Result.success(DataBuilder.createExampleProject(id = projectId))
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(projectId) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(projectId)
        } returns listOf(DataBuilder.createExampleProjectPaperWithPaper())
        coEvery { reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(any()) } returns emptyList()
        coEvery { criterionRepoMock.getAllProjectCriteria(any()) } returns emptyList()
        every {
            ProjectExportManager.exportProject(any(), any(), any(), any(), any())
        } returns FileExport(ByteArray(0), "test.json")

        mainService.exportProject(request)
    }

    @Test
    fun `When the exported project doesn't exist, then a NotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

        assertThrows<NotFoundException> { mainService.exportProject(request) }
    }
}
