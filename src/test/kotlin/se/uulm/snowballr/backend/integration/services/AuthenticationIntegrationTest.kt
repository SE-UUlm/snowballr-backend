package se.uulm.snowballr.backend.integration.services

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.invalidargument.IncorrectOldPasswordException
import se.uulm.snowballr.backend.model.incoming.authentication.ChangePasswordRequest
import se.uulm.snowballr.backend.model.incoming.authentication.LoginRequest
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest

class AuthenticationIntegrationTest : IntegrationTest() {
    @Nested
    inner class Login {
        @Test
        fun `When a verified user logs in with correct credentials, then login succeeds`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "login.user@example.com"))

            assertDoesNotThrow {
                authenticationService.login(LoginRequest(user.email, "SecureP@ssw0rd!"))
            }
        }

        @Test
        fun `When a user logs in with the wrong password, then login fails`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "wrong.pass@example.com"))

            assertThrows<UnauthenticatedException> {
                authenticationService.login(LoginRequest(user.email, "WrongP@ssw0rd!"))
            }
        }

        @Test
        fun `When a login is attempted with a non-existent email, then login fails`() = runTest {
            assertThrows<UnauthenticatedException> {
                authenticationService.login(LoginRequest("ghost@example.com", "AnyP@ssw0rd!"))
            }
        }

        @Test
        fun `When a registered but unverified user attempts to login, then login fails`() = runTest {
            val tokenSlot = slot<String>()
            coEvery { emailManagerMock.createVerificationLink(capture(tokenSlot)) } returns "https://example.com/verify"
            coJustRun { emailManagerMock.sendVerificationEmail(any(), any()) }

            val newUser = DataBuilder.createExampleUser(email = "unverified.user@example.com")
            userService.register(
                RegisterRequest(
                    firstName = newUser.firstName,
                    lastName = newUser.lastName,
                    email = newUser.email,
                    password = "SecureP@ssw0rd!",
                ),
            )

            assertThrows<UnauthenticatedException> {
                authenticationService.login(LoginRequest(newUser.email, "SecureP@ssw0rd!"))
            }
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `When a logged-in user logs out, then logout succeeds`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "logout.user@example.com"))
            authenticationService.login(LoginRequest(user.email, "SecureP@ssw0rd!"))

            assertDoesNotThrow { authenticationService.logout() }
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `When a user provides the correct old password, then the password is changed`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "change.password@example.com"))
            val oldPassword = "SecureP@ssw0rd!"
            val newPassword = "NewP@ssw0rd!!22"

            actAsUser(user.id) {
                assertDoesNotThrow {
                    authenticationService.changePassword(ChangePasswordRequest(oldPassword, newPassword))
                }
            }

            assertThrows<UnauthenticatedException> {
                authenticationService.login(LoginRequest(user.email, oldPassword))
            }
            assertDoesNotThrow {
                authenticationService.login(LoginRequest(user.email, newPassword))
            }
        }

        @Test
        fun `When a non-active user tries to change the password, then changing fails`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "change.password.deleted@example.com"))
            userService.softDeleteUser(user.id)

            actAsUser(user.id) {
                assertThrows<FailedPreconditionException> {
                    authenticationService.changePassword(ChangePasswordRequest("SecureP@ssw0rd!", "NewP@ssw0rd!!22"))
                }
            }
        }

        @Test
        fun `When a wrong old password is provided, then changing fails`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "change.password.wrong.old@example.com"))

            actAsUser(user.id) {
                assertThrows<IncorrectOldPasswordException> {
                    authenticationService.changePassword(ChangePasswordRequest("wrong-password", "NewP@ssw0rd!!22"))
                }
            }
        }
    }
}
