package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReviewByIdTest : MainServiceTest() {
    @Test
    fun `When a user requests a review and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val review = DataBuilder.createExampleReview()
        val selectedCriteriaIds = listOf(UUID.randomUUID())

        mockCurrentUser(user)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)
        coJustRun { reviewAccessCheckerMock.isAllowedToReadReview(user, review) }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        val reviewResult = mainService.getReviewById(review.id)

        assertEquals(review.id.toString(), reviewResult.id)
        assertEquals(1, reviewResult.selectedCriteriaIdsCount)
        val selectedCriterionId = reviewResult.selectedCriteriaIdsList[0]
        assertEquals(selectedCriteriaIds[0].toString(), selectedCriterionId)
    }

    @Test
    fun `When retrieving the review fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(user)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getReviewById(review.id) }
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

        assertThrows<TestSpecificException> { mainService.getReviewById(review.id) }
    }
}
