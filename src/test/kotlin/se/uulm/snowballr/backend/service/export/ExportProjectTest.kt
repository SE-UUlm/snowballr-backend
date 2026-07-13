package se.uulm.snowballr.backend.service.export

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import java.util.UUID
import kotlin.test.assertEquals

class ExportProjectTest : ExportServiceTest() {
    private val testFormat = ExportFormat.JSON

    @Test
    fun `When a user exports a project and has access, then the result has the correct values`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectId = UUID.randomUUID()

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, projectId) }
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

        val result = service.exportProject(projectId, testFormat)

        assertEquals("test.json", result.filename)
        assertEquals(ByteArray(0).toList(), result.data.toList())
    }

    @Test
    fun `When a user exports a project, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.exportProject(project.id, testFormat) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.exportProject(project.id, testFormat) }
    }
}
