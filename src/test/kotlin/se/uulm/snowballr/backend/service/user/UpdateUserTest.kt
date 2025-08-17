package se.uulm.snowballr.backend.service.user

import com.google.protobuf.FieldMask
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class UpdateUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private val newEmail = "newemail@example.com"

    private fun getExampleRequest(): UserOuterClass.User.Update {
        val user = UserOuterClass.User.newBuilder()
            .setId(requestedUserId.toString())
            .setEmail(newEmail)
            .setRole(UserRole.USER_ROLE_ADMIN)
            .build()

        val mask = FieldMask.newBuilder()
            .addPaths("email")
            .addPaths("role")
            .build()

        return UserOuterClass.User.Update.newBuilder()
            .setUser(user)
            .setMask(mask)
            .build()
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When requested user retrieval fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When user is not admin and tries to change another user's role, then UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser

            assertThrows<UnauthorizedException.Single> { mainService.updateUser(getExampleRequest()) }
        }

    @Test
    fun `When user is not admin and tries to update another user, then UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val otherUser = DataBuilder.createExampleUser(id = requestedUserId)
        val request = UserOuterClass.User.Update.newBuilder()
            .setUser(UserOuterClass.User.newBuilder().setId(otherUser.id.toString()).setFirstName("NewFirstName"))
            .setMask(FieldMask.newBuilder().addPaths("first_name"))
            .build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(otherUser.id) } returns otherUser

        assertThrows<UnauthorizedException.Single> { mainService.updateUser(request) }
    }

    @Test
    fun `When updating email to existing email, then DuplicateEntityException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(newEmail) } returns true

        assertThrows<DuplicateEntityException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When update fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)
        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(newEmail) } returns false
        coEvery { userRepoMock.updateUser(request) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(request) }
    }

    @Test
    fun `When user updates own email, then it succeeds`() = runTest {
        val newEmail = "new-and-shiny@example.com"
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_DEFAULT)
        val request = UserOuterClass.User.Update.newBuilder()
            .setUser(UserOuterClass.User.newBuilder().setId(currentUser.id.toString()).setEmail(newEmail))
            .setMask(FieldMask.newBuilder().addPaths("email"))
            .build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.doesUserExistByEmail(newEmail) } returns false
        coEvery { userRepoMock.updateUser(request) } returns currentUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When admin updates another user's role, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val otherUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_DEFAULT)
        val request = UserOuterClass.User.Update.newBuilder()
            .setUser(UserOuterClass.User.newBuilder().setId(otherUser.id.toString()).setRole(UserRole.USER_ROLE_ADMIN))
            .setMask(FieldMask.newBuilder().addPaths("role"))
            .build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(otherUser.id) } returns otherUser
        coEvery { userRepoMock.updateUser(request) } returns otherUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When admin updates all fields of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)
        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(newEmail) } returns false
        coEvery { userRepoMock.updateUser(request) } returns requestedUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }
}
