package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.CriterionOuterClass
import java.util.UUID

class CreateCriterionTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When creating a criterion fails, then an exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { criterionRepoMock.createCriterion(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When a criterion is correctly created, then no exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()
        val criterion = DataBuilder.createExampleCriterion()

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }
}
