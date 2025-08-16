package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetAllProjectsForUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When parsing the ID of the requested user fails, then an exception is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()

        assertThrows<InvalidIdException> { mainService.getAllProjectsForUser(request) }

        verify(exactly = 0) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { projectRepoMock.getUserProjects(any()) }
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { projectRepoMock.getUserProjects(any()) }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUserId) }
        coVerify(exactly = 0) { projectRepoMock.getUserProjects(any()) }
    }

    @Test
    fun `When retrieving the requested user fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUserId) }
        coVerify(exactly = 0) { projectRepoMock.getUserProjects(any()) }
    }

    @Test
    fun `When all user projects are retrieved by another non-admin user, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser

        assertThrows<UnauthorizedException.Single> { mainService.getAllProjectsForUser(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUserId) }
        coVerify(exactly = 0) { projectRepoMock.getUserProjects(any()) }
    }

    @Test
    fun `When retrieving the projects of a user fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUserId) }
        coVerify(exactly = 1) { projectRepoMock.getUserProjects(requestedUserId) }
    }

    @Test
    fun `When the user projects are retrieved by an admin, then all user projects are returned successfully`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
            coEvery { projectRepoMock.getUserProjects(requestedUserId) } returns emptyList()

            assertDoesNotThrow { mainService.getAllProjectsForUser(getExampleRequest()) }

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
            coVerify(exactly = 1) { userRepoMock.getUserById(requestedUserId) }
            coVerify(exactly = 1) { projectRepoMock.getUserProjects(requestedUserId) }
        }

    @Test
    fun `When a user retrieves its own projects, then all projects are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val request = Base.Id.newBuilder().setId(currentUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getUserProjects(currentUser.id) } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjectsForUser(request) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 2) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { projectRepoMock.getUserProjects(currentUser.id) }
    }
}
