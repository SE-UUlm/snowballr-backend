package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertReviewAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.ReviewOuterClass.Review
import snowballr.ReviewOuterClass.ReviewDecision
import java.sql.SQLException
import java.util.UUID

class ReviewTableRepoTest : RepositoryTest(arrayOf(ReviewTable, ProjectTable, ProjectPaperTable, PaperTable), true) {
    private val repo = ReviewTableRepo(db)

    @Nested
    inner class GetReviewById {
        @Test
        fun `When a review is found, then a successful result with the correct review is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val reviewId = insertReviewAndGetId(projectPaperId, userId = testUserId)
            val result = repo.getReviewById(reviewId)

            val review = assertResultSuccess(result)
            assertEquals(reviewId, review.id)
            assertEquals(projectPaperId, review.projectPaperId)
            assertEquals(testUserId, review.userId)
            assertEquals(ReviewDecision.REVIEW_DECISION_ACCEPTED, review.decision)
        }

        @Test
        fun `When a review is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getReviewById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class GetAllReviewsForProjectPaper {
        @Test
        fun `When the project paper has reviews, then the reviews are correctly returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val reviewId = insertReviewAndGetId(projectPaperId, userId = testUserId)
            val reviews = repo.getAllReviewsForProjectPaper(projectPaperId)

            assertThat(reviews).hasSize(1)
            assertThat(reviews).anyMatch { it.id == reviewId }
        }

        @Test
        fun `When the project paper has no reviews, then an empty list is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId1 = insertPaperAndGetId()
            val paperId2 = insertPaperAndGetId()
            val noReviewProjectPaperId = insertProjectPaperAndGetId(
                paperId = paperId1,
                projectId = projectId,
                createdBy = testUserId,
            )
            val reviewProjectPaperId = insertProjectPaperAndGetId(
                paperId = paperId2,
                projectId = projectId,
                createdBy = testUserId,
            )
            val reviewId = insertReviewAndGetId(reviewProjectPaperId, userId = testUserId)
            val reviews = repo.getAllReviewsForProjectPaper(noReviewProjectPaperId)

            assertThat(reviews).hasSize(0)
            assertThat(reviews).noneMatch { it.id == reviewId }
        }

        @Test
        fun `When the project paper has reviews and other reviews exist too, then only the project paper reviews are returned`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId1 = insertPaperAndGetId()
                val paperId2 = insertPaperAndGetId()
                val projectPaperId1 = insertProjectPaperAndGetId(
                    paperId = paperId1,
                    projectId = projectId,
                    createdBy = testUserId,
                )
                val projectPaperId2 = insertProjectPaperAndGetId(
                    paperId = paperId2,
                    projectId = projectId,
                    createdBy = testUserId,
                )
                val reviewId1 = insertReviewAndGetId(projectPaperId1, userId = testUserId)
                val reviewId2 = insertReviewAndGetId(projectPaperId2, userId = testUserId)
                val reviews = repo.getAllReviewsForProjectPaper(projectPaperId1)

                assertThat(reviews).hasSize(1)
                assertThat(reviews).anyMatch { it.id == reviewId1 }
                assertThat(reviews).noneMatch { it.id == reviewId2 }
            }
    }

    @Nested
    inner class CreateReview {
        private fun createReviewRequest(projectPaperId: UUID) = Review.Create.newBuilder()
            .setProjectPaperId(projectPaperId.toString())
            .setDecision(ReviewDecision.REVIEW_DECISION_ACCEPTED)
            .build()

        @Test
        fun `When a review is created, then the correct review is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val userId = insertUserAndGetId(email = "existing.reviewer@example.com")

            val review = repo.createReview(createReviewRequest(projectPaperId), userId)

            assertEquals(projectPaperId, review.projectPaperId)
            assertEquals(ReviewDecision.REVIEW_DECISION_ACCEPTED, review.decision)
            assertEquals(userId, review.userId)
        }

        @Test
        fun `When a review is created, but the assigned user does not exist, then a SQLException is thrown`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)

                assertThrows<SQLException> { repo.createReview(createReviewRequest(projectPaperId), UUID.randomUUID()) }
            }

        @Test
        fun `When a review is created, but the user already reviewed this project paper, then a SQLException is thrown`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
                val userId = insertUserAndGetId(email = "double.reviewing.user@example.com")
                insertReviewAndGetId(projectPaperId, userId)

                assertThrows<SQLException> { repo.createReview(createReviewRequest(projectPaperId), userId) }
            }

        // TODO: (question for reviewer): Check whether the selected criteria are correctly stored for the review
    }
}
