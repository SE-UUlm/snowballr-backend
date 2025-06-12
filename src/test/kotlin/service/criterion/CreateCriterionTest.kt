package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.CriterionOuterClass

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class CreateCriterionTest : MainServiceTest() {
    @Test
    fun `When a criterion is correctly created, then no exception is thrown`() =
        testCoroutine {
            val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()
            val criterion = CriterionOuterClass.Criterion.getDefaultInstance()

            coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion

            assertDoesNotThrow { mainService.createCriterion(request) }
        }

    @Test
    fun `When an error occurs while a criterion is created, then an exception is thrown`() =
        testCoroutine {
            val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

            coEvery { criterionRepoMock.createCriterion(any(), any()) } throws Exception("Failed to create criterion")

            assertThrows<Exception> { mainService.createCriterion(request) }
        }
}
