package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.service.MainServiceTest
import kotlin.test.assertEquals

class GetNextPaperTest : MainServiceTest() {
    @Test
    fun `When a user requests the next project paper and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val nextProjectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectRepoMock.getProjectById(projectPaper.projectId) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(project.id, projectPaper.localPaperId, PaperNavigationDirection.NEXT)
        } returns Result.success(nextProjectPaper)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id) } returns emptyList()

        val nextPaper = mainService.getNextPaper(projectPaper.id)

        assertEquals(nextProjectPaper.id.toString(), nextPaper.id)
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaper(projectPaper.id) }
    }

    @Test
    fun `When a user requests the next project paper, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val paper = DataBuilder.createExamplePaper()
            val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaper.id)
            } returns Result.success(projectPaper)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.getNextPaper(projectPaper.id) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaper(projectPaper.id) }
    }

    @Test
    fun `When retrieving the next project paper fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(project.id, projectPaper.localPaperId, PaperNavigationDirection.NEXT)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaper(projectPaper.id) }
    }

    @Test
    fun `When retrieving the next paper fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val nextProjectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(project.id, projectPaper.localPaperId, PaperNavigationDirection.NEXT)
        } returns Result.success(nextProjectPaper)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaper(projectPaper.id) }
    }
}
