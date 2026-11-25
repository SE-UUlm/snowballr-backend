package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import java.util.UUID

class GetNextPaperToReviewTest : ProjectPaperServiceTest() {
    @Test
    fun `When a user requests the next project paper and has access, then the correct values are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            stage = 0,
            localPaperId = 0,
        )
        val otherPaper = DataBuilder.createExamplePaper()
        val nextProjectPaper = DataBuilder.createExampleProjectPaper(
            stage = 0,
            localPaperId = 1,
            paperId = otherPaper.id,
        )
        val review = DataBuilder.createExampleReview(projectId = projectPaper.projectId, paperId = projectPaper.paperId)

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaper(projectPaper.projectId, projectPaper.paperId)
        } returns Result.success(projectPaper)
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(project.id, projectPaper.localPaperId, projectPaper.stage)
        } returns listOf(nextProjectPaper)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.projectId, nextProjectPaper.paperId)
        } returns listOf(review)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(otherPaper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(otherPaper.id) } returns emptyList()
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                nextProjectPaper.projectId,
                nextProjectPaper.paperId,
            )
        } returns emptyList()

        val result = service.getNextPaperToReview(projectPaper.projectId, projectPaper.paperId)

        assertProjectPaperEquality(nextProjectPaper, result)
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id)

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaper(projectPaper.projectId, projectPaper.paperId)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> {
            service.getNextPaperToReview(projectPaper.projectId, projectPaper.paperId)
        }
    }

    @Test
    fun `When a user requests the next project paper, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> {
                service.getNextPaperToReview(projectPaper.projectId, projectPaper.paperId)
            }
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
        val review = DataBuilder.createExampleReview(
            projectId = projectPaper.projectId,
            paperId = projectPaper.paperId,
            userId = currentUser.id,
        )

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaper(projectPaper.projectId, projectPaper.paperId)
        } returns Result.success(projectPaper)
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(project.id, projectPaper.localPaperId, projectPaper.stage)
        } returns listOf(nextProjectPaper)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.projectId, nextProjectPaper.paperId)
        } returns listOf(review)

        assertThrows<FailedPreconditionException> {
            service.getNextPaperToReview(projectPaper.projectId, projectPaper.paperId)
        }
    }

    @Test
    @Suppress("LongMethod")
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
                decision = PaperDecision.UNREVIEWED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 0,
                localPaperId = 2,
                decision = PaperDecision.UNREVIEWED,
            )
            val review = DataBuilder.createExampleReview(
                projectId = projectPaper1.projectId,
                paperId = projectPaper1.paperId,
                userId = currentUser.id,
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getProjectPaper(currentProjectPaper.projectId, currentProjectPaper.paperId)
            } returns Result.success(currentProjectPaper)
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id,
                    currentProjectPaper.localPaperId,
                    currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper1.projectId,
                    projectPaper1.paperId,
                )
            } returns listOf(review)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper2.id) } returns Result.success(paper2)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns emptyList()
            coEvery {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            } returns emptyList()

            service.getNextPaperToReview(currentProjectPaper.projectId, currentProjectPaper.paperId)
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.projectId, projectPaper1.paperId) }
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.projectId, projectPaper2.paperId) }
            coVerify(exactly = 1) {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            }
        }

    @Test
    @Suppress("LongMethod")
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
            decision = PaperDecision.UNREVIEWED,
        )
        val projectPaper2 = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper2.id,
            stage = 1,
            localPaperId = 2,
            decision = PaperDecision.UNREVIEWED,
        )
        val projectPaper3 = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper3.id,
            stage = 0,
            localPaperId = 3,
            decision = PaperDecision.UNREVIEWED,
        )
        val review = DataBuilder.createExampleReview(
            projectId = projectPaper1.projectId,
            paperId = projectPaper1.paperId,
            userId = UUID.randomUUID(),
        )

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getProjectPaper(currentProjectPaper.projectId, currentProjectPaper.paperId)
        } returns Result.success(currentProjectPaper)
        coEvery {
            projectPaperRepoMock.getSubsequentProjectPapers(
                project.id,
                currentProjectPaper.localPaperId,
                currentProjectPaper.stage,
            )
        } returns listOf(projectPaper1, projectPaper2, projectPaper3)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(
                projectPaper1.projectId,
                projectPaper1.paperId,
            )
        } returns listOf(review)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(
                projectPaper2.projectId,
                projectPaper2.paperId,
            )
        } returns emptyList()
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(
                projectPaper3.projectId,
                projectPaper3.paperId,
            )
        } returns emptyList()

        coEvery { paperRepoMock.getPaperById(paper3.id) } returns Result.success(paper3)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper3.id) } returns emptyList()
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                projectPaper3.projectId,
                projectPaper3.paperId,
            )
        } returns emptyList()

        service.getNextPaperToReview(currentProjectPaper.projectId, currentProjectPaper.paperId)
        coVerify(
            exactly = 1,
        ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.projectId, projectPaper1.paperId) }
        coVerify(
            exactly = 1,
        ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.projectId, projectPaper2.paperId) }
        coVerify(
            exactly = 1,
        ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper3.projectId, projectPaper3.paperId) }
        coVerify(
            exactly = 1,
        ) {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                projectPaper3.projectId,
                projectPaper3.paperId,
            )
        }
    }

    @Test
    @Suppress("LongMethod")
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
                decision = PaperDecision.ACCEPTED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 1,
                localPaperId = 2,
                decision = PaperDecision.UNREVIEWED,
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getProjectPaper(currentProjectPaper.projectId, currentProjectPaper.paperId)
            } returns Result.success(currentProjectPaper)
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id,
                    currentProjectPaper.localPaperId,
                    currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper1.projectId,
                    projectPaper1.paperId,
                )
            } returns emptyList()
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper2.id) } returns Result.success(paper2)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns emptyList()
            coEvery {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            } returns emptyList()

            service.getNextPaperToReview(currentProjectPaper.projectId, currentProjectPaper.paperId)
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.projectId, projectPaper1.paperId) }
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.projectId, projectPaper2.paperId) }
            coVerify(exactly = 1) {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            }
        }

    @Test
    @Suppress("LongMethod")
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
                decision = PaperDecision.ACCEPTED,
            )
            val projectPaper2 = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper2.id,
                stage = 1,
                localPaperId = 2,
                decision = PaperDecision.ACCEPTED,
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getProjectPaper(currentProjectPaper.projectId, currentProjectPaper.paperId)
            } returns Result.success(currentProjectPaper)
            coEvery {
                projectPaperRepoMock.getSubsequentProjectPapers(
                    project.id,
                    currentProjectPaper.localPaperId,
                    currentProjectPaper.stage,
                )
            } returns listOf(projectPaper1, projectPaper2)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper1.projectId,
                    projectPaper1.paperId,
                )
            } returns emptyList()
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(
                    projectPaper2.projectId,
                    projectPaper2.paperId,
                )
            } returns emptyList()
            coEvery {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper1.projectId,
                    projectPaper1.paperId,
                )
            } returns emptyList()

            coEvery { paperRepoMock.getPaperById(paper1.id) } returns Result.success(paper1)

            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()

            service.getNextPaperToReview(currentProjectPaper.projectId, currentProjectPaper.paperId)
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.projectId, projectPaper1.paperId) }
            coVerify(
                exactly = 1,
            ) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper2.projectId, projectPaper2.paperId) }
            coVerify(exactly = 1) {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaper1.projectId,
                    projectPaper1.paperId,
                )
            }
        }
}
