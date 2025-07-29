package se.uulm.snowballr.backend.service.user

import com.google.protobuf.FieldMask
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
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

    private fun getExampleRequest(): UserOuterClass.User.Update {
        val user = UserOuterClass.User.newBuilder()
            .setId(requestedUserId.toString())
            .setEmail("newemail@example.com")
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

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { userRepoMock.doesUserExistByEmail(any()) } throws NotImplementedError()
        coEvery { userRepoMock.updateUser(any()) } throws NotImplementedError()
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

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
    fun `When user is not admin and tries to change role, then UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser

        assertThrows<UnauthorizedException.Single> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When updating email to existing email, then DuplicateEntityException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(any()) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<DuplicateEntityException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When update fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(any()) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.updateUser(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When update succeeds, then updated user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(any()) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.updateUser(any()) } returns requestedUser

        assertDoesNotThrow { mainService.updateUser(getExampleRequest()) }
    }
}
