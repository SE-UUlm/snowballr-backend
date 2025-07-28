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
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.jwt.JwtAuthTokens
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Authentication
import kotlin.test.assertEquals

@ExtendWith(GrpcTestContextExtension::class)
class RegisterTest : MainServiceTest() {
    @Test
    fun `When a user provides valid credentials, then the user is registered successfully`() = runTest {
        val testUser = DataBuilder.createExampleUser()
        val tokens = JwtAuthTokens("accessToken", "refreshToken")

        val request = Authentication.RegisterRequest.newBuilder().setPassword("AAbb__00").build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns testUser
        every { jwtServiceMock.generateAuthTokens(any()) } returns tokens

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
    fun `When a user already exists, then an exception is thrown`() = runTest {
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
    fun `When an error occurs while creating a user, then an exception is thrown`() = runTest {
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
