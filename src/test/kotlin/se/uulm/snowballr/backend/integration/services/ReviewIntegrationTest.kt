package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.ProjectField
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse

class ReviewIntegrationTest : IntegrationTest() {
    private suspend fun setupProjectAndPaper(): Pair<ProjectResponse, ProjectPaperResponse> {
        var project = projectService.createProject(CreateProjectRequest(name = "Review Test Project"))
        val paper = createPaper()
        val projectPaper = projectPaperService.addPaperToProject(project.id, paper.id, 0)

        val modifiedProject = project.copy(
            reviewDecisionMatrix = project.reviewDecisionMatrix.copy(
                numberOfReviewers = 1,
            ),
        )
        val projectUpdate = UpdateProjectRequest.fromProjectResponse(modifiedProject)

        project = projectService.updateProject(projectUpdate, setOf(ProjectField.NUMBER_OF_REVIEWERS))

        return project to projectPaper
    }

    @Nested
    inner class CreateReview {
        @Test
        fun `When a review is created, then it appears in the reviews list for the project paper`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val reviews = reviewService.getAllReviewsForProjectPaper(projectPaper.id)
            assertTrue(reviews.isNotEmpty())
        }

        @Test
        fun `When a review is created, then it can be retrieved by ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            val review = reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.DECLINED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val fetched = reviewService.getReviewById(review.id)

            assertEquals(review.id, fetched.id)
            assertEquals(ReviewDecision.DECLINED, fetched.decision)
        }

        @Test
        fun `When an accepted review is created, then the paper decision is updated to accepted`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProjectPaper = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(PaperDecision.ACCEPTED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a declined review is created, then the paper decision is updated to declined`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.DECLINED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProjectPaper = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(PaperDecision.DECLINED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a paper is added to a project, then it initially has an unreviewed decision`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            val fetchedProjectPaper = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(PaperDecision.UNREVIEWED, fetchedProjectPaper.decision)
        }

        @Test
        fun `When a review is submitted, then the SLR settings cannot be changed anymore`() = runTest {
            val (project, projectPaper) = setupProjectAndPaper()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProject = project.copy(reviewMaybeAllowed = true)
            val updateRequest = UpdateProjectRequest.fromProjectResponse(updatedProject)

            assertThrows<FailedPreconditionException> {
                projectService.updateProject(updateRequest, setOf(ProjectField.REVIEW_MAYBE_ALLOWED))
            }
        }
    }

    @Nested
    inner class AddPaperToProject {
        @Test
        fun `When a paper is added to a project, then it appears in the project's paper list`() = runTest {
            val (project, projectPaper) = setupProjectAndPaper()

            val papers = projectPaperService.getAllProjectPapersForProject(project.id)

            assertTrue(papers.any { it.id == projectPaper.id })
        }

        @Test
        fun `When a paper is added to a project, then it can be retrieved by its project paper ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            val fetched = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(projectPaper.id, fetched.id)
        }
    }
}
