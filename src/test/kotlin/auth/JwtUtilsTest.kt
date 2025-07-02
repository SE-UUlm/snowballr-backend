package se.uulm.snowballr.backend.auth

import io.jsonwebtoken.JwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class JwtUtilsTest {
    @Nested
    inner class GenerateTokens {
        @Test
        fun `When generating tokens for a user, then non-blank access and refresh tokens are returned`() {
            val userId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId)

            assertTrue(tokens.accessToken.isNotBlank())
            assertTrue(tokens.refreshToken.isNotBlank())
        }
    }

    @Nested
    inner class ParseToken {
        @Test
        fun `When parsing an access token, then the correct userId and timestamps are extracted`() {
            val userId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId)

            val parsedToken = JwtUtils.parseToken(tokens.accessToken)

            assertEquals(userId, parsedToken.userId)
            assertNotNull(parsedToken.issuedAt)
            assertNotNull(parsedToken.expiration)
        }

        @Test
        fun `When parsing a null token, then a JwtException is thrown`() {
            assertThrows<JwtException> {
                JwtUtils.parseToken(null)
            }
        }

        @Test
        fun `When parsing a malformed token, then a JwtException is thrown`() {
            val malformed = "bad.token.without.structure"
            assertThrows<JwtException> {
                JwtUtils.parseToken(malformed)
            }
        }
    }

    @Nested
    inner class RefreshAccessToken {
        @Test
        fun `When refreshing an access token with a valid refresh token, then a new valid access token is issued`() {
            val userId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId)
            val parsedToken = JwtUtils.parseToken(tokens.accessToken)

            val newAccessToken = JwtUtils.refreshAccessToken(parsedToken)

            val parsedNewToken = JwtUtils.parseToken(newAccessToken)
            assertEquals(userId, parsedNewToken.userId)
        }
    }
}
