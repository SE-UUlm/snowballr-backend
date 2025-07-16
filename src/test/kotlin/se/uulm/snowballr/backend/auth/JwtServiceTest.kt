package se.uulm.snowballr.backend.auth

import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import java.util.UUID

class JwtServiceTest : KoinTest {
    private val envReaderMock = mockk<EnvReader>()
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUpTest() {
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()

        val encryptionMock = mockk<Env.Encryption>()
        every { encryptionMock.jwtPrivateKeyBase64 } returns privateKeyBase64
        every { encryptionMock.jwtPublicKeyBase64 } returns publicKeyBase64

        val envMock = mockk<Env>()
        every { envMock.encryption } returns encryptionMock

        every { envReaderMock.env } returns envMock

        jwtService = JwtService(envReaderMock)
    }

    @Nested
    inner class GenerateTokens {
        @Test
        fun `When generating tokens for a user, then non-blank access and refresh tokens are returned`() {
            val userId = UUID.randomUUID()
            val tokens = jwtService.generateTokens(userId)

            assertTrue(tokens.accessToken.isNotBlank())
            assertTrue(tokens.refreshToken.isNotBlank())
        }
    }

    @Nested
    inner class ParseToken {
        @Test
        fun `When parsing an access token, then the correct userId and timestamps are extracted`() {
            val userId = UUID.randomUUID()
            val tokens = jwtService.generateTokens(userId)

            val parsedToken = jwtService.parseToken(tokens.accessToken)

            assertEquals(userId, parsedToken.userId)
            assertNotNull(parsedToken.issuedAt)
            assertNotNull(parsedToken.expiration)
        }

        @Test
        fun `When parsing a null token, then a JwtException is thrown`() {
            assertThrows<JwtException> {
                jwtService.parseToken(null)
            }
        }

        @Test
        fun `When parsing a malformed token, then a JwtException is thrown`() {
            val malformed = "bad.token.without.structure"

            assertThrows<JwtException> {
                jwtService.parseToken(malformed)
            }
        }
    }

    @Nested
    inner class RefreshAccessToken {
        @Test
        fun `When refreshing an access token with a valid refresh token, then a new valid access token is issued`() {
            val userId = UUID.randomUUID()
            val tokens = jwtService.generateTokens(userId)
            val parsedToken = jwtService.parseToken(tokens.accessToken)

            val newAccessToken = jwtService.refreshAccessToken(parsedToken)

            val parsedNewToken = jwtService.parseToken(newAccessToken)
            assertEquals(userId, parsedNewToken.userId)
        }
    }

    @Nested
    inner class TokenTTL {
        @Test
        fun `When getting access token TTL, then correct TTL in seconds is returned`() {
            val ttl = jwtService.getAccessTokenTTL()

            assertEquals(JwtService.ACCESS_TOKEN_EXPIRATION_MS / 1000, ttl)
        }

        @Test
        fun `When getting refresh token TTL, then correct TTL in seconds is returned`() {
            val ttl = jwtService.getRefreshTokenTTL()

            assertEquals(JwtService.REFRESH_TOKEN_EXPIRATION_MS / 1000, ttl)
        }
    }
}
