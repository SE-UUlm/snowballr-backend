package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.model.SnowballRException.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertTestToken
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.util.UUID

class InvitationTokenTableRepoTest : RepositoryTest(arrayOf(UserTable, ProjectTable, InvitationTokenTable), true) {
    private val repo = InvitationTokenTableRepo(db)

    private val testEmail = "test.invite@example.com"

    @Nested
    inner class SaveInvitationToken {
        @Test
        fun `When a token is saved, then it can be retrieved by its value`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-unique-token-for-saving"

            repo.saveInvitationToken(testEmail, projectId, tokenValue)

            val retrievedToken = assertResultSuccess(repo.getInvitationTokenByValue(tokenValue))
            assertEquals(testEmail, retrievedToken.email)
            assertEquals(projectId, retrievedToken.projectId)
            assertEquals(tokenValue, retrievedToken.token)
        }
    }

    @Nested
    inner class GetInvitationTokenByValue {
        @Test
        fun `When a token exists, then the correct token is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-token-that-exists"
            insertTestToken(testEmail, projectId, tokenValue)

            val result = assertResultSuccess(repo.getInvitationTokenByValue(tokenValue))
            assertEquals(testEmail, result.email)
            assertEquals(projectId, result.projectId)
            assertEquals(tokenValue, result.token)
        }

        @Test
        fun `When a token does not exist, then the call returns a failed result`() = runTest {
            assertResultFailure<InvitationTokenNotFoundException>(repo.getInvitationTokenByValue("non-existent-token"))
        }
    }

    @Nested
    inner class GetInvitationTokenByEmailAndProjectId {
        @Test
        fun `When a token exists for email and project id, then the correct token is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-token-for-email-and-project"
            insertTestToken(testEmail, projectId, tokenValue)

            val result = assertResultSuccess(repo.getInvitationTokenByEmailAndProjectId(testEmail, projectId))
            assertEquals(tokenValue, result.token)
        }

        @Test
        fun `When a token exists for the email but not the project, then the call returns a failed result`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            insertTestToken(testEmail, projectId)
            val anotherProjectId = UUID.randomUUID()

            assertResultFailure<InvitationTokenNotFoundException>(
                repo.getInvitationTokenByEmailAndProjectId(
                    testEmail,
                    anotherProjectId,
                ),
            )
        }

        @Test
        fun `When a token exists for the project but not the email, then the call returns a failed result`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            insertTestToken(testEmail, projectId)
            val anotherEmail = "another.email@example.com"

            assertResultFailure<InvitationTokenNotFoundException>(
                repo.getInvitationTokenByEmailAndProjectId(
                    anotherEmail,
                    projectId,
                ),
            )
        }

        @Test
        fun `When no token exists for the email and project, then the call returns a failed result`() = runTest {
            assertResultFailure<InvitationTokenNotFoundException>(
                repo.getInvitationTokenByEmailAndProjectId(
                    "no.such.email@example.com",
                    UUID.randomUUID(),
                ),
            )
        }
    }

    @Nested
    inner class DeleteInvitationToken {
        @Test
        fun `When a token exists, then it is deleted successfully`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-token-to-be-deleted"
            insertTestToken(testEmail, projectId, tokenValue)

            assertResultSuccess(repo.getInvitationTokenByValue(tokenValue))

            repo.deleteInvitationToken(tokenValue)

            assertResultFailure<InvitationTokenNotFoundException>(repo.getInvitationTokenByValue(tokenValue))
        }

        @Test
        fun `When deleting a non-existent token, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.deleteInvitationToken("token-that-never-existed")
            }
        }
    }
}
