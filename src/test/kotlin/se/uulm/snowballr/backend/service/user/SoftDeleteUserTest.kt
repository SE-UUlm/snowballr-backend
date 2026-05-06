package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class SoftDeleteUserTest : MainServiceTest() {
    @Test
    fun `When retrieving user to delete fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUserId = UUID.randomUUID()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.softDeleteUser(requestedUserId) }
    }

    @Test
    fun `When the user soft-deletes another user, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val userToDelete = DataBuilder.createExampleUser()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(userToDelete.id) } returns Result.success(userToDelete)
            coEvery {
                userAccessCheckerMock.isAllowedToDeleteUser(currentUser, userToDelete)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.softDeleteUser(userToDelete.id) }
        }

    @Test
    fun `When the user soft-deletes another user, but they are a last project admin, then TestSpecificException is thrown`() =
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

            assertThrows<TestSpecificException> { mainService.softDeleteUser(userToDelete.id) }
        }

    @Test
    fun `When the user soft-deletes another user and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToDelete = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(userToDelete.id) } returns Result.success(userToDelete)
        coJustRun { userAccessCheckerMock.isAllowedToDeleteUser(currentUser, userToDelete) }
        coEvery { projectRepoMock.getUserProjects(userToDelete.id, any()) } returns listOf(project)
        coJustRun { userRepoMock.softDeleteUser(userToDelete.id) }
        coJustRun { projectAccessCheckerMock.isNotLastProjectAdmin(userToDelete, project.id, any()) }

        assertDoesNotThrow { mainService.softDeleteUser(userToDelete.id) }
    }
}
