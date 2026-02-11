package se.uulm.snowballr.backend.repository

import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.isBetweenWithDelta
import se.uulm.snowballr.backend.isEqualToWithDelta
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertTestVerificationToken
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.time.OffsetDateTime

class VerificationTokenTableRepoTest : RepositoryTest(arrayOf(UserTable, VerificationTokenTable), true) {
    private val repo = VerificationTokenTableRepo(db, envReaderMock)

    @Nested
    inner class SaveVerificationToken {
        @Test
        fun `When a token is saved for an existent user, then it can be retrieved by its value`() = runTest {
            val tokenValue = "a-unique-token-for-saving"

            every { envReaderMock.env.lifetime.verificationTokenLifeTimeInDays } returns 1

            val expectedExpirationDate = OffsetDateTime.now().plusDays(1)
            repo.saveVerificationToken(testUserId, tokenValue)

            val retrievedToken = assertResultSuccess(repo.getVerificationTokenByValue(tokenValue))
            assertEquals(testUserId, retrievedToken.userId)
            assertEquals(tokenValue, retrievedToken.token)
            assertThat(retrievedToken.expiresAt).isEqualToWithDelta(expectedExpirationDate)
        }
    }

    @Nested
    inner class GetVerificationTokenByValue {
        @Test
        fun `When a token exists, then the correct token is returned`() = runTest {
            val tokenValue = "a-token-that-exists"
            insertTestVerificationToken(userId = testUserId, token = tokenValue)

            val result = assertResultSuccess(repo.getVerificationTokenByValue(tokenValue))
            assertEquals(testUserId, result.userId)
            assertEquals(tokenValue, result.token)
        }

        @Test
        fun `When a token does not exist, then a failed result with a VerificationTokenNotFoundException is returned`() =
            runTest {
                assertResultFailure<VerificationTokenNotFoundException>(
                    repo.getVerificationTokenByValue("non-existent-token"),
                )
            }
    }

    @Nested
    inner class DeleteVerificationToken {
        @Test
        fun `When a token exists, then it is deleted successfully`() = runTest {
            val tokenValue = "a-token-to-be-deleted"
            insertTestVerificationToken(userId = testUserId, token = tokenValue)

            assertResultSuccess(repo.getVerificationTokenByValue(tokenValue))

            assertDoesNotThrow { repo.deleteVerificationToken(tokenValue) }

            assertResultFailure<VerificationTokenNotFoundException>(repo.getVerificationTokenByValue(tokenValue))
        }

        @Test
        fun `When deleting a non-existent token, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.deleteVerificationToken("token-that-never-existed")
            }
        }
    }

    @Nested
    inner class DeleteExpiredVerificationTokens {
        @Test
        fun `When no expired tokens exist, then no tokens are deleted`() = runTest {
            insertTestVerificationToken(testUserId, "valid-token")

            assertDoesNotThrow { repo.deleteExpiredVerificationTokens() }

            assertResultSuccess(repo.getVerificationTokenByValue("valid-token"))
        }

        @Test
        fun `When expired tokens exist, then they are deleted`() = runTest {
            insertTestVerificationToken(
                testUserId,
                "expired-token",
                expiresAt = OffsetDateTime.now().minusDays(1),
            )

            assertDoesNotThrow { repo.deleteExpiredVerificationTokens() }

            assertResultFailure<VerificationTokenNotFoundException>(
                repo.getVerificationTokenByValue("expired-token"),
            )
        }
    }
}
