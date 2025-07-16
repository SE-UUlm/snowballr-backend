package se.uulm.snowballr.backend.auth

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.model.auth.CookieConfig

class CookieServiceTest {
    private val jwtServiceMock = mockk<IJwtService> {
        every { getAccessTokenTTL() } returns JwtService.ACCESS_TOKEN_EXPIRATION_MS
        every { getRefreshTokenTTL() } returns JwtService.REFRESH_TOKEN_EXPIRATION_MS
    }
    private val cookieService = CookieService(jwtServiceMock)

    @Nested
    inner class ParseCookies {
        @Test
        fun `When parsing a null or blank header, then an empty map is returned`() {
            assertTrue(cookieService.parseCookies(null).isEmpty())
            assertTrue(cookieService.parseCookies("").isEmpty())
            assertTrue(cookieService.parseCookies("   ").isEmpty())
        }

        @Test
        fun `When parsing a single cookie header, then the cookie is parsed correctly`() {
            val result = cookieService.parseCookies("session=abc123")

            assertEquals(1, result.size)
            assertEquals("abc123", result["session"])
        }

        @Test
        fun `When parsing multiple cookies with spaces, then all cookies are parsed correctly`() {
            val result = cookieService.parseCookies("key1=val1; key2=val2; key3=val3")

            assertEquals(3, result.size)
            assertEquals("val1", result["key1"])
            assertEquals("val2", result["key2"])
            assertEquals("val3", result["key3"])
        }

        @Test
        fun `When parsing cookies with duplicate keys, then the last value is taken`() {
            val result = cookieService.parseCookies("token=abc; token=xyz")

            assertEquals(1, result.size)
            assertEquals("xyz", result["token"])
        }
    }

    @Nested
    inner class BuildAuthCookieString {
        @Test
        fun `When building auth cookie for access token with value, then correct cookie string is returned`() {
            val cookie = cookieService.buildAuthCookieString(GrpcContext.ACCESS_TOKEN_COOKIE_NAME, "token123")

            assertNotNull(cookie)
            assertTrue(cookie.contains("${GrpcContext.ACCESS_TOKEN_COOKIE_NAME}=token123"))
            assertTrue(cookie.contains("Max-Age=${JwtService.ACCESS_TOKEN_EXPIRATION_MS}"))
            assertTrue(cookie.contains("SameSite=Strict"))
            assertTrue(cookie.contains("HttpOnly"))
            assertTrue(cookie.contains("Secure"))
        }

        @Test
        fun `When building auth cookie for access token with null value, then cookie expires immediately`() {
            val cookie = cookieService.buildAuthCookieString(GrpcContext.ACCESS_TOKEN_COOKIE_NAME, null)

            assertNotNull(cookie)
            assertTrue(cookie.contains("${GrpcContext.ACCESS_TOKEN_COOKIE_NAME}="))
            assertTrue(cookie.contains("Max-Age=0"))
        }

        @Test
        fun `When building auth cookie for refresh token with value, then correct cookie string is returned`() {
            val cookie = cookieService.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "refresh456")

            assertNotNull(cookie)
            assertTrue(cookie.contains("${GrpcContext.REFRESH_TOKEN_COOKIE_NAME}=refresh456"))
            assertTrue(cookie.contains("Max-Age=${JwtService.REFRESH_TOKEN_EXPIRATION_MS}"))
            assertTrue(cookie.contains("SameSite=Strict"))
            assertTrue(cookie.contains("HttpOnly"))
            assertTrue(cookie.contains("Secure"))
        }

        @Test
        fun `When building auth cookie for refresh token with empty value, then cookie expires immediately`() {
            val cookie = cookieService.buildAuthCookieString(GrpcContext.REFRESH_TOKEN_COOKIE_NAME, "")

            assertNotNull(cookie)
            assertTrue(cookie.contains("${GrpcContext.REFRESH_TOKEN_COOKIE_NAME}="))
            assertTrue(cookie.contains("Max-Age=0"))
        }

        @Test
        fun `When building auth cookie for unrecognized cookie name, then null is returned`() {
            val cookie = cookieService.buildAuthCookieString("unknown_cookie", "value")

            assertNull(cookie)
        }
    }

    @Nested
    inner class CreateCookieString {
        @Test
        fun `When creating a cookie string with default config, then the expected string is generated`() {
            val cookie = cookieService.createCookieString(CookieConfig("session", "abc123", 3600))

            assertTrue(cookie.contains("session=abc123"))
            assertTrue(cookie.contains("Path=/"))
            assertTrue(cookie.contains("Max-Age=3600"))
            assertTrue(cookie.contains("SameSite=Lax"))
            assertTrue(cookie.contains("HttpOnly"))
            assertTrue(cookie.contains("Secure"))
        }

        @Test
        fun `When creating a cookie string with a domain specified, then the domain is included`() {
            val cookie =
                cookieService.createCookieString(CookieConfig("session", "abc123", 3600, domain = "example.com"))

            assertTrue(cookie.contains("Domain=example.com"))
        }

        @Test
        fun `When creating a cookie string with HttpOnly and Secure disabled, then they are omitted`() {
            val cookie =
                cookieService.createCookieString(
                    CookieConfig(
                        "session",
                        "abc123",
                        3600,
                        httpOnly = false,
                        secure = false,
                    ),
                )

            assertFalse(cookie.contains("HttpOnly"))
            assertFalse(cookie.contains("Secure"))
        }
    }
}
