package se.uulm.snowballr.backend.service.user

import io.grpc.Context
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthenticatedException
import se.uulm.snowballr.backend.model.jwt.JwtTokens
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Authentication.LoginRequest
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class LoginTest : MainServiceTest() {
    @Test
    fun `When a user provides valid credentials, then the user is logged in successfully`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val testUser = DataBuilder.createExampleUser()
        val userPassword = "AAbb__00"
        val passwordHash = PasswordUtils.hashPassword(userPassword)
        val tokens = JwtTokens("accessToken", "refreshToken")

        val request = LoginRequest.newBuilder().apply {
            email = testUser.email
            password = userPassword
        }.build()

        coEvery { userRepoMock.getUserByEmail(any()) } returns testUser
        coEvery { userRepoMock.getPasswordHashByEmail(testUser.email) } returns passwordHash
        every { jwtServiceMock.generateTokens(testUser.id) } returns tokens

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

    @Test
    fun `When a user provides an invalid email, then an exception is thrown`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val request = LoginRequest.newBuilder().setEmail("wrongEmail").build()

        coEvery { userRepoMock.getUserByEmail(any()) } throws SnowballRException.NotFoundException(
            EntityType.USER, "wrongEmail",
            identifierType = IdentifierType.EMAIL,
        )

        assertThrows<UnauthenticatedException> { mainService.login(request) }
    }

    @Test
    fun `When the password hash cannot be retrieved, then an exception is thrown`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val testUser = DataBuilder.createExampleUser()

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
    fun `When a user provides an invalid password, then an exception is thrown`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val testUser = DataBuilder.createExampleUser()
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
}
