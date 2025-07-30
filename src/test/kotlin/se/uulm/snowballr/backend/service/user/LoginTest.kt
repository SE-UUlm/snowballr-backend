package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthenticatedException
import se.uulm.snowballr.backend.model.jwt.JwtAuthTokens
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Authentication.LoginRequest
import snowballr.UserOuterClass.UserStatus
import kotlin.test.assertEquals

@ExtendWith(GrpcTestContextExtension::class)
class LoginTest : MainServiceTest() {
    @Test
    fun `When a user provides an invalid email, then an exception is thrown`() = runTest {
        val request = LoginRequest.newBuilder().setEmail("wrongEmail").build()

        coEvery { userRepoMock.getUserByEmail(any()) } throws SnowballRException.NotFoundException(
            EntityType.USER, "wrongEmail",
            identifierType = IdentifierType.EMAIL,
        )

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When a user with an unconfirmed email tries to log in, then an exception is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = "anyPassword"
        }.build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns testUser

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When a deleted user tries to log in, then an exception is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_DELETED)
        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = "anyPassword"
        }.build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns testUser

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When a user with an unspecified status tries to log in, then an exception is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_UNSPECIFIED)
        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = "anyPassword"
        }.build()

        coEvery { userRepoMock.getUserByEmail(testUser.email) } returns testUser

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When the password hash cannot be retrieved, then an exception is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = "anyPassword"
        }.build()

        coEvery { userRepoMock.getUserByEmail(any()) } returns testUser
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } throws SnowballRException.NotFoundException(
            EntityType.USER, testUser.email, identifierType = IdentifierType.EMAIL,
        )

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When a user provides an invalid password, then an exception is thrown`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
        val userPassword = "AAbb__00"
        val passwordHash = PasswordUtils.hashPassword(userPassword)

        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = "wrongPassword"
        }.build()

        coEvery { userRepoMock.getUserByEmail(any()) } returns testUser
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns passwordHash

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When an active user provides valid credentials, then the user is logged in successfully`() = runTest {
        val testUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
        val userPassword = "AAbb__00"
        val passwordHash = PasswordUtils.hashPassword(userPassword)
        val tokens = JwtAuthTokens("accessToken", "refreshToken")

        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = userPassword
        }.build()

        coEvery { userRepoMock.getUserByEmail(any()) } returns testUser
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns passwordHash
        every { jwtServiceMock.generateAuthTokens(testUser.id) } returns tokens

        assertDoesNotThrow { mainService.login(request) }

        assertEquals(
            tokens.accessToken,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME],
        )
        assertEquals(
            tokens.refreshToken,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME],
        )
    }
}
