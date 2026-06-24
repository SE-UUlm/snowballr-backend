package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import java.util.UUID

class SoftDeleteUserTest : UserServiceTest() {
    @Test
    fun `When retrieving user to delete fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUserId = UUID.randomUUID()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.softDeleteUser(requestedUserId) }
    }

    @Test
    fun `When a user soft-deletes another user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToDelete = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(userToDelete.id) } returns Result.success(userToDelete)
        coEvery {
            userAccessCheckerMock.isAllowedToDeleteUser(currentUser, userToDelete)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.softDeleteUser(userToDelete.id) }
    }

    @Test
    fun `When a user soft-deletes another user, but they are a last project admin, then TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val userToDelete = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(userToDelete.id) } returns Result.success(userToDelete)
            coJustRun { userAccessCheckerMock.isAllowedToDeleteUser(currentUser, userToDelete) }
            coEvery { projectRepoMock.getUserProjects(userToDelete.id, any()) } returns listOf(project)
            coEvery {
                projectAccessCheckerMock.isNotLastProjectAdmin(userToDelete, project.id, any())
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.softDeleteUser(userToDelete.id) }
        }

    @Test
    fun `When a user soft-deletes another user and has access, then the other user is successfully deleted`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val userToDelete = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(userToDelete.id) } returns Result.success(userToDelete)
            coJustRun { userAccessCheckerMock.isAllowedToDeleteUser(currentUser, userToDelete) }
            coEvery { projectRepoMock.getUserProjects(userToDelete.id, any()) } returns listOf(project)
            coJustRun { userRepoMock.softDeleteUser(userToDelete.id) }
            coJustRun { projectAccessCheckerMock.isNotLastProjectAdmin(userToDelete, project.id, any()) }

            service.softDeleteUser(userToDelete.id)

            coVerify(exactly = 1) { userRepoMock.softDeleteUser(userToDelete.id) }
        }
}
