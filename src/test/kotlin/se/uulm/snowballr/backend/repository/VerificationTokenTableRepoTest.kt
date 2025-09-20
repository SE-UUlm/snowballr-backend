package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.model.SnowballRException.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertTestVerificationToken
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess

class VerificationTokenTableRepoTest : RepositoryTest(arrayOf(UserTable, VerificationTokenTable), true) {
    private val repo = VerificationTokenTableRepo(db)

    @Nested
    inner class SaveVerificationToken {
        @Test
        fun `When a token is saved for an existent user, then it can be retrieved by its value`() = runTest {
            val tokenValue = "a-unique-token-for-saving"

            repo.saveVerificationToken(testUserId, tokenValue)

            val retrievedToken = assertResultSuccess(repo.getVerificationTokenByValue(tokenValue))
            assertThat(retrievedToken.userId).isEqualTo(testUserId)
            assertThat(retrievedToken.token).isEqualTo(tokenValue)
        }
    }

    @Nested
    inner class GetVerificationTokenByValue {
        @Test
        fun `When a token exists, then the correct token is returned`() = runTest {
            val tokenValue = "a-token-that-exists"
            insertTestVerificationToken(userId = testUserId, token = tokenValue)

            val result = assertResultSuccess(repo.getVerificationTokenByValue(tokenValue))
            assertThat(result.userId).isEqualTo(testUserId)
            assertThat(result.token).isEqualTo(tokenValue)
        }

        @Test
        fun `When a token does not exist, then the call returns a failed result`() = runTest {
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

            repo.deleteVerificationToken(tokenValue)

            assertResultFailure<VerificationTokenNotFoundException>(repo.getVerificationTokenByValue(tokenValue))
        }

        @Test
        fun `When deleting a non-existent token, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.deleteVerificationToken("token-that-never-existed")
            }
        }
    }
}
