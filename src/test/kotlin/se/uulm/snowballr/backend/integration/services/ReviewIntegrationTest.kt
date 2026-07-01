package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.ProjectOuterClass.PaperDecision
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

class ReviewIntegrationTest : IntegrationTest() {
    private suspend fun setupProjectAndPaper(): Pair<Project, GrpcProjectPaper> {
        var project = projectService.createProject(CreateProjectRequest(name = "Review Test Project"))
        val paper = createPaper()
        val projectPaper = projectPaperService.addPaperToProject(
            GrpcProjectPaper.Add.newBuilder()
                .setProjectId(project.id.toString())
                .setPaperId(paper.id.toString())
                .setStage(0)
                .build(),
        )

        val modifiedProject = project.copy(
            reviewDecisionMatrix = project.reviewDecisionMatrix.copy(
                numberOfReviewers = 1,
            ),
        )
        val projectUpdate = UpdateProjectRequest.fromProject(modifiedProject)

        project = projectService.updateProject(
            projectUpdate,
            setOf("project.settings.decision_matrix.number_of_reviewers"),
        )

        return project to projectPaper
    }

    @Nested
    inner class CreateReview {
        @Test
        fun `When a review is created, then it appears in the reviews list for the project paper`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER),
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val reviews = reviewService.getAllReviewsForProjectPaper(projectPaperId)
            assertTrue(reviews.isNotEmpty())
        }

        @Test
        fun `When a review is created, then it can be retrieved by ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            val review = reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER),
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
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER),
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProjectPaper = projectPaperService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_ACCEPTED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a declined review is created, then the paper decision is updated to declined`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER),
                    decision = ReviewDecision.DECLINED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProjectPaper = projectPaperService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_DECLINED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a paper is added to a project, then it initially has an unreviewed decision`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            val fetchedProjectPaper = projectPaperService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_UNREVIEWED, fetchedProjectPaper.decision)
        }

        @Test
        fun `When a review is submitted, then the SLR settings cannot be changed anymore`() = runTest {
            val (project, projectPaper) = setupProjectAndPaper()

            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER),
                    decision = ReviewDecision.ACCEPTED,
                    selectedCriteriaIds = emptyList(),
                ),
            )

            val updatedProject = project.copy(reviewMaybeAllowed = true)
            val updateRequest = UpdateProjectRequest.fromProject(updatedProject)

            assertThrows<FailedPreconditionException> {
                projectService.updateProject(updateRequest, setOf("project.settings.review_maybe_allowed"))
            }
        }
    }

    @Nested
    inner class AddPaperToProject {
        @Test
        fun `When a paper is added to a project, then it appears in the project's paper list`() = runTest {
            val (project, projectPaper) = setupProjectAndPaper()

            val papers = projectPaperService.getAllProjectPapersForProject(project.id)

            assertTrue(papers.projectPapersList.any { it.id == projectPaper.id })
        }

        @Test
        fun `When a paper is added to a project, then it can be retrieved by its project paper ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            val fetched = projectPaperService.getProjectPaperById(projectPaperId)
            assertEquals(projectPaper.id, fetched.id)
        }
    }
}
