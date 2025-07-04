package se.uulm.snowballr.backend.auth

import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.env.EnvReader
import java.util.UUID

class JwtUtilsTest : KoinTest {
    private val envReaderMock = mockk<EnvReader>()

    @BeforeEach
    fun setUpTest() {
        // Stop any existing Koin context
        stopKoin()

        // Start Koin context with a mock module
        startKoin {
            modules(
                module {
                    single { envReaderMock }
                },
            )
        }

        // Mock JWT key pair
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
        every { envReaderMock.env.encryption.jwtPrivateKeyBase64 } returns privateKeyBase64
        every { envReaderMock.env.encryption.jwtPublicKeyBase64 } returns publicKeyBase64
    }

    @AfterEach
    fun tearDownTest() {
        stopKoin()
    }

    @Nested
    inner class GenerateTokens {
        @Test
        fun `When generating tokens for a user, then non-blank access and refresh tokens are returned`() {
            val userId = UUID.randomUUID()
            val sessionId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId, sessionId)

            assertTrue(tokens.accessToken.isNotBlank())
            assertTrue(tokens.refreshToken.isNotBlank())
        }
    }

    @Nested
    inner class ParseToken {
        @Test
        fun `When parsing an access token, then the correct information is extracted`() {
            val userId = UUID.randomUUID()
            val sessionId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId, sessionId)

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
            val sessionId = UUID.randomUUID()
            val tokens = JwtUtils.generateTokens(userId, sessionId)
            val parsedToken = JwtUtils.parseToken(tokens.accessToken)

            val newAccessToken = JwtUtils.refreshAccessToken(parsedToken)

            val parsedNewToken = JwtUtils.parseToken(newAccessToken)
            assertEquals(userId, parsedNewToken.userId)
        }
    }
}
