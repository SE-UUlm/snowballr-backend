package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.PaperDecision
import java.util.UUID

class GetNextPaperToReviewTest : MainServiceTest() {
    @Test
    fun `When a user requests the next project paper and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            stage = 0,
            localPaperId = 0,
        )
        val nextProjectPaper = DataBuilder.createExampleProjectPaper(stage = 0, localPaperId = 1)
        val review = DataBuilder.createExampleReview(projectPaperId = projectPaper.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(
                project.id, projectPaper.localPaperId, projectPaper.stage,
            )
        } returns listOf(nextProjectPaper)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id)
        } returns listOf(review)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id) } returns emptyList()

        assertDoesNotThrow { mainService.getNextPaperToReview(projectPaper.id) }
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaperToReview(projectPaper.id) }
    }

    @Test
    fun `When a user requests the next project paper, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaper.id)
            } returns Result.success(projectPaper)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.getNextPaperToReview(projectPaper.id) }
        }

    @Test
    fun `When no next paper to review exists, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            stage = 0,
            localPaperId = 0,
        )
        val nextProjectPaper = DataBuilder.createExampleProjectPaper(stage = 0, localPaperId = 1)
        val review = DataBuilder.createExampleReview(projectPaperId = projectPaper.id, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(
                project.id, projectPaper.localPaperId, projectPaper.stage,
            )
        } returns listOf(nextProjectPaper)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id)
        } returns listOf(review)

        assertThrows<FailedPreconditionException> { mainService.getNextPaperToReview(projectPaper.id) }
    }

    @Test
    fun `When a user requests the next project paper, then the project papers without own review are filtered out correctly`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val currentPaper = DataBuilder.createExamplePaper()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper()
            val currentProjectPaper = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = currentPaper.id,
                stage = 0,
                localPaperId = 0,
            )
            val projectPaper1 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper1.id,
                stage = 0,
                localPaperId = 1,
                decision = PaperDecision.PAPER_DECISION_UNSPECIFIED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 0,
                localPaperId = 2,
                decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
            )
            val review = DataBuilder.createExampleReview(projectPaperId = projectPaper1.id, userId = currentUser.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(currentProjectPaper.id)
            } returns Result.success(currentProjectPaper)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id, currentProjectPaper.localPaperId, currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) } returns listOf(review)
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper2.id) } returns Result.success(paper2)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns emptyList()

            assertDoesNotThrow { mainService.getNextPaperToReview(currentProjectPaper.id) }
            coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) }
            coVerify(exactly = 2) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) }
        }

    @Test
    fun `When a user requests the next project paper, then the project papers are sorted correctly`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val currentPaper = DataBuilder.createExamplePaper()
        val paper1 = DataBuilder.createExamplePaper()
        val paper2 = DataBuilder.createExamplePaper()
        val paper3 = DataBuilder.createExamplePaper()
        val currentProjectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = currentPaper.id,
            stage = 0,
            localPaperId = 0,
        )
        val projectPaper1 = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper1.id,
            stage = 0,
            localPaperId = 1,
            decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        )
        val projectPaper2 = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper2.id,
            stage = 1,
            localPaperId = 2,
            decision = PaperDecision.PAPER_DECISION_UNSPECIFIED,
        )
        val projectPaper3 = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper3.id,
            stage = 0,
            localPaperId = 3,
            decision = PaperDecision.PAPER_DECISION_UNSPECIFIED,
        )
        val review = DataBuilder.createExampleReview(projectPaperId = projectPaper1.id, userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(currentProjectPaper.id)
        } returns Result.success(currentProjectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(
                project.id, currentProjectPaper.localPaperId, currentProjectPaper.stage,
            )
        } returns listOf(projectPaper1, projectPaper2, projectPaper3)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) } returns listOf(review)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper3.id) } returns emptyList()

        coEvery { paperRepoMock.getPaperById(paper3.id) } returns Result.success(paper3)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper3.id) } returns emptyList()

        assertDoesNotThrow { mainService.getNextPaperToReview(currentProjectPaper.id) }
        coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) }
        coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) }
        coVerify(exactly = 2) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper3.id) }
    }

    @Test
    fun `When a user requests the next project paper, then the already decided paper are correctly filtered`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val currentPaper = DataBuilder.createExamplePaper()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper()
            val currentProjectPaper = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = currentPaper.id,
                stage = 0,
                localPaperId = 0,
            )
            val projectPaper1 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper1.id,
                stage = 0,
                localPaperId = 1,
                decision = PaperDecision.PAPER_DECISION_ACCEPTED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 1,
                localPaperId = 2,
                decision = PaperDecision.PAPER_DECISION_UNSPECIFIED,
            )

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(currentProjectPaper.id)
            } returns Result.success(currentProjectPaper)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id, currentProjectPaper.localPaperId, currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) } returns emptyList()
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper2.id) } returns Result.success(paper2)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns emptyList()

            assertDoesNotThrow { mainService.getNextPaperToReview(currentProjectPaper.id) }
            coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) }
            coVerify(exactly = 2) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) }
        }

    @Test
    fun `When a user requests the next project paper and only already decided papers are left, then an already decided paper is returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val currentPaper = DataBuilder.createExamplePaper()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper()
            val currentProjectPaper = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = currentPaper.id,
                stage = 0,
                localPaperId = 0,
            )
            val projectPaper1 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper1.id,
                stage = 0,
                localPaperId = 1,
                decision = PaperDecision.PAPER_DECISION_ACCEPTED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 1,
                localPaperId = 2,
                decision = PaperDecision.PAPER_DECISION_ACCEPTED,
            )

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(currentProjectPaper.id)
            } returns Result.success(currentProjectPaper)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id, currentProjectPaper.localPaperId, currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) } returns emptyList()
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper1.id) } returns Result.success(paper1)

            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()

            assertDoesNotThrow { mainService.getNextPaperToReview(currentProjectPaper.id) }
            coVerify(exactly = 2) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.id) }
            coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.id) }
        }
}
