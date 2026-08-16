package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import java.util.UUID

class GetReviewByIdTest : ReviewServiceTest() {
    @Test
    fun `When a user requests a review and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val review = DataBuilder.createExampleReview()
        val selectedCriteriaIds = listOf(UUID.randomUUID())

        mockCurrentUser(user)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)
        coJustRun { reviewAccessCheckerMock.isAllowedToReadReview(user, review) }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        val reviewResult = service.getReviewById(review.id)

        assertEquals(review.id, reviewResult.id)
        assertEquals(1, reviewResult.selectedCriteriaIds.size)
        val selectedCriterionId = reviewResult.selectedCriteriaIds[0]
        assertEquals(selectedCriteriaIds[0], selectedCriterionId)
        assertReviewEquality(review, reviewResult)
    }

    @Test
    fun `When retrieving the review fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(user)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getReviewById(review.id) }
    }

    @Test
    fun `When a user requests a review, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(user)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)
        coEvery {
            reviewAccessCheckerMock.isAllowedToReadReview(user, review)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.getReviewById(review.id) }
    }
}
