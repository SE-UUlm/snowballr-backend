package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.invalidargument.IncorrectOldPasswordException
import se.uulm.snowballr.backend.model.incoming.authentication.ChangePasswordRequest

class ChangePasswordTest : AuthenticationServiceTest() {
    @Test
    fun `When the current user is not active, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.DELETED)
        val request = ChangePasswordRequest("AAbb__00", "CCdd__11")

        mockCurrentUser(currentUser)

        assertThrows<FailedPreconditionException> { service.changePassword(request) }
    }

    @Test
    fun `When getPasswordHashByEmail returns a failure, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)
        val request = ChangePasswordRequest("AAbb__00", "CCdd__11")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getPasswordHashByEmail(currentUser.email) } returns
            Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.changePassword(request) }
    }

    @Test
    fun `When the provided old password is incorrect, then an InvalidOldPasswordException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)
        val request = ChangePasswordRequest("wrong-password", "CCdd__11")
        val storedPasswordHash = PasswordUtils.hashPassword("AAbb__00")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getPasswordHashByEmail(currentUser.email) } returns Result.success(storedPasswordHash)

        assertThrows<IncorrectOldPasswordException> { service.changePassword(request) }
    }

    @Test
    fun `When the request is valid, then the password hash is updated`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)
        val oldPassword = "AAbb__00"
        val newPassword = "CCdd__11"
        val request = ChangePasswordRequest("AAbb__00", "CCdd__11")
        val storedPasswordHash = PasswordUtils.hashPassword(oldPassword)

        val passwordHashSlot = slot<String>()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getPasswordHashByEmail(currentUser.email) } returns Result.success(storedPasswordHash)
        coJustRun { userRepoMock.updatePasswordHash(currentUser.id, capture(passwordHashSlot)) }

        service.changePassword(request)

        assertNotNull(passwordHashSlot.captured)
        assertTrue(PasswordUtils.verifyPassword(newPassword, passwordHashSlot.captured))
    }
}
