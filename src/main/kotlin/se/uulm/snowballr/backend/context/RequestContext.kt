package se.uulm.snowballr.backend.context

import kotlinx.coroutines.ThreadContextElement
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingRequestContextException
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingUserIdException
import snowballr.Authentication.AuthenticationStatus
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Transport-agnostic, request-scoped context carrying the authenticated identity, the authentication
 * status, and the cookies to be written to the response.
 *
 * This replaces direct usage of [io.grpc.Context] in the service and authentication layers so that the
 * same business logic can be driven by any transport (gRPC, REST, ...). A transport adapter is
 * responsible for creating a [RequestContext], populating it, and making it available to the request:
 *
 * - For coroutine-based flows, add the [RequestContext] to the coroutine context. As a
 *   [ThreadContextElement] it keeps a thread-local in sync across dispatcher hops, so
 *   [current] works inside `suspend` code without it being aware of this mechanism.
 * - For blocking flows (or tests), use [with] (or [bind]/[unbind]) to bind it to the current thread.
 *
 * Reads happen through [current]/[currentOrNull].
 */
class RequestContext(
    var userId: UUID? = null,
    var authStatus: AuthenticationStatus = AuthenticationStatus.AUTHENTICATION_STATUS_UNSPECIFIED,
) : AbstractCoroutineContextElement(RequestContext), ThreadContextElement<RequestContext?> {
    private val cookiesToSet: MutableMap<String, String?> = mutableMapOf()

    /**
     * Read-only view of the cookies queued to be written to the response. A `null` value signals that
     * the corresponding cookie should be expired.
     */
    val cookies: Map<String, String?>
        get() = cookiesToSet

    /**
     * Returns the authenticated user's ID.
     *
     * @throws MissingUserIdException if no authenticated user is associated with this context.
     */
    fun requireUserId(): UUID = userId ?: throw MissingUserIdException()

    /**
     * Queues a cookie to be set on the response.
     *
     * @param name The cookie name.
     * @param value The cookie value, or `null`/empty to signal that the cookie should be expired.
     */
    fun queueCookie(name: String, value: String?) {
        cookiesToSet[name] = value
    }

    override fun updateThreadContext(context: CoroutineContext): RequestContext? {
        val previous = threadLocal.get()
        threadLocal.set(this)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: RequestContext?) {
        threadLocal.set(oldState)
    }

    companion object Key : CoroutineContext.Key<RequestContext> {
        private val threadLocal = ThreadLocal<RequestContext?>()

        /**
         * Returns the [RequestContext] bound to the current thread, or `null` if none is bound.
         */
        fun currentOrNull(): RequestContext? = threadLocal.get()

        /**
         * Returns the [RequestContext] bound to the current thread.
         *
         * @throws MissingRequestContextException if no context is bound (e.g. called outside a request).
         */
        fun current(): RequestContext = currentOrNull() ?: throw MissingRequestContextException()

        /**
         * Binds [context] to the current thread, returning the previously bound context (if any) so it
         * can be restored. Prefer [with] for scoped usage.
         */
        fun bind(context: RequestContext): RequestContext? {
            val previous = threadLocal.get()
            threadLocal.set(context)
            return previous
        }

        /**
         * Restores [previous] as the context bound to the current thread (or clears it when `null`).
         */
        fun unbind(previous: RequestContext? = null) {
            threadLocal.set(previous)
        }

        /**
         * Binds [context] to the current thread for the duration of [block], then restores the previous
         * binding. Intended for blocking transport adapters and tests that drive the flow synchronously.
         */
        fun <T> with(context: RequestContext, block: () -> T): T {
            val previous = bind(context)
            return try {
                block()
            } finally {
                unbind(previous)
            }
        }
    }
}
