package se.uulm.snowballr.backend.grpc

import io.grpc.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.MissingContextException
import java.util.UUID

class GrpcContextTest {
    @Nested
    inner class GetUserIdFromContext {
        @Test
        fun `When userId is set in the context, then getUserIdFromContext returns the userId`() {
            val testUserId = UUID.randomUUID()
            val context = Context.current().withValue(GrpcContext.USER_ID_CONTEXT_KEY, testUserId)

            context.run {
                val result = GrpcContext.getUserIdFromContext()
                assertEquals(testUserId, result)
            }
        }

        @Test
        fun `When userId is not set in the context, then getUserIdFromContext throws MissingUserId`() {
            val context = Context.current().fork() // ensure a clean context

            context.run {
                assertThrows<MissingContextException.MissingUserId> { GrpcContext.getUserIdFromContext() }
            }
        }
    }

    @Nested
    inner class GetSessionIdFromContext {
        @Test
        fun `When sessionId is set in the context, then getSessionIdFromContext returns the sessionId`() {
            val testSessionId = UUID.randomUUID()
            val context = Context.current().withValue(GrpcContext.SESSION_ID_CONTEXT_KEY, testSessionId)

            context.run {
                val result = GrpcContext.getSessionIdFromContext()
                assertEquals(testSessionId, result)
            }
        }

        @Test
        fun `When sessionId is not set in the context, then getSessionIdFromContext throws MissingSessionId`() {
            val context = Context.current().fork() // ensure a clean context

            context.run {
                assertThrows<MissingContextException.MissingSessionId> { GrpcContext.getSessionIdFromContext() }
            }
        }
    }

    @Nested
    inner class SetAuthCookiesInContext {
        @Test
        fun `When setting auth cookies with a cookies map in context, then both access and refresh tokens are added`() {
            val cookiesMap = mutableMapOf<String, String?>()
            val context = Context.current().withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)

            val accessToken = "access-token-value"
            val refreshToken = "refresh-token-value"

            context.run {
                GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)
                assertEquals(accessToken, cookiesMap[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
                assertEquals(refreshToken, cookiesMap[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
            }
        }

        @Test
        fun `When setting auth cookies without a cookies map in context, then IllegalStateException is thrown`() {
            val context = Context.current().fork()

            context.run {
                assertThrows<MissingContextException.MissingCookiesMap> {
                    GrpcContext.setAuthCookiesInContext("access", "refresh")
                }
            }
        }
    }
}
