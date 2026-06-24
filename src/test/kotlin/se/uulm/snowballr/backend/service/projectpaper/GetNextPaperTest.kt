package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.PaperNavigationDirection

class GetNextPaperTest : ProjectPaperServiceTest() {
    @Test
    fun `When a user requests the next project paper and has access, then the correct values are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val otherPaper = DataBuilder.createExamplePaper()
        val nextProjectPaper = DataBuilder.createExampleProjectPaper(paperId = otherPaper.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectRepoMock.getProjectById(projectPaper.projectId) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(project.id, projectPaper.localPaperId, PaperNavigationDirection.NEXT)
        } returns Result.success(nextProjectPaper)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(otherPaper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(otherPaper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id) } returns emptyList()

        val nextPaper = service.getNextPaper(projectPaper.id)

        assertProjectPaperEquality(nextProjectPaper, nextPaper)
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

        assertThrows<TestSpecificException> { service.getNextPaper(projectPaper.id) }
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

            assertThrows<TestSpecificException> { service.getNextPaper(projectPaper.id) }
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

        assertThrows<TestSpecificException> { service.getNextPaper(projectPaper.id) }
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

        assertThrows<TestSpecificException> { service.getNextPaper(projectPaper.id) }
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

        assertThrows<TestSpecificException> { service.getNextPaper(projectPaper.id) }
    }
}
