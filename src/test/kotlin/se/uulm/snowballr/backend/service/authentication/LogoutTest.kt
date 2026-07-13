package se.uulm.snowballr.backend.service.authentication

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.auth.ACCESS_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.REFRESH_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.setAuthCookies
import se.uulm.snowballr.backend.context.RequestContext

class LogoutTest : AuthenticationServiceTest() {
    @Test
    fun `When a user logs out, then the access and refresh token are cleared`() = runTest {
        // Simulate setting cookies via login
        RequestContext.current().setAuthCookies("testAccessToken", "testRefreshToken")

        service.logout()

        val cookies = RequestContext.current().cookies
        assertEquals("", cookies[ACCESS_TOKEN_COOKIE_NAME])
        assertEquals("", cookies[REFRESH_TOKEN_COOKIE_NAME])
    }
}
