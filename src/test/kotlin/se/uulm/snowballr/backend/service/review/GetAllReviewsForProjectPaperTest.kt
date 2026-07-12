package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import java.util.UUID
import kotlin.test.assertEquals

class GetAllReviewsForProjectPaperTest : ReviewServiceTest() {
    @Test
    fun `When a user requests all reviews and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()
        val selectedCriteriaIds = listOf(UUID.randomUUID())
        val review = DataBuilder.createExampleReviewWithSelectedCriteriaIds(selectedCriteriaIds = selectedCriteriaIds)

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId) }
        coEvery {
            reviewRepoMock.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaper.id)
        } returns listOf(review)

        val reviews = service.getAllReviewsForProjectPaper(projectPaper.id)

        assertEquals(1, reviews.size)
        assertEquals(review.review.id, reviews[0].id)
        assertEquals(1, reviews[0].selectedCriteriaIds.size)
        val selectedCriterionId = reviews[0].selectedCriteriaIds[0]
        assertEquals(selectedCriteriaIds[0], selectedCriterionId)
        assertReviewEquality(review.review, reviews[0])
    }

    @Test
    fun `When retrieving the project paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getAllReviewsForProjectPaper(projectPaper.id) }
    }

    @Test
    fun `When a user requests all reviews, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(user)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coEvery {
            projectAccessCheckerMock.isAllowedToReadProject(user, projectPaper.projectId)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.getAllReviewsForProjectPaper(projectPaper.id) }
    }
}
