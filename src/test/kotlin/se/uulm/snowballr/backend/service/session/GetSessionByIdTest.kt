package se.uulm.snowballr.backend.service.session

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetSessionByIdTest : MainServiceTest() {
    @Test
    fun `When a session is correctly retrieved, then no exception is thrown`() = testCoroutine {
        val session = DataBuilder.createExampleSession()

        coEvery { sessionRepoMock.getSessionById(session.id) } returns session

        assertDoesNotThrow { mainService.getSessionById(session.id) }
    }

    @Test
    fun `When an error occurs while a session is retrieved, then an exception is thrown`() = testCoroutine {
        val sessionId = DataBuilder.createExampleSession().id

        coEvery { sessionRepoMock.getSessionById(sessionId) } throws Exception("Failed to retrieve session")

        assertThrows<Exception> { mainService.getSessionById(sessionId) }
    }
}
