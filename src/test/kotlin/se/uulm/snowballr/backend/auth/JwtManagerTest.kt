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

class JwtManagerTest : KoinTest {
    private val envReaderMock = mockk<EnvReader>()
    private lateinit var jwtManager: IJwtManager

    @BeforeEach
    fun setUpTest() {
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()

        val encryptionMock = mockk<Env.Encryption>()
        every { encryptionMock.jwtPrivateKeyBase64 } returns privateKeyBase64
        every { encryptionMock.jwtPublicKeyBase64 } returns publicKeyBase64

        val envMock = mockk<Env>()
        every { envMock.encryption } returns encryptionMock

        every { envReaderMock.env } returns envMock

        jwtManager = JwtManager(envReaderMock)
    }

    @Nested
    inner class GenerateAuthTokens {
        @Test
        fun `When generating auth tokens for a user, then non-blank access and refresh tokens are returned`() {
            val userId = UUID.randomUUID()
            val tokens = jwtManager.generateAuthTokens(userId)

            assertTrue(tokens.accessToken.isNotBlank())
            assertTrue(tokens.refreshToken.isNotBlank())
        }
    }

    @Nested
    inner class ParseAuthToken {
        @Test
        fun `When parsing a valid access token, then the correct userId and timestamps are extracted`() {
            val userId = UUID.randomUUID()
            val tokens = jwtManager.generateAuthTokens(userId)

            val parsedToken = jwtManager.parseAuthToken(tokens.accessToken)

            assertEquals(userId, parsedToken.userId)
            assertNotNull(parsedToken.issuedAt)
            assertNotNull(parsedToken.expiration)
        }

        @Test
        fun `When parsing a null token, then a JwtException is thrown`() {
            assertThrows<JwtException> {
                jwtManager.parseAuthToken(null)
            }
        }

        @Test
        fun `When parsing a malformed token, then a JwtException is thrown`() {
            val malformed = "bad.token.without.structure"

            assertThrows<JwtException> {
                jwtManager.parseAuthToken(malformed)
            }
        }
    }

    @Nested
    inner class RefreshAccessToken {
        @Test
        fun `When refreshing an access token with valid refresh token claims, then a new valid access token is issued`() {
            val userId = UUID.randomUUID()
            val tokens = jwtManager.generateAuthTokens(userId)
            val parsedRefreshToken = jwtManager.parseAuthToken(tokens.refreshToken)

            val newAccessToken = jwtManager.refreshAccessToken(parsedRefreshToken)
            assertNotNull(newAccessToken)
            assertTrue(newAccessToken.isNotBlank())

            val parsedNewToken = jwtManager.parseAuthToken(newAccessToken)
            assertEquals(userId, parsedNewToken.userId)
        }
    }

    @Nested
    inner class TokenTTL {
        @Test
        fun `When getting access token TTL, then correct TTL in seconds is returned`() {
            val ttl = jwtManager.getAccessTokenTTL()

            assertEquals(JwtManager.ACCESS_TOKEN_EXPIRATION_MS / 1000, ttl)
        }

        @Test
        fun `When getting refresh token TTL, then correct TTL in seconds is returned`() {
            val ttl = jwtManager.getRefreshTokenTTL()

            assertEquals(JwtManager.REFRESH_TOKEN_EXPIRATION_MS / 1000, ttl)
        }
    }
}
