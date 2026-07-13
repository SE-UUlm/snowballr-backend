package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithPaper
import java.util.UUID

class GetPapersToReviewForProjectTest : ProjectPaperServiceTest() {
    @Test
    fun `When the project papers to review are requested, then only the undecided papers are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaperAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.ACCEPTED,
        )
        val projectPaperNotAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.UNREVIEWED,
        )
        val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperAlreadyDecided, paper)
        val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperNotAlreadyDecided, paper)
        val review = DataBuilder.createExampleReviewWithSelectedCriteriaIds(
            review = DataBuilder.createExampleReview(userId = UUID.randomUUID()),
            selectedCriteriaIds = listOf(UUID.randomUUID()),
        )

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaperAlreadyDecided.id)
        } returns listOf(review)
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaperNotAlreadyDecided.id)
        } returns listOf(review)

        val projectPapers = service.getPapersToReviewForProject(project.id)

        assertThat(projectPapers).hasSize(1)
        assertThat(projectPapers).anyMatch { it.id == projectPaperNotAlreadyDecided.id }
        assertThat(projectPapers).noneMatch { it.id == projectPaperAlreadyDecided.id }
    }

    @Test
    fun `When the project papers to review are requested, then only undecided papers that were not already reviewed by the current user are returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(authors = listOf(author))
            val projectPaperWithCurrentUserReview = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper.id,
                decision = PaperDecision.UNREVIEWED,
            )
            val projectPaperWithoutCurrentUserReview = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper.id,
                decision = PaperDecision.UNREVIEWED,
            )
            val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperWithCurrentUserReview, paper)
            val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperWithoutCurrentUserReview, paper)
            val reviewByCurrentUser = DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                review = DataBuilder.createExampleReview(userId = currentUser.id),
                selectedCriteriaIds = listOf(UUID.randomUUID()),
            )
            val reviewByOtherUser = DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                review = DataBuilder.createExampleReview(userId = UUID.randomUUID()),
                selectedCriteriaIds = listOf(UUID.randomUUID()),
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
            } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(UUID.randomUUID())
            coEvery {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaperWithCurrentUserReview.id)
            } returns listOf(reviewByCurrentUser)
            coEvery {
                reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
                    projectPaperWithoutCurrentUserReview.id,
                )
            } returns listOf(reviewByOtherUser)

            val projectPapers = service.getPapersToReviewForProject(project.id)

            assertThat(projectPapers).hasSize(1)
            assertThat(projectPapers).anyMatch { it.id == projectPaperWithoutCurrentUserReview.id }
            assertThat(projectPapers).noneMatch { it.id == projectPaperWithCurrentUserReview.id }
        }

    @Test
    fun `When a project paper has no reviews yet, then it is returned as paper to review`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.UNREVIEWED,
        )
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectPaperRepoMock.getAllProjectPapersWithPapers(project.id) } returns listOf(projectPaperWithPaper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaper.id)
        } returns emptyList()

        val result = service.getPapersToReviewForProject(project.id)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(projectPaper.id)
    }

    @Test
    fun `When a user requests project papers to review, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getPapersToReviewForProject(project.id) }
        }
}
