package se.uulm.snowballr.backend.service.user

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class LogoutTest : MainServiceTest() {
    @Test
    fun `When a user logs out, then the access and refresh token are cleared`() = testCoroutine {
        val cookiesMap = mutableMapOf<String, String?>()

        // Create a new context with cookiesMap and run code inside it
        val initialContext = io.grpc.Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)
        initialContext.attach()

        // Simulate setting cookies via login
        GrpcContext.setAuthCookiesInContext("testAccessToken", "testRefreshToken")

        assertDoesNotThrow { mainService.logout() }

        assertEquals("", GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
        assertEquals("", GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
    }
}
