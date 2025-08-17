package se.uulm.snowballr.backend.service.user

import io.mockk.coVerify
import io.mockk.verify
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

            coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
            coVerify(exactly = 0) { userRepoMock.getUserByEmail(any()) }
            verify(exactly = 0) { jwtServiceMock.generateAuthTokens(any()) }
        }
}
