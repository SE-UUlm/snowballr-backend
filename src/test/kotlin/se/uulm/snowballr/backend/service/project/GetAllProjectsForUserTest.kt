package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
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
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving the requested user fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When all user projects are retrieved by another non-admin user, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser

        assertThrows<UnauthorizedException.Single> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving the projects of a user fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When the user projects are retrieved by an admin, then all user projects are returned successfully`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(any()) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
            coEvery { projectRepoMock.getUserProjects(any()) } returns emptyList()

            assertDoesNotThrow { mainService.getAllProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When a user retrieves its own projects, then all projects are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id)
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any()) } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjectsForUser(request) }
    }
}
