package se.uulm.snowballr.backend.integration.services

import com.google.protobuf.FieldMask
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.Project
import snowballr.ReviewOuterClass.ReviewDecision
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

class ReviewIntegrationTest : IntegrationTest() {
    private suspend fun setupProjectAndPaper(): Pair<Project, GrpcProjectPaper> {
        var project = mainService.createProject(Project.Create.newBuilder().setName("Review Test Project").build())

        val paper = createPaper()

        val projectPaper = mainService.addPaperToProject(
            GrpcProjectPaper.Add.newBuilder()
                .setProjectId(project.id)
                .setPaperId(paper.id)
                .setStage(0)
                .build(),
        )

        val modifiedProject = project.toBuilder().setSettings(
            project.settings.toBuilder().setDecisionMatrix(
                project.settings.decisionMatrix.toBuilder()
                    .setNumberOfReviewers(1)
                    .build(),
            ).build(),
        ).build()
        val projectUpdate = Project.Update.newBuilder()
            .setProject(modifiedProject)
            .setMask(FieldMask.newBuilder().addPaths("project.settings.decision_matrix.number_of_reviewers"))
            .build()
        project = mainService.updateProject(projectUpdate)

        return project to projectPaper
    }

    @Nested
    inner class CreateReview {
        @Test
        fun `When a review is created, then it appears in the reviews list for the project paper`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            mainService.createReview(
                GrpcReview.Create.newBuilder()
                    .setProjectPaperId(projectPaper.id)
                    .setDecision(ReviewDecision.REVIEW_DECISION_ACCEPTED)
                    .build(),
            )

            val reviews = mainService.getAllReviewsForProjectPaper(projectPaperId)
            assertTrue(reviews.reviewsList.isNotEmpty())
        }

        @Test
        fun `When a review is created, then it can be retrieved by ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()

            val review = mainService.createReview(
                GrpcReview.Create.newBuilder()
                    .setProjectPaperId(projectPaper.id)
                    .setDecision(ReviewDecision.REVIEW_DECISION_DECLINED)
                    .build(),
            )
            val reviewId = parseUUID(review.id, EntityType.REVIEW)

            val fetched = mainService.getReviewById(reviewId)

            assertEquals(review.id, fetched.id)
            assertEquals(ReviewDecision.REVIEW_DECISION_DECLINED, fetched.decision)
        }

        @Test
        fun `When an accepted review is created, then the paper decision is updated to accepted`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            mainService.createReview(
                GrpcReview.Create.newBuilder()
                    .setProjectPaperId(projectPaper.id)
                    .setDecision(ReviewDecision.REVIEW_DECISION_ACCEPTED)
                    .build(),
            )

            val updatedProjectPaper = mainService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_ACCEPTED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a declined review is created, then the paper decision is updated to declined`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            mainService.createReview(
                GrpcReview.Create.newBuilder()
                    .setProjectPaperId(projectPaper.id)
                    .setDecision(ReviewDecision.REVIEW_DECISION_DECLINED)
                    .build(),
            )

            val updatedProjectPaper = mainService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_DECLINED, updatedProjectPaper.decision)
        }

        @Test
        fun `When a paper is added to a project, then it initially has an unreviewed decision`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            val fetchedProjectPaper = mainService.getProjectPaperById(projectPaperId)
            assertEquals(PaperDecision.PAPER_DECISION_UNREVIEWED, fetchedProjectPaper.decision)
        }
    }

    @Nested
    inner class AddPaperToProject {
        @Test
        fun `When a paper is added to a project, then it appears in the project's paper list`() = runTest {
            val (project, projectPaper) = setupProjectAndPaper()
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val papers = mainService.getAllProjectPapersForProject(projectId)
            assertTrue(papers.projectPapersList.any { it.id == projectPaper.id })
        }

        @Test
        fun `When a paper is added to a project, then it can be retrieved by its project paper ID`() = runTest {
            val (_, projectPaper) = setupProjectAndPaper()
            val projectPaperId = parseUUID(projectPaper.id, EntityType.PROJECT_PAPER)

            val fetched = mainService.getProjectPaperById(projectPaperId)
            assertEquals(projectPaper.id, fetched.id)
        }
    }
}
