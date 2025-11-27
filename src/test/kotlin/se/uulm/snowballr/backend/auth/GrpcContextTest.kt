package se.uulm.snowballr.backend.auth

import io.grpc.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.exception.MissingContextException
import snowballr.Authentication.AuthenticationStatus
import java.util.UUID

class GrpcContextTest {
    @Nested
    inner class GetAuthenticationStatusFromContext {
        @Test
        fun `When authentication status is in context, then it is returned without exception`() {
            val expectedStatus = AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED
            val context = Context.current().withValue(GrpcContext.AUTHENTICATION_STATUS, expectedStatus)

            withContext(context) {
                val actual = GrpcContext.getAuthenticationStatusFromContext()
                assertEquals(expectedStatus, actual)
            }
        }

        @Test
        fun `When authentication status is missing from context, then MissingAuthenticationStatus exception is thrown`() {
            val context = Context.current()

            withContext(context) {
                assertThrows<MissingContextException.MissingAuthenticationStatus> {
                    GrpcContext.getAuthenticationStatusFromContext()
                }
            }
        }
    }

    @Nested
    inner class GetUserIdFromContext {
        @Test
        fun `When user ID is in context, then it is returned without exception`() {
            val expectedId = UUID.randomUUID()
            val context = Context.current().withValue(GrpcContext.USER_ID_CONTEXT_KEY, expectedId)

            withContext(context) {
                val actual = GrpcContext.getUserIdFromContext()
                assertEquals(expectedId, actual)
            }
        }

        @Test
        fun `When user ID is missing from context, then MissingUserId exception is thrown`() {
            val context = Context.current()

            withContext(context) {
                assertThrows<MissingContextException.MissingUserId> {
                    GrpcContext.getUserIdFromContext()
                }
            }
        }
    }

    @Nested
    inner class SetAuthCookiesInContext {
        @Test
        fun `When cookies map is in context, then setAuthCookiesInContext stores both tokens`() {
            val cookiesMap = mutableMapOf<String, String?>()
            val context = Context.current().withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)

            withContext(context) {
                val accessToken = "access"
                val refreshToken = "refresh"

                GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)

                assertEquals(accessToken, cookiesMap[GrpcContext.ACCESS_TOKEN_COOKIE_NAME])
                assertEquals(refreshToken, cookiesMap[GrpcContext.REFRESH_TOKEN_COOKIE_NAME])
            }
        }

        @Test
        fun `When cookies map is missing from context, then MissingCookiesMap exception is thrown`() {
            val context = Context.current()

            withContext(context) {
                assertThrows<MissingContextException.MissingCookiesMap> {
                    GrpcContext.setAuthCookiesInContext("access", "refresh")
                }
            }
        }
    }

    @Nested
    inner class MetadataKeysTests {
        @Test
        fun `COOKIE_METADATA_KEY has name 'cookie'`() {
            assertEquals("cookie", GrpcContext.COOKIE_METADATA_KEY.name())
        }

        @Test
        fun `SET_COOKIE_METADATA_KEY has name 'set-cookie'`() {
            assertEquals("set-cookie", GrpcContext.SET_COOKIE_METADATA_KEY.name())
        }
    }

    /**
     * Helper function to execute a block of code within a specific gRPC context.
     *
     * This function attaches the provided context, executes the block,
     * and ensures that the context is detached afterward.
     *
     * @param T The type of the result returned by the block.
     * @param context The gRPC context to attach.
     * @param block The block of code to execute within the context.
     * @return The result of the block execution.
     */
    private inline fun <T> withContext(context: Context, block: () -> T): T {
        val previous = context.attach()
        return try {
            block()
        } finally {
            Context.current().detach(previous)
        }
    }
}
