package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertReviewAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import snowballr.ReviewOuterClass
import java.util.UUID

class ReviewTableRepoTest : RepositoryTest(arrayOf(ReviewTable, ProjectTable, ProjectPaperTable, PaperTable), true) {
    private val repo = ReviewTableRepo(db)

    @Nested
    inner class GetReviewById {
        @Test
        fun `When a review is found, then the correct review is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val reviewId = insertReviewAndGetId(projectPaperId, userId = testUserId)
            val review = repo.getReviewById(reviewId)

            assertThat(review.id).isEqualTo(reviewId)
            assertThat(review.projectPaperId).isEqualTo(projectPaperId)
            assertThat(review.userId).isEqualTo(testUserId)
            assertThat(review.decision).isEqualTo(ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED)
        }

        @Test
        fun `When a review is not found, then an exception is thrown`() = runTest {
            assertThrows<NotFoundException> { repo.getReviewById(UUID.randomUUID()) }
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
        fun `When the project paper has no reviews and other reviews exist, then only the project paper reviews are returned`() =
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
}
