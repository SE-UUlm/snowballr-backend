package se.uulm.snowballr.backend.service.authentication

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import se.uulm.snowballr.backend.GrpcTestContextExtension
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import kotlin.test.assertEquals

@ExtendWith(GrpcTestContextExtension::class)
class LogoutTest : MainServiceTest() {
    @Test
    fun `When a user logs out, then the access and refresh token are cleared`(cookiesMap: MutableMap<String, String>) =
        runTest {
            // Simulate setting cookies via login
            GrpcContext.setAuthCookiesInContext("testAccessToken", "testRefreshToken")

            assertDoesNotThrow { mainService.logout() }

            assertEquals("", cookiesMap[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
            assertEquals("", cookiesMap[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
        }
}
