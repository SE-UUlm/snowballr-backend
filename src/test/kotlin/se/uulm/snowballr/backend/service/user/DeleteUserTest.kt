package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
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
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class DeleteUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { userRepoMock.softDeleteUser(any()) } throws NotImplementedError()
    }

    @Test
    fun `When parsing user ID fails, then InvalidIdException is thrown`() = testCoroutine {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } returns DataBuilder.createExampleUser()

        assertThrows<InvalidIdException> { mainService.softDeleteUser(request) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving user to delete fails, then exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When user is not admin and tries to delete another user, then UnauthorizedException is thrown`() =
        testCoroutine {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete

            assertThrows<UnauthorizedException.Single> { mainService.softDeleteUser(getExampleRequest()) }
        }

    @Test
    fun `When trying to delete another admin user, then FailedPreconditionException is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete

        assertThrows<FailedPreconditionException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When soft delete fails, then exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete
        coEvery { userRepoMock.softDeleteUser(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When soft delete succeeds, then nothing is returned`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns userToDelete
        coEvery { userRepoMock.softDeleteUser(requestedUserId) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }
}
