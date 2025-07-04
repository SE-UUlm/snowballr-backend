package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.SessionTable
import se.uulm.snowballr.backend.testCoroutine
import java.time.OffsetDateTime
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class SessionTableRepoTest : H2DatabaseTest(arrayOf(SessionTable), true) {
    private val repo = SessionTableRepo(db)

    @Nested
    inner class CreateSession {
        @Test
        fun `When a session is created, then all values are correctly assigned`() = testCoroutine {
            val session = repo.createSession(testUserId)

            assertThat(session.userId).isEqualTo(testUserId)
            assertThat(session.revoked).isFalse()
            assertThat(session.expiresAt).isAfterOrEqualTo(OffsetDateTime.now())
            assertThat(session.createdAt).isBeforeOrEqualTo(OffsetDateTime.now())
        }

        @Test
        fun `When two sessions are created, then they have different IDs`() = testCoroutine {
            val session1 = repo.createSession(testUserId)
            val session2 = repo.createSession(testUserId)

            assertThat(session1.id).isNotEqualTo(session2.id)
        }

        @Test
        fun `When a session is created, but the assigned user does not exist, then an exception is thrown`() =
            testCoroutine {
                assertThrows<NotFoundException> { repo.createSession(UUID.randomUUID()) }
            }
    }

    @Nested
    inner class GetSessionById {
        @Test
        fun `When a session is found, then the correct session is returned`() = testCoroutine {
            val session = repo.createSession(testUserId)

            val foundSession = repo.getSessionById(session.id)

            assertThat(foundSession.id).isEqualTo(session.id)
            assertThat(foundSession.userId).isEqualTo(testUserId)
            assertThat(foundSession.revoked).isFalse()
            assertThat(foundSession.expiresAt).isAfterOrEqualTo(OffsetDateTime.now())
            assertThat(foundSession.createdAt).isBeforeOrEqualTo(OffsetDateTime.now())
        }

        @Test
        fun `When a session is not found, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException> { repo.getSessionById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class RevokeSession {
        @Test
        fun `When a session is revoked, then it is marked as revoked`() = testCoroutine {
            val session = repo.createSession(testUserId)

            repo.revokeSessionById(session.id)

            val updatedSession = repo.getSessionById(session.id)
            assertThat(updatedSession.revoked).isTrue()
        }

        @Test
        fun `When a non-existing session is revoked, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException> { repo.revokeSessionById(UUID.randomUUID()) }
        }
    }
}
