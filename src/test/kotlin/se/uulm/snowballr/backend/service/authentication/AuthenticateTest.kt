package se.uulm.snowballr.backend.service.authentication

import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.JwtService
import se.uulm.snowballr.backend.model.jwt.ParsedJwtClaims
import se.uulm.snowballr.backend.service.AuthenticationService
import snowballr.Authentication
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(GrpcTestContextExtension::class)
class AuthenticateTest {
    private val jwtServiceMock = mockk<JwtService>()
    private lateinit var authenticationService: AuthenticationService

    @BeforeAll
    fun setUpTest() {
        every { jwtServiceMock.getAccessTokenTTL() } returns JwtService.ACCESS_TOKEN_EXPIRATION_MS
        every { jwtServiceMock.getRefreshTokenTTL() } returns JwtService.REFRESH_TOKEN_EXPIRATION_MS

        authenticationService = AuthenticationService(jwtServiceMock)
    }

    @Test
    fun `When access token is valid, then authentication succeeds`() {
        val parsedClaims = ParsedJwtClaims(UUID.randomUUID(), Date(), Date())
        every { jwtServiceMock.parseToken("validAccessToken") } returns parsedClaims

        val authResult = authenticationService.authenticate("validAccessToken", "anyRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtClaimsResult.isSuccess)
        assertEquals(parsedClaims, authResult.parsedJwtClaimsResult.getOrNull())
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
    }

    @Test
    fun `When access token is invalid but refresh token is valid, then token is not refreshed and authentication succeeds`() {
        val parsedRefreshClaims = ParsedJwtClaims(UUID.randomUUID(), Date(), Date())
        every { jwtServiceMock.parseToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtServiceMock.parseToken("validRefreshToken") } returns parsedRefreshClaims
        every { jwtServiceMock.refreshAccessToken(parsedRefreshClaims) } returns "newAccessToken"

        val authResult = authenticationService.authenticate("invalidAccessToken", "validRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtClaimsResult.isSuccess)
        assertEquals(parsedRefreshClaims, authResult.parsedJwtClaimsResult.getOrNull())
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_ACCESS_TOKEN_EXPIRED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
    }

    @Test
    fun `When both access and refresh tokens are invalid, then authentication fails and cookies are cleared`() {
        every { jwtServiceMock.parseToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtServiceMock.parseToken("invalidRefreshToken") } throws JwtException("Invalid refresh token")

        val authResult = authenticationService.authenticate("invalidAccessToken", "invalidRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtClaimsResult.isFailure)
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When access token is invalid and refresh token is missing, then authentication fails`() {
        every { jwtServiceMock.parseToken("invalidAccessToken") } throws JwtException("Invalid access token")

        val authResult = authenticationService.authenticate("invalidAccessToken", null, false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtClaimsResult.isFailure)
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }
}
