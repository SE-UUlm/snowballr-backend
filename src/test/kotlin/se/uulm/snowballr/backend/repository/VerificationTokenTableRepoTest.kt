package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import java.util.UUID

class VerificationTokenTableRepoTest : RepositoryTest(arrayOf(UserTable, VerificationTokenTable), true) {
    private val repo = VerificationTokenTableRepo(db)

    private suspend fun insertTestToken(userId: UUID = testUserId, token: String = "secure-random-token-123") {
        db.query {
            VerificationTokenTable.insert {
                it[VerificationTokenTable.userId] = userId
                it[VerificationTokenTable.token] = token
            }
        }
    }

    @Nested
    inner class SaveVerificationToken {
        @Test
        fun `When a token is saved for an existing user, then it can be retrieved by its value`() = runTest {
            val tokenValue = "a-unique-token-for-saving"

            repo.saveVerificationToken(testUserId, tokenValue)

            val retrievedToken = repo.getVerificationTokenByValue(tokenValue)
            assertThat(retrievedToken).isNotNull()
            assertThat(retrievedToken!!.userId).isEqualTo(testUserId)
            assertThat(retrievedToken.token).isEqualTo(tokenValue)
        }
    }

    @Nested
    inner class GetVerificationTokenByValue {
        @Test
        fun `When a token exists, then the correct token is returned`() = runTest {
            val tokenValue = "a-token-that-exists"
            insertTestToken(userId = testUserId, token = tokenValue)

            val result = repo.getVerificationTokenByValue(tokenValue)

            assertThat(result).isNotNull()
            assertThat(result!!.userId).isEqualTo(testUserId)
            assertThat(result.token).isEqualTo(tokenValue)
        }

        @Test
        fun `When a token does not exist, then null is returned`() = runTest {
            val result = repo.getVerificationTokenByValue("non-existent-token")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class DeleteVerificationToken {
        @Test
        fun `When a token exists, then it is deleted successfully`() = runTest {
            val tokenValue = "a-token-to-be-deleted"
            insertTestToken(userId = testUserId, token = tokenValue)

            val beforeDelete = repo.getVerificationTokenByValue(tokenValue)
            assertThat(beforeDelete).isNotNull()

            repo.deleteVerificationToken(tokenValue)

            val afterDelete = repo.getVerificationTokenByValue(tokenValue)
            assertThat(afterDelete).isNull()
        }

        @Test
        fun `When deleting a non-existent token, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.deleteVerificationToken("token-that-never-existed")
            }
        }
    }
}
