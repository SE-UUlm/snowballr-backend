package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.PaperDecision
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPapersToReviewForProjectTest : MainServiceTest() {
    @Test
    fun `When the project papers to review are requested, then only the undecided papers are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaperAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.PAPER_DECISION_ACCEPTED,
        )
        val projectPaperNotAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        )
        val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperAlreadyDecided, paper)
        val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperNotAlreadyDecided, paper)
        val review = DataBuilder.createExampleReview(userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(projectPaperAlreadyDecided.id)
        } returns listOf(review)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(projectPaperNotAlreadyDecided.id)
        } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())

        var projectPapers: GrpcProjectPaper.List
        assertDoesNotThrow { projectPapers = mainService.getPapersToReviewForProject(project.id) }
        assertThat(projectPapers.projectPapersList).hasSize(1)
        assertThat(projectPapers.projectPapersList).anyMatch { it.id == projectPaperNotAlreadyDecided.id.toString() }
        assertThat(projectPapers.projectPapersList).noneMatch { it.id == projectPaperAlreadyDecided.id.toString() }
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
                decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
            )
            val projectPaperWithoutCurrentUserReview = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper.id,
                decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
            )
            val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperWithCurrentUserReview, paper)
            val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperWithoutCurrentUserReview, paper)
            val reviewByCurrentUser = DataBuilder.createExampleReview(userId = currentUser.id)
            val reviewByOtherUser = DataBuilder.createExampleReview(userId = UUID.randomUUID())

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
            } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(UUID.randomUUID())
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaperWithCurrentUserReview.id)
            } returns listOf(reviewByCurrentUser)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaperWithoutCurrentUserReview.id)
            } returns listOf(reviewByOtherUser)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviewByCurrentUser.id)
            } returns listOf(UUID.randomUUID())
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviewByOtherUser.id)
            } returns listOf(UUID.randomUUID())

            var projectPapers: GrpcProjectPaper.List
            assertDoesNotThrow { projectPapers = mainService.getPapersToReviewForProject(project.id) }
            assertThat(projectPapers.projectPapersList).hasSize(1)
            assertThat(projectPapers.projectPapersList)
                .anyMatch { it.id == projectPaperWithoutCurrentUserReview.id.toString() }
            assertThat(projectPapers.projectPapersList)
                .noneMatch { it.id == projectPaperWithCurrentUserReview.id.toString() }
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
            decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        )
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
        coEvery { projectPaperRepoMock.getAllProjectPapersWithPapers(project.id) } returns listOf(projectPaperWithPaper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns emptyList()

        val result = mainService.getPapersToReviewForProject(project.id)
        assertThat(result.projectPapersList).hasSize(1)
        assertThat(result.projectPapersList.first().id).isEqualTo(projectPaper.id.toString())
    }

    @Test
    fun `When the user requests project papers to review, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.getPapersToReviewForProject(project.id) }
        }
}
