package se.uulm.snowballr.backend.service.export

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.copy
import snowballr.exportRequest
import java.util.UUID

class ExportProjectTest : MainServiceTest() {
    fun getExampleRequest() = exportRequest {
        id = UUID.randomUUID().toString()
        format = ExportFormat.JSON.toString()
    }

    @Test
    fun `When the exported project doesn't exist, then a TestSpecificException is thrown`() = runTest {
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }

        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectRepoMock.getProjectById(projectId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.exportProject(request) }
    }

    @Test
    fun `When exporting a project is successful, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()
        val request = getExampleRequest().copy { id = projectId.toString() }

        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(ExportFormat.JSON)
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.success(DataBuilder.createExampleProject())
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(projectId) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(projectId)
        } returns listOf(DataBuilder.createExampleProjectPaperWithPaper())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(any()) } returns emptyList()
        every { ProjectExportManager.exportProject(any(), any(), any(), any()) } returns ByteArray(0)

        mainService.exportProject(request)
    }
}
