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
class GetProjectPaperByIdTest : ProjectPaperServiceTest() {
    @Test
    fun `When a user requests a project paper and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(paperId = paper.id)

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId) }
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns emptyList()

        val result = service.getProjectPaperById(projectPaper.id)

        assertProjectPaperEquality(projectPaper, result)
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getProjectPaperById(projectPaper.id) }
    }

    @Test
    fun `When a user requests a project paper, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coEvery {
            projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.getProjectPaperById(projectPaper.id) }
    }
}
