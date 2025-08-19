package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertTestToken
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
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

            val retrievedToken = repo.getInvitationTokenByValue(tokenValue)
            assertNotNull(retrievedToken)
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

            val result = repo.getInvitationTokenByValue(tokenValue)

            assertNotNull(result)
            assertEquals(testEmail, result.email)
            assertEquals(projectId, result.projectId)
            assertEquals(tokenValue, result.token)
        }

        @Test
        fun `When a token does not exist, then null is returned`() = runTest {
            val result = repo.getInvitationTokenByValue("non-existent-token")

            assertNull(result)
        }
    }

    @Nested
    inner class GetInvitationTokenByEmailAndProjectId {
        @Test
        fun `When a token exists for email and project id, then the correct token is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-token-for-email-and-project"
            insertTestToken(testEmail, projectId, tokenValue)

            val result = repo.getInvitationTokenByEmailAndProjectId(testEmail, projectId)

            assertNotNull(result)
            assertEquals(tokenValue, result.token)
        }

        @Test
        fun `When a token exists for the email but not the project, then null is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            insertTestToken(testEmail, projectId)
            val anotherProjectId = UUID.randomUUID()

            val result = repo.getInvitationTokenByEmailAndProjectId(testEmail, anotherProjectId)

            assertNull(result)
        }

        @Test
        fun `When a token exists for the project but not the email, then null is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            insertTestToken(testEmail, projectId)
            val anotherEmail = "another.email@example.com"

            val result = repo.getInvitationTokenByEmailAndProjectId(anotherEmail, projectId)

            assertNull(result)
        }

        @Test
        fun `When no token exists for the email and project, then null is returned`() = runTest {
            val result = repo.getInvitationTokenByEmailAndProjectId("no.such.email@example.com", UUID.randomUUID())

            assertNull(result)
        }
    }

    @Nested
    inner class DeleteInvitationToken {
        @Test
        fun `When a token exists, then it is deleted successfully`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            val tokenValue = "a-token-to-be-deleted"
            insertTestToken(testEmail, projectId, tokenValue)

            val beforeDelete = repo.getInvitationTokenByValue(tokenValue)
            assertNotNull(beforeDelete)

            repo.deleteInvitationToken(tokenValue)

            val afterDelete = repo.getInvitationTokenByValue(tokenValue)
            assertNull(afterDelete)
        }

        @Test
        fun `When deleting a non-existent token, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.deleteInvitationToken("token-that-never-existed")
            }
        }
    }
}
