package se.uulm.snowballr.backend.auth

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.auth.CookieConfig

class CookieUtilsTest {
    private val jwtServiceMock = mockk<IJwtService> {
        every { getAccessTokenTTL() } returns JwtService.ACCESS_TOKEN_EXPIRATION_MS
        every { getRefreshTokenTTL() } returns JwtService.REFRESH_TOKEN_EXPIRATION_MS
    }
    private val cookieUtils = CookieUtils(jwtServiceMock)

    @Nested
    inner class ParseCookies {
        @Test
        fun `When parsing a null or blank header, then an empty map is returned`() {
            assertTrue(cookieUtils.parseCookies(null).isEmpty())
            assertTrue(cookieUtils.parseCookies("").isEmpty())
            assertTrue(cookieUtils.parseCookies("   ").isEmpty())
        }

        @Test
        fun `When parsing a single cookie header, then the cookie is parsed correctly`() {
            val result = cookieUtils.parseCookies("session=abc123")
            assertEquals(1, result.size)
            assertEquals("abc123", result["session"])
        }

        @Test
        fun `When parsing multiple cookies with spaces, then all cookies are parsed correctly`() {
            val result = cookieUtils.parseCookies("key1=val1; key2=val2; key3=val3")
            assertEquals(3, result.size)
            assertEquals("val1", result["key1"])
            assertEquals("val2", result["key2"])
            assertEquals("val3", result["key3"])
        }
    }

    @Nested
    inner class CreateCookieString {
        @Test
        fun `When creating a cookie string with default config, then the expected string is generated`() {
            val cookie = cookieUtils.createCookieString(CookieConfig("session", "abc123", 3600))
            assertTrue(cookie.contains("session=abc123"))
            assertTrue(cookie.contains("Path=/"))
            assertTrue(cookie.contains("Max-Age=3600"))
            assertTrue(cookie.contains("SameSite=Lax"))
            assertTrue(cookie.contains("HttpOnly"))
            assertTrue(cookie.contains("Secure"))
        }

        @Test
        fun `When creating a cookie string with a domain specified, then the domain is included`() {
            val cookie = cookieUtils.createCookieString(CookieConfig("session", "abc123", 3600, domain = "example.com"))
            assertTrue(cookie.contains("Domain=example.com"))
        }

        @Test
        fun `When creating a cookie string with HttpOnly and Secure disabled, then they are omitted`() {
            val cookie =
                cookieUtils.createCookieString(
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
