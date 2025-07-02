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
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class CreateSessionTest : MainServiceTest() {
    @Test
    fun `When a session is correctly created, then no exception is thrown`() = testCoroutine {
        val userId = UUID.randomUUID()
        val session = DataBuilder.createExampleSession(userId = userId)

        coEvery { sessionRepoMock.createSession(any()) } returns session

        assertDoesNotThrow { mainService.createSession(userId) }
    }

    @Test
    fun `When an error occurs while a session is created, then an exception is thrown`() = testCoroutine {
        val userId = UUID.randomUUID()

        coEvery { sessionRepoMock.createSession(any()) } throws Exception("Failed to create session")

        assertThrows<Exception> { mainService.createSession(userId) }
    }
}
