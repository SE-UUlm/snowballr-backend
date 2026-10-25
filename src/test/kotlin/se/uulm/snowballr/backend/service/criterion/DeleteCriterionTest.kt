package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException

class DeleteCriterionTest : CriterionServiceTest() {
    @Test
    fun `When retrieving the criterion fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.deleteCriterion(criterion.id) }
        coVerify(exactly = 0) { criterionRepoMock.deleteCriterionById(any()) }
    }

    @Test
    fun `When a user deletes a criterion, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coEvery {
            criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.deleteCriterion(criterion.id) }
        coVerify(exactly = 0) { criterionRepoMock.deleteCriterionById(any()) }
    }

    @Test
    fun `When a project criterion is associated with a review, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleProjectCriterion()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
            coJustRun { criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion) }
            coEvery { criterionRepoMock.isAssociatedWithReviews(criterion.id) } returns true

            assertThrows<FailedPreconditionException> { service.deleteCriterion(criterion.id) }
            coVerify(exactly = 0) { criterionRepoMock.deleteCriterionById(any()) }
        }

    @Test
    fun `When a project criterion is not associated with a review, then the criterion is deleted`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion) }
        coEvery { criterionRepoMock.isAssociatedWithReviews(criterion.id) } returns false
        coJustRun { criterionRepoMock.deleteCriterionById(criterion.id) }

        service.deleteCriterion(criterion.id)

        coVerify(exactly = 1) { criterionRepoMock.deleteCriterionById(criterion.id) }
    }

    @Test
    fun `When a user criterion is deleted, then the criterion is deleted without checking review associations`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
            coJustRun { criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion) }
            coJustRun { criterionRepoMock.deleteCriterionById(criterion.id) }

            service.deleteCriterion(criterion.id)

            coVerify(exactly = 0) { criterionRepoMock.isAssociatedWithReviews(any()) }
            coVerify(exactly = 1) { criterionRepoMock.deleteCriterionById(criterion.id) }
        }
}
