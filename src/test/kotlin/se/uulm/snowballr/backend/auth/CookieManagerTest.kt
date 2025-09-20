package se.uulm.snowballr.backend.auth

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.auth.CookieConfig

class CookieManagerTest {
    private val jwtManagerMock = mockk<IJwtManager> {
        every { getAccessTokenTTL() } returns JwtManager.ACCESS_TOKEN_EXPIRATION_MS
        every { getRefreshTokenTTL() } returns JwtManager.REFRESH_TOKEN_EXPIRATION_MS
    }
    private val cookieManager = CookieManager(jwtManagerMock, createEnvReader("https://"))

    private fun createEnvReader(frontendBaseUrl: String): EnvReader {
        val envReaderMock = mockk<EnvReader>()
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns frontendBaseUrl

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock
        every { envReaderMock.env } returns envMock
        return envReaderMock
    }

    @Nested
    inner class ParseCookies {
        @Test
        fun `When parsing a null or blank header, then an empty map is returned`() {
            assertThat(cookieManager.parseCookies(null)).isEmpty()
            assertThat(cookieManager.parseCookies("")).isEmpty()
            assertThat(cookieManager.parseCookies("   ")).isEmpty()
        }

        @Test
        fun `When parsing a single cookie header, then the cookie is parsed correctly`() {
            val result = cookieManager.parseCookies("session=abc123")

            assertThat(result).hasSize(1)
            assertEquals("abc123", result["session"])
        }

        @Test
        fun `When parsing multiple cookies with spaces, then all cookies are parsed correctly`() {
            val result = cookieManager.parseCookies("key1=val1; key2=val2; key3=val3")

            assertThat(result).hasSize(3)
            assertEquals("val1", result["key1"])
            assertEquals("val2", result["key2"])
            assertEquals("val3", result["key3"])
        }

        @Test
        fun `When parsing cookies with duplicate keys, then the last value is taken`() {
            val result = cookieManager.parseCookies("token=abc; token=xyz")

            assertThat(result).hasSize(1)
            assertEquals("xyz", result["token"])
        }
    }

    @Nested
    inner class BuildAuthCookieString {
        @Test
        fun `When building auth cookie for access token with value, then correct cookie string is returned`() {
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.ACCESS_TOKEN_COOKIE_NAME, "token123")

            assertNotNull(cookie)
            assertThat(cookie).contains("${GrpcContext.ACCESS_TOKEN_COOKIE_NAME}=token123")
            assertThat(cookie).contains("Max-Age=${JwtManager.ACCESS_TOKEN_EXPIRATION_MS}")
            assertThat(cookie).contains("SameSite=Strict")
            assertThat(cookie).contains("HttpOnly")
            assertThat(cookie).contains("Secure")
        }

        @Test
        fun `When building auth cookie for access token with null value, then cookie expires immediately`() {
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.ACCESS_TOKEN_COOKIE_NAME, null)

            assertNotNull(cookie)
            assertThat(cookie).contains("${GrpcContext.ACCESS_TOKEN_COOKIE_NAME}=")
            assertThat(cookie).contains("Max-Age=0")
        }

        @Test
        fun `When building auth cookie for refresh token with value, then correct cookie string is returned`() {
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "refresh456")

            assertNotNull(cookie)
            assertThat(cookie).contains("${GrpcContext.REFRESH_TOKEN_COOKIE_NAME}=refresh456")
            assertThat(cookie).contains("Max-Age=${JwtManager.REFRESH_TOKEN_EXPIRATION_MS}")
            assertThat(cookie).contains("SameSite=Strict")
            assertThat(cookie).contains("HttpOnly")
            assertThat(cookie).contains("Secure")
        }

        @Test
        fun `When building auth cookie for refresh token with empty value, then cookie expires immediately`() {
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "")

            assertNotNull(cookie)
            assertThat(cookie).contains("${GrpcContext.REFRESH_TOKEN_COOKIE_NAME}=")
            assertThat(cookie).contains("Max-Age=0")
        }

        @Test
        fun `When building auth cookie for unrecognized cookie name, then null is returned`() {
            val cookie = cookieManager.buildAuthCookieString("unknown_cookie", "value")

            assertNull(cookie)
        }

        @Test
        fun `When the frontend base url starts with http(colon), then secure defaults to false`() {
            val cookieManager = CookieManager(jwtManagerMock, createEnvReader("http://"))
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "value")

            assertNotNull(cookie)
            assertThat(cookie).doesNotContain("Secure")
        }

        @Test
        fun `When the frontend base url starts with https(colon), then secure defaults to true`() {
            val cookieManager = CookieManager(jwtManagerMock, createEnvReader("https://"))
            val cookie = cookieManager.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "value")

            assertNotNull(cookie)
            assertThat(cookie).contains("Secure")
        }
    }

    @Nested
    inner class CreateCookieString {
        @Test
        fun `When creating a cookie string with default config, then the expected string is generated`() {
            val cookie = cookieManager.createCookieString(CookieConfig("session", "abc123", 3600))

            assertThat(cookie).contains("session=abc123")
            assertThat(cookie).contains("Path=/")
            assertThat(cookie).contains("Max-Age=3600")
            assertThat(cookie).contains("SameSite=Lax")
            assertThat(cookie).contains("HttpOnly")
            assertThat(cookie).contains("Secure")
        }

        @Test
        fun `When creating a cookie string with a domain specified, then the domain is included`() {
            val cookie =
                cookieManager.createCookieString(CookieConfig("session", "abc123", 3600, domain = "example.com"))

            assertThat(cookie).contains("Domain=example.com")
        }

        @Test
        fun `When creating a cookie string with HttpOnly and Secure disabled, then they are omitted`() {
            val cookie =
                cookieManager.createCookieString(
                    CookieConfig(
                        "session",
                        "abc123",
                        3600,
                        httpOnly = false,
                        secure = false,
                    ),
                )

            assertThat(cookie).doesNotContain("HttpOnly")
            assertThat(cookie).doesNotContain("Secure")
        }
    }
}
