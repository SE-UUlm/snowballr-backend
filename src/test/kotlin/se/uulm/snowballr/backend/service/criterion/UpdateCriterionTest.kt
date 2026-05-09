package se.uulm.snowballr.backend.service.criterion

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

class UpdateCriterionTest : MainServiceTest() {
    private fun getExampleRequest(criterion: Criterion): GrpcCriterion.Update {
        val updateFieldMask = FieldMaskUtil.fromStringList(
            listOf("tag", "name", "description", "category"),
        )

        return GrpcCriterion.Update.newBuilder()
            .setCriterion(criterion.toGrpcCriterion())
            .setMask(updateFieldMask)
            .build()
    }

    @Test
    fun `When retrieving the criterion fails, then the a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        val request = getExampleRequest(criterion)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.updateCriterion(request) }
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

        assertThrows<TestSpecificException> { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a user updates a criterion and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        val request = getExampleRequest(criterion)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToUpdateCriterion(user, criterion) }
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }
}
