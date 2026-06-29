package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.jwt.JwtAuthTokens
import snowballr.Authentication.LoginRequest

@ExtendWith(GrpcTestContextExtension::class)
class LoginTest : AuthenticationServiceTest() {
    @Test
    fun `When a user provides an invalid email, then an UnauthenticatedException is thrown`() = runTest {
        val email = "wrongEmail"
        val request = LoginRequest.newBuilder().setEmail(email).build()

        val exception = UserNotFoundByEmailException("wrongEmail")
        coEvery { userRepoMock.getUserByEmail(email) } returns Result.failure(exception)

        assertThrows<UnauthenticatedException> { service.login(request) }
    }

    @Test
    fun `When a user with an unconfirmed email tries to log in, then an UnauthenticatedException is thrown`() =
        runTest {
            val testUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE_UNCONFIRMED)
            val request = LoginRequest.newBuilder()
                .setEmail(testUser.email)
                .setPassword("anyPassword")
                .build()

            coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)

            assertThrows<UnauthenticatedException> { service.login(request) }
        }

    @Test
    fun `When a deleted user tries to log in, then an UnauthenticatedException is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.DELETED)
        val request = LoginRequest.newBuilder()
            .setEmail(testUser.email)
            .setPassword("anyPassword")
            .build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)

        assertThrows<UnauthenticatedException> { service.login(request) }
    }

    @ParameterizedTest
    @EnumSource(UserStatus::class, names = ["ACTIVE"], mode = EnumSource.Mode.EXCLUDE)
    fun `When a user with status other than USER_STATUS_ACTIVE tries to log in, then an UnauthenticatedException is thrown`(
        status: UserStatus,
    ) = runTest {
        val testUser = DataBuilder.createExampleUser(status = status)
        val request = LoginRequest.newBuilder()
            .setEmail(testUser.email)
            .setPassword("anyPassword")
            .build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)

        assertThrows<UnauthenticatedException> { service.login(request) }
    }

    @Test
    fun `When the password hash cannot be retrieved, then an UnauthenticatedException is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)

        val request = LoginRequest.newBuilder()
            .setEmail(testUser.email)
            .setPassword("anyPassword")
            .build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)
        val exception = UserNotFoundByEmailException(testUser.email)
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns Result.failure(exception)

        assertThrows<UnauthenticatedException> { service.login(request) }
    }

    @Test
    fun `When a user provides an invalid password, then an UnauthenticatedException is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)
        val userPassword = "AAbb__00"
        val passwordHash = PasswordUtils.hashPassword(userPassword)

        val request = LoginRequest.newBuilder()
            .setEmail(testUser.email)
            .setPassword("wrongPassword")
            .build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns Result.success(passwordHash)

        assertThrows<UnauthenticatedException> { service.login(request) }
    }

    @Test
    fun `When an active user provides valid credentials, then the user is logged in successfully`(
        cookiesMap: MutableMap<String, String?>,
    ) = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.ACTIVE)
        val userPassword = "AAbb__00"
        val passwordHash = PasswordUtils.hashPassword(userPassword)
        val tokens = JwtAuthTokens("accessToken", "refreshToken")

        val request = LoginRequest.newBuilder()
            .setEmail(testUser.email)
            .setPassword(userPassword)
            .build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns Result.success(testUser)
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns Result.success(passwordHash)
        every { jwtManagerMock.generateAuthTokens(testUser.id) } returns tokens

        service.login(request)

        assertEquals(tokens.accessToken, cookiesMap[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertEquals(tokens.refreshToken, cookiesMap[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }
}
