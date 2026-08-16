package se.uulm.snowballr.backend.auth

import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.jwt.ParsedJwtAuthClaims
import java.util.Date
import java.util.UUID

class AuthenticateTest {
    private val jwtManagerMock = mockk<JwtManager>()
    private val authenticationManager = AuthenticationManager(jwtManagerMock)

    @Test
    fun `When access token is valid, then authentication succeeds`() {
        val parsedClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("validAccessToken") } returns parsedClaims
        val requestContext = RequestContext()

        val result = authenticationManager.authenticate("validAccessToken", "anyRefreshToken", false, requestContext)

        assertTrue(result.isSuccess)
        assertEquals(parsedClaims, result.getOrNull())
        assertEquals(AuthenticationStatus.AUTHENTICATED, requestContext.authStatus)
    }

    @Test
    fun `When access token is invalid but refresh token is valid, then token is refreshed and authentication succeeds`() {
        val parsedRefreshClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("validRefreshToken") } returns parsedRefreshClaims
        every { jwtManagerMock.refreshAccessToken(parsedRefreshClaims) } returns "newAccessToken"
        val requestContext = RequestContext()

        val result = authenticationManager.authenticate(
            "invalidAccessToken",
            "validRefreshToken",
            false,
            requestContext,
        )

        assertTrue(result.isSuccess)
        assertEquals(parsedRefreshClaims, result.getOrNull())
        assertEquals(AuthenticationStatus.ACCESS_TOKEN_EXPIRED, requestContext.authStatus)
        assertEquals("newAccessToken", requestContext.cookies[ACCESS_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When access token is invalid, refresh token is valid, and skipRefresh is true, then auth succeeds without refresh`() {
        val parsedRefreshClaims = ParsedJwtAuthClaims(UUID.randomUUID(), Date(), Date())
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("validRefreshToken") } returns parsedRefreshClaims
        val requestContext = RequestContext()

        val result = authenticationManager.authenticate("invalidAccessToken", "validRefreshToken", true, requestContext)

        assertTrue(result.isSuccess)
        assertEquals(parsedRefreshClaims, result.getOrNull())
        assertEquals(AuthenticationStatus.ACCESS_TOKEN_EXPIRED, requestContext.authStatus)
        assertThat(requestContext.cookies).isEmpty()
    }

    @Test
    fun `When both access and refresh tokens are invalid, then authentication fails and cookies are cleared`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("invalidRefreshToken") } throws JwtException("Invalid refresh token")
        val requestContext = RequestContext()

        val result =
            authenticationManager.authenticate("invalidAccessToken", "invalidRefreshToken", false, requestContext)

        assertTrue(result.isFailure)
        assertEquals(AuthenticationStatus.UNAUTHENTICATED, requestContext.authStatus)
        assertNull(requestContext.cookies[ACCESS_TOKEN_COOKIE_NAME])
        assertNull(requestContext.cookies[REFRESH_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When access token is invalid and refresh token is missing, then authentication fails`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        val requestContext = RequestContext()

        val result = authenticationManager.authenticate("invalidAccessToken", null, false, requestContext)

        assertTrue(result.isFailure)
        assertEquals(AuthenticationStatus.UNAUTHENTICATED, requestContext.authStatus)
        assertNull(requestContext.cookies[ACCESS_TOKEN_COOKIE_NAME])
        assertNull(requestContext.cookies[REFRESH_TOKEN_COOKIE_NAME])
    }

    @Test
    fun `When both tokens are invalid and skipRefresh is true, then authentication fails and cookies are not cleared`() {
        every { jwtManagerMock.parseAuthToken("invalidAccessToken") } throws JwtException("Invalid access token")
        every { jwtManagerMock.parseAuthToken("invalidRefreshToken") } throws JwtException("Invalid refresh token")
        val requestContext = RequestContext()

        val result =
            authenticationManager.authenticate("invalidAccessToken", "invalidRefreshToken", true, requestContext)

        assertTrue(result.isFailure)
        assertEquals(AuthenticationStatus.UNAUTHENTICATED, requestContext.authStatus)
        assertThat(requestContext.cookies).isEmpty()
    }
}
