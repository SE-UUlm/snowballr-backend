package se.uulm.snowballr.backend.auth

import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.model.jwt.ParsedJwtAuthClaims
import snowballr.Authentication
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(GrpcTestContextExtension::class)
class AuthenticateTest {
    private val jwtManagerMock = mockk<JwtManager> {
        every { getAccessTokenTTL() } returns JwtManager.ACCESS_TOKEN_EXPIRATION_MS
        every { getRefreshTokenTTL() } returns JwtManager.REFRESH_TOKEN_EXPIRATION_MS
    }
    private val authenticationManager = AuthenticationManager(jwtManagerMock)

    @Test
    fun `When access token is valid, then authentication succeeds`() {
        val parsedClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("validAccessToken") } returns parsedClaims

        val authResult = authenticationManager.authenticate("validAccessToken", "anyRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtAuthClaimsResult.isSuccess)
        assertEquals(parsedClaims, authResult.parsedJwtAuthClaimsResult.getOrNull())
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
    }

    @Test
    fun `When access token is invalid but refresh token is valid, then token is refreshed and authentication succeeds`() {
        val parsedRefreshClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("validRefreshToken") } returns parsedRefreshClaims
        every { jwtManagerMock.refreshAccessToken(parsedRefreshClaims) } returns "newAccessToken"

        val authResult = authenticationManager.authenticate("invalidAccessToken", "validRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtAuthClaimsResult.isSuccess)
        assertEquals(parsedRefreshClaims, authResult.parsedJwtAuthClaimsResult.getOrNull())
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_ACCESS_TOKEN_EXPIRED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertEquals(
            "newAccessToken",
            GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME],
        )
    }

    @Test
    fun `When access token is invalid, refresh token is valid, and skipRefresh is true, then auth succeeds without refresh`() {
        val parsedRefreshClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("validRefreshToken") } returns parsedRefreshClaims

        val authResult = authenticationManager.authenticate("invalidAccessToken", "validRefreshToken", true)
        authResult.updatedContext.attach()

        assertTrue(authResult.parsedJwtAuthClaimsResult.isSuccess)
        assertEquals(parsedRefreshClaims, authResult.parsedJwtAuthClaimsResult.getOrNull())
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_ACCESS_TOKEN_EXPIRED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertTrue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get().isEmpty())
    }

    @Test
    fun `When both access and refresh tokens are invalid, then authentication fails and cookies are cleared`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("invalidRefreshToken") } throws JwtException("Invalid refresh token")

        val authResult = authenticationManager.authenticate("invalidAccessToken", "invalidRefreshToken", false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtAuthClaimsResult.isFailure)
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When access token is invalid and refresh token is missing, then authentication fails`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")

        val authResult = authenticationManager.authenticate("invalidAccessToken", null, false)
        authResult.updatedContext.attach() // Simulate using the updated context in the authentication interceptor

        assertTrue(authResult.parsedJwtAuthClaimsResult.isFailure)
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertNull(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When both tokens are invalid and skipRefresh is true, then authentication fails and cookies are not cleared`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("invalidRefreshToken") } throws JwtException("Invalid refresh token")

        val authResult = authenticationManager.authenticate("invalidAccessToken", "invalidRefreshToken", true)
        authResult.updatedContext.attach()

        assertTrue(authResult.parsedJwtAuthClaimsResult.isFailure)
        assertEquals(
            Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED,
            GrpcContext.getAuthenticationStatusFromContext(),
        )
        assertTrue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get().isEmpty())
    }
}
