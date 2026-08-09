package se.uulm.snowballr.backend.context

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.auth.ACCESS_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.REFRESH_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.setAuthCookies
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingRequestContextException
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingUserIdException
import java.util.UUID

class RequestContextTest {
    @AfterEach
    fun tearDown() {
        RequestContext.unbind()
    }

    @Nested
    inner class Current {
        @Test
        fun `When a context is bound, then current returns it`() {
            val context = RequestContext()
            RequestContext.with(context) {
                assertEquals(context, RequestContext.current())
            }
        }

        @Test
        fun `When no context is bound, then current throws MissingRequestContext exception`() {
            assertThrows<MissingRequestContextException> { RequestContext.current() }
        }

        @Test
        fun `When no context is bound, then currentOrNull returns null`() {
            assertNull(RequestContext.currentOrNull())
        }

        @Test
        fun `When the with block completes, then the previous binding is restored`() {
            assertNull(RequestContext.currentOrNull())
            RequestContext.with(RequestContext()) { /* no-op */ }
            assertNull(RequestContext.currentOrNull())
        }
    }

    @Nested
    inner class RequireUserId {
        @Test
        fun `When a user ID is present, then it is returned`() {
            val expectedId = UUID.randomUUID()
            assertEquals(expectedId, RequestContext(userId = expectedId).requireUserId())
        }

        @Test
        fun `When the user ID is missing, then a MissingUserId exception is thrown`() {
            assertThrows<MissingUserIdException> { RequestContext().requireUserId() }
        }
    }

    @Nested
    inner class AuthStatus {
        @Test
        fun `When no status is set, then it defaults to UNAUTHENTICATED`() {
            assertEquals(AuthenticationStatus.UNAUTHENTICATED, RequestContext().authStatus)
        }

        @Test
        fun `When a status is set, then it is returned`() {
            val context = RequestContext(authStatus = AuthenticationStatus.AUTHENTICATED)
            assertEquals(AuthenticationStatus.AUTHENTICATED, context.authStatus)
        }
    }

    @Nested
    inner class Cookies {
        @Test
        fun `When a cookie is queued, then it appears in the cookies view`() {
            val context = RequestContext()
            context.queueCookie("theme", "dark")
            assertEquals("dark", context.cookies["theme"])
        }

        @Test
        fun `When auth cookies are set, then both tokens are queued`() {
            val context = RequestContext()
            context.setAuthCookies("access", "refresh")
            assertEquals("access", context.cookies[ACCESS_TOKEN_COOKIE_NAME])
            assertEquals("refresh", context.cookies[REFRESH_TOKEN_COOKIE_NAME])
        }
    }

    @Nested
    inner class CoroutineElement {
        @Test
        fun `When added to the coroutine context, then current resolves it inside the coroutine`() = runTest {
            val context = RequestContext(userId = UUID.randomUUID())
            withContext(context) {
                assertEquals(context, RequestContext.current())
            }
        }
    }
}
