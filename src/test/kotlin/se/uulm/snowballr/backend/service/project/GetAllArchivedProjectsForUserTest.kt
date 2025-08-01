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

class GetAllArchivedProjectsForUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When parsing the ID fails, then an exception is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()

        assertThrows<InvalidIdException> { mainService.getAllArchivedProjectsForUser(request) }
    }

    @Test
    fun `When retrieving current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested user fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When a non-admin retrieves another user's archived projects, then an unauthorized exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser

            assertThrows<UnauthorizedException.Single> {
                mainService.getAllArchivedProjectsForUser(
                    getExampleRequest(),
                )
            }
        }

    @Test
    fun `When retrieving archived projects fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When archived projects are retrieved by an admin, then they are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(any()) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When a user retrieves its own archived projects, then they are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id)
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(request) }
    }
}
