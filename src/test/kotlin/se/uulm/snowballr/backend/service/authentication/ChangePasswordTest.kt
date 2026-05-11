package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidOldPasswordException
import snowballr.Authentication
import snowballr.UserOuterClass.UserStatus

class ChangePasswordTest : AuthenticationServiceTest() {
    @Test
    fun `When the current user is not active, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_DELETED)
        val request = Authentication.PasswordChangeRequest.newBuilder()
            .setOldPassword("AAbb__00")
            .setNewPassword("CCdd__11")
            .build()

        mockCurrentUser(currentUser)

        assertThrows<FailedPreconditionException> { service.changePassword(request) }
    }

    @Test
    fun `When the provided old password is incorrect, then an InvalidOldPasswordException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
        val request = Authentication.PasswordChangeRequest.newBuilder()
            .setOldPassword("wrong-password")
            .setNewPassword("CCdd__11")
            .build()
        val storedPasswordHash = PasswordUtils.hashPassword("AAbb__00")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getPasswordHashByEmail(currentUser.email) } returns Result.success(storedPasswordHash)

        assertThrows<InvalidOldPasswordException> { service.changePassword(request) }
    }

    @Test
    fun `When the request is valid, then the password hash is updated`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
        val oldPassword = "AAbb__00"
        val newPassword = "CCdd__11"
        val request = Authentication.PasswordChangeRequest.newBuilder()
            .setOldPassword(oldPassword)
            .setNewPassword(newPassword)
            .build()
        val storedPasswordHash = PasswordUtils.hashPassword(oldPassword)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getPasswordHashByEmail(currentUser.email) } returns Result.success(storedPasswordHash)
        coJustRun { userRepoMock.updatePasswordHash(currentUser.id, any()) }

        assertDoesNotThrow { service.changePassword(request) }

        coVerify(exactly = 1) {
            userRepoMock.updatePasswordHash(currentUser.id, match { PasswordUtils.verifyPassword(newPassword, it) })
        }
    }
}
