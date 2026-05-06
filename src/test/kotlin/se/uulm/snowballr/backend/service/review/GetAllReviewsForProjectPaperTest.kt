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
class GetAllReviewsForProjectPaperTest : MainServiceTest() {
    @Test
    fun `When the user requests all reviews and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()
        val review = DataBuilder.createExampleReview()
        val selectedCriteriaIds = listOf(UUID.randomUUID())

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId) }
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        val reviews = mainService.getAllReviewsForProjectPaper(projectPaper.id).reviewsList

        assertEquals(1, reviews.size)
        assertEquals(review.id.toString(), reviews[0].id)
        assertEquals(1, reviews[0].selectedCriteriaIdsCount)
        val selectedCriterionId = reviews[0].selectedCriteriaIdsList[0]
        assertEquals(selectedCriteriaIds[0].toString(), selectedCriterionId)
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(projectPaper.id) }
    }

    @Test
    fun `When the user requests all reviews, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coEvery {
            projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(projectPaper.id) }
    }
}
