package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest

class GetCriterionByIdTest : MainServiceTest() {
    @Test
    fun `When retrieving the criterion fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleUserCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getCriterionById(criterion.id) }
    }

    @Test
    fun `When a user retrieves a criterion, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleUserCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coEvery { criterionAccessCheckerMock.isAllowedToReadCriterion(user, criterion) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getCriterionById(criterion.id) }
    }

    @Test
    fun `When a user retrieves a criterion and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleUserCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToReadCriterion(user, criterion) }

        assertDoesNotThrow { mainService.getCriterionById(criterion.id) }
    }
}
