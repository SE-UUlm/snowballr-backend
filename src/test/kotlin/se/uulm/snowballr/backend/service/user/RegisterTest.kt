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
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.jwt.JwtTokens
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Authentication
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class RegisterTest : MainServiceTest() {
    @Test
    fun `When a user provides valid credentials, the user is registered successfully`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val testUser = DataBuilder.createExampleUser()
        val userPassword = "AAbb__00"
        val tokens = JwtTokens("accessToken", "refreshToken")

        val request = Authentication.RegisterRequest.newBuilder().apply {
            email = testUser.email
            password = userPassword
            firstName = testUser.firstName
            lastName = testUser.lastName
        }.build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns testUser
        every { jwtServiceMock.generateTokens(any()) } returns tokens

        assertDoesNotThrow { mainService.register(request) }

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
    fun `When a user already exists, then an exception is thrown`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<SnowballRException.DuplicateEntityException> { mainService.register(request) }

        assertEquals(
            null,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME],
        )
        assertEquals(
            null,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME],
        )
    }

    @Test
    fun `When an error occurs while creating a user, then an exception is thrown`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } throws Exception("Failed to create user")

        assertThrows<Exception> { mainService.register(request) }

        assertEquals(
            null,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME],
        )
        assertEquals(
            null,
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME],
        )
    }
}
