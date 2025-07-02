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
class RevokeSessionTest : MainServiceTest() {
    @Test
    fun `When a session is correctly revoked, then no exception is thrown`() = testCoroutine {
        val session = DataBuilder.createExampleSession()

        coEvery { sessionRepoMock.revokeSessionById(session.id) } returns Unit

        assertDoesNotThrow { mainService.revokeSession(session.id) }
    }

    @Test
    fun `When an error occurs while revoking a session, then an exception is thrown`() = testCoroutine {
        val sessionId = DataBuilder.createExampleSession().id

        coEvery { sessionRepoMock.revokeSessionById(sessionId) } throws Exception("Failed to revoke session")

        assertThrows<Exception> { mainService.revokeSession(sessionId) }
    }
}
