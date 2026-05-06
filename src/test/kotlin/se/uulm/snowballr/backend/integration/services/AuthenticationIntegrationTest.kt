package se.uulm.snowballr.backend.integration.services

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import snowballr.Authentication

@ExtendWith(GrpcTestContextExtension::class)
class AuthenticationIntegrationTest : IntegrationTest() {
    @Nested
    inner class Login {
        @Test
        fun `When a verified user logs in with correct credentials, then login succeeds`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "login.user@example.com"))

            assertDoesNotThrow {
                mainService.login(
                    Authentication.LoginRequest.newBuilder()
                        .setEmail(user.email)
                        .setPassword("SecureP@ssw0rd!")
                        .build(),
                )
            }
        }

        @Test
        fun `When a user logs in with the wrong password, then login fails`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "wrong.pass@example.com"))

            assertThrows<UnauthenticatedException> {
                mainService.login(
                    Authentication.LoginRequest.newBuilder()
                        .setEmail(user.email)
                        .setPassword("WrongP@ssw0rd!")
                        .build(),
                )
            }
        }

        @Test
        fun `When a login is attempted with a non-existent email, then login fails`() = runTest {
            assertThrows<UnauthenticatedException> {
                mainService.login(
                    Authentication.LoginRequest.newBuilder()
                        .setEmail("ghost@example.com")
                        .setPassword("AnyP@ssw0rd!")
                        .build(),
                )
            }
        }

        @Test
        fun `When a registered but unverified user attempts to login, then login fails`() = runTest {
            val tokenSlot = slot<String>()
            coEvery { emailManagerMock.createVerificationLink(capture(tokenSlot)) } returns "https://example.com/verify"
            coJustRun { emailManagerMock.sendVerificationEmail(any(), any()) }

            val newUser = DataBuilder.createExampleUser(email = "unverified.user@example.com")
            mainService.register(
                Authentication.RegisterRequest.newBuilder()
                    .setFirstName(newUser.firstName)
                    .setLastName(newUser.lastName)
                    .setEmail(newUser.email)
                    .setPassword("SecureP@ssw0rd!")
                    .build(),
            )

            assertThrows<UnauthenticatedException> {
                mainService.login(
                    Authentication.LoginRequest.newBuilder()
                        .setEmail(newUser.email)
                        .setPassword("SecureP@ssw0rd!")
                        .build(),
                )
            }
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `When a logged-in user logs out, then logout succeeds`() = runTest {
            val user = addUser(DataBuilder.createExampleUser(email = "logout.user@example.com"))
            mainService.login(
                Authentication.LoginRequest.newBuilder()
                    .setEmail(user.email)
                    .setPassword("SecureP@ssw0rd!")
                    .build(),
            )

            assertDoesNotThrow { mainService.logout() }
        }
    }
}
