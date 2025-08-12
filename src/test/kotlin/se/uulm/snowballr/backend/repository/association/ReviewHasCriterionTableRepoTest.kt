package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignCriterionToReview
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertCriterionAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertReviewAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.association.ReviewTable

class ReviewHasCriterionTableRepoTest : RepositoryTest(
    arrayOf(
        ReviewHasCriterionTable,
        ProjectTable,
        PaperTable,
        ProjectPaperTable,
        ReviewTable,
        CriterionTable,
    ),
    true,
) {
    private val repo = ReviewHasCriterionTableRepo(db)

    @Nested
    inner class GetSelectedCriteriaIdsForReviewById {
        @Test
        fun `When a selected criteria for the given review exist, then the criteria ids are returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val reviewId = insertReviewAndGetId(projectPaperId, userId = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            assignCriterionToReview(reviewId, criterionId)
            val selectedCriteriaIds = repo.getSelectedCriteriaIdsForReviewById(reviewId)

            assertThat(selectedCriteriaIds).hasSize(1)
            assertThat(selectedCriteriaIds).anyMatch { it == criterionId }
        }

        @Test
        fun `When no selected criteria for the given review exist, then an empty list is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val reviewId = insertReviewAndGetId(projectPaperId, userId = testUserId)
            insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val selectedCriteriaIds = repo.getSelectedCriteriaIdsForReviewById(reviewId)

            assertThat(selectedCriteriaIds).isEmpty()
        }

        @Test
        fun `When a selected criteria for the given review exist beside other criteria for other reviews, then only the selected criteria ids for the given review are returned`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId1 = insertPaperAndGetId()
                val paperId2 = insertPaperAndGetId()
                val projectPaperId1 =
                    insertProjectPaperAndGetId(paperId = paperId1, projectId = projectId, createdBy = testUserId)
                val projectPaperId2 =
                    insertProjectPaperAndGetId(paperId = paperId2, projectId = projectId, createdBy = testUserId)
                val reviewId1 = insertReviewAndGetId(projectPaperId1, userId = testUserId)
                val reviewId2 = insertReviewAndGetId(projectPaperId2, userId = testUserId)
                val criterionId1 = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
                val criterionId2 = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
                assignCriterionToReview(reviewId1, criterionId1)
                assignCriterionToReview(reviewId2, criterionId2)
                val selectedCriteriaIds = repo.getSelectedCriteriaIdsForReviewById(reviewId1)

                assertThat(selectedCriteriaIds).hasSize(1)
                assertThat(selectedCriteriaIds).anyMatch { it == criterionId1 }
                assertThat(selectedCriteriaIds).noneMatch { it == criterionId2 }
            }
    }
}
