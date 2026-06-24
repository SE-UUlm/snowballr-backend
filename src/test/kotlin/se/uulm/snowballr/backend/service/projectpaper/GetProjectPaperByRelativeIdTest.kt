package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetProjectPaperByRelativeIdTest : ProjectPaperServiceTest() {
    private fun getRequest(projectId: UUID, relativeId: Long = -1) = GrpcProjectPaper.Get
        .newBuilder()
        .setProjectId(projectId.toString())
        .setRelativeProjectPaperId(relativeId.toString())
        .build()

    @Test
    fun `When a user request the project paper and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val relativeId = 1L
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(paperId = paper.id)

        val request = getRequest(project.id, relativeId)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, relativeId)
        } returns Result.success(projectPaper)
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns emptyList()

        val result = service.getProjectPaperByRelativeId(request)

        assertProjectPaperEquality(projectPaper, result)
    }

    @Test
    fun `When a user requests the project paper, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val request = getRequest(project.id)

            mockCurrentUser(user)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(user, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getProjectPaperByRelativeId(request) }
        }

    @Test
    fun `When retrieving the relative paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val relativeId = 2L

        val request = getRequest(project.id, relativeId)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, relativeId)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getProjectPaperByRelativeId(request) }
    }
}
