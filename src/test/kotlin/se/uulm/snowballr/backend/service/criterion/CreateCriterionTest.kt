package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.CriterionOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class CreateCriterionTest : MainServiceTest() {
    @Test
    fun `When the requesting user has an invalid ID, then an exception is thrown`() = testCoroutine {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

        dummyUserId = "invalid-UUID"

        assertThrows<InvalidIdException.UUID> { mainService.createCriterion(request) }
    }

    @Test
    fun `When a criterion is correctly created, then no exception is thrown`() = testCoroutine {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()
        val criterion = DataBuilder.createExampleCriterion()

        coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion

        dummyUserId = UUID.randomUUID().toString()

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When an error occurs while a criterion is created, then an exception is thrown`() = testCoroutine {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

        coEvery { criterionRepoMock.createCriterion(any(), any()) } throws TestSpecificException()

        dummyUserId = UUID.randomUUID().toString()

        assertThrows<TestSpecificException> { mainService.createCriterion(request) }
    }
}
