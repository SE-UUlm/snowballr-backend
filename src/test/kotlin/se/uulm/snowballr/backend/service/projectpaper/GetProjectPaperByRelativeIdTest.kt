package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetProjectPaperByRelativeIdTest : ProjectPaperServiceTest() {
    @Test
    fun `When a user request the project paper and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val relativeId = 1
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(paperId = paper.id)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, relativeId)
        } returns Result.success(projectPaper)
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns emptyList()

        val result = service.getProjectPaperByRelativeId(project.id, relativeId)

        assertProjectPaperEquality(projectPaper, result)
    }

    @Test
    fun `When a user requests the project paper, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(user, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getProjectPaperByRelativeId(project.id, -1) }
        }

    @Test
    fun `When retrieving the relative paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val relativeId = 2

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, relativeId)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getProjectPaperByRelativeId(project.id, relativeId) }
    }
}
