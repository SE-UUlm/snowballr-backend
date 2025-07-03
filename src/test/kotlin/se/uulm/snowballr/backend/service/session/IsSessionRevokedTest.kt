package se.uulm.snowballr.backend.service.session

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class IsSessionRevokedTest : MainServiceTest() {
    @Test
    fun `When a session is not revoked, then isSessionRevoked returns false`() = testCoroutine {
        val session = DataBuilder.createExampleSession()

        coEvery { sessionRepoMock.getSessionById(session.id) } returns session

        assertFalse { mainService.isSessionRevoked(session.id) }
    }

    @Test
    fun `When a session is revoked, then isSessionRevoked returns true`() = testCoroutine {
        val session = DataBuilder.createExampleSession(revoked = true)

        coEvery { sessionRepoMock.getSessionById(session.id) } returns session

        assertTrue { mainService.isSessionRevoked(session.id) }
    }
}
