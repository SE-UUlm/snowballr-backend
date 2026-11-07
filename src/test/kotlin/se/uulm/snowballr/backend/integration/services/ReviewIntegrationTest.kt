package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.invalidargument.CriteriaNotInProjectException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import java.util.UUID

class ReviewIntegrationTest : IntegrationTest() {
    @Nested
    inner class CreateReview {
        private suspend fun createCriterion(projectId: UUID?): Criterion = criterionService.createCriterion(
            CreateCriterionRequest(
                tag = "TC",
                name = "Test Criterion",
                description = "A test criterion",
                category = CriterionCategory.EXCLUSION,
                projectId = projectId,
            ),
        )

        private fun declineRequest(projectPaperId: UUID, criterion: Criterion) = CreateReviewRequest(
            projectPaperId = projectPaperId,
            decision = ReviewDecision.DECLINED,
            selectedCriteriaIds = listOf(criterion.id),
        )

        @Test
        fun `When a review selects a criterion of the same project, then the criterion is stored with the review`() =
            runTest {
                val (project, projectPaper) = setupProjectAndPaperForReview()
                val criterion = createCriterion(project.id)

                val review = reviewService.createReview(declineRequest(projectPaper.id, criterion))

                val fetched = reviewService.getReviewById(review.id)
                assertEquals(listOf(criterion.id), fetched.selectedCriteriaIds)
            }

        @Test
        fun `When a review selects a criterion of another project, then the creation fails and no review is stored`() =
            runTest {
                val (_, projectPaper) = setupProjectAndPaperForReview()
                val otherProject = projectService.createProject(CreateProjectRequest(name = "Other Project"))
                val foreignCriterion = createCriterion(otherProject.id)

                assertThrows<CriteriaNotInProjectException> {
                    reviewService.createReview(declineRequest(projectPaper.id, foreignCriterion))
                }
                assertTrue(reviewService.getAllReviewsForProjectPaper(projectPaper.id).isEmpty())
            }

        @Test
        fun `When a review selects a user criterion, then the creation fails`() = runTest {
            val (_, projectPaper) = setupProjectAndPaperForReview()
            val userCriterion = createCriterion(projectId = null)

            assertThrows<CriteriaNotInProjectException> {
                reviewService.createReview(declineRequest(projectPaper.id, userCriterion))
            }
        }

        @Test
        fun `When a review is created, then it appears in the reviews list for the project paper`() = runTest {
            val (_, projectPaper) = setupProjectAndPaperForReview()

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
            val (_, projectPaper) = setupProjectAndPaperForReview()

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
            val (_, projectPaper) = setupProjectAndPaperForReview()

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
            val (_, projectPaper) = setupProjectAndPaperForReview()

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
            val (_, projectPaper) = setupProjectAndPaperForReview()

            val fetchedProjectPaper = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(PaperDecision.UNREVIEWED, fetchedProjectPaper.decision)
        }

        @Test
        fun `When a review is submitted, then the SLR settings cannot be changed anymore`() = runTest {
            val (project, projectPaper) = setupProjectAndPaperForReview()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProject = project.copy(settings = project.settings.copy(reviewMaybeAllowed = true))
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
            val (project, projectPaper) = setupProjectAndPaperForReview()

            val papers = projectPaperService.getAllProjectPapersForProject(project.id)

            assertTrue(papers.any { it.id == projectPaper.id })
        }

        @Test
        fun `When a paper is added to a project, then it can be retrieved by its project paper ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaperForReview()

            val fetched = projectPaperService.getProjectPaperById(projectPaper.id)
            assertEquals(projectPaper.id, fetched.id)
        }
    }
}
