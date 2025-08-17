package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class SoftDeleteUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When parsing user ID fails, then InvalidIdException is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        assertThrows<InvalidIdException> { mainService.softDeleteUser(request) }
    }

    @Test
    fun `When retrieving user to delete fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When user is not admin and tries to delete another user, then UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete

        assertThrows<UnauthorizedException.Single> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When trying to delete another admin user, then FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete

        assertThrows<FailedPreconditionException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When soft delete fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete
        coEvery { userRepoMock.softDeleteUser(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When admin soft deletes other user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete
        coEvery { userRepoMock.softDeleteUser(requestedUserId) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When user tries to delete own account, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_DEFAULT)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.softDeleteUser(currentUser.id) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When admin tries to delete own account, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.softDeleteUser(currentUser.id) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }
}
