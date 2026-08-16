package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionField
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest

class UpdateCriterionTest : CriterionServiceTest() {
    private val allFields = CriterionField.entries.toSet()

    private fun getExampleRequest(criterion: Criterion) = UpdateCriterionRequest(
        criterionId = criterion.id,
        tag = criterion.tag,
        name = criterion.name,
        description = criterion.description,
        category = criterion.category,
    )

    @Test
    fun `When retrieving the criterion fails, then the a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        val request = getExampleRequest(criterion)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.updateCriterion(request, allFields) }
    }

    @Test
    fun `When a user updates a criterion, but has no access, then the a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        val request = getExampleRequest(criterion)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coEvery {
            criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateCriterion(request, allFields) }
    }

    @Test
    fun `When a user updates a criterion and has access, then the updated criterion is returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        val request = getExampleRequest(criterion)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion) }
        coEvery { criterionRepoMock.updateCriterion(request, allFields) } returns criterion

        val result = service.updateCriterion(request, allFields)

        assertCriterionEquality(criterion, result)
    }
}
