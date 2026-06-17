package se.uulm.snowballr.backend.service.authentication

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.auth.GrpcContext

@ExtendWith(GrpcTestContextExtension::class)
class LogoutTest : AuthenticationServiceTest() {
    @Test
    fun `When a user logs out, then the access and refresh token are cleared`(cookiesMap: MutableMap<String, String>) =
        runTest {
            // Simulate setting cookies via login
            GrpcContext.setAuthCookiesInContext("testAccessToken", "testRefreshToken")

            service.logout()

            assertEquals("", cookiesMap[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
            assertEquals("", cookiesMap[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
        }
}
