package se.uulm.snowballr.backend.context

import kotlinx.coroutines.ThreadContextElement
import org.slf4j.MDC
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingRequestContextException
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingUserIdException
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.optionals.getOrNull

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
 *
 * As a side effect of keeping the thread-local in sync, the [requestId] and the current [userId] are
 * mirrored into the SLF4J [MDC] on every thread the request runs on, so all log lines emitted while the
 * context is active are automatically tagged for correlation (see the `logback.xml` pattern). The
 * [requestId] defaults to a freshly generated one; transport adapters may pass an ID established earlier in
 * the call chain so interceptor and service logs share it.
 *
 * The mirrored [userId] is always the *acting* (authenticated) user, which is why log messages only need to
 * name the entity being acted upon. It is absent for unauthenticated calls such as login and registration,
 * so those messages must still name the user explicitly.
 */
class RequestContext(
    userId: UUID? = null,
    authStatus: AuthenticationStatus = AuthenticationStatus.UNAUTHENTICATED,
    val requestId: String = generateRequestId(),
) : AbstractCoroutineContextElement(RequestContext), ThreadContextElement<RequestContext?> {
    @Volatile
    var userId: UUID? = userId
        internal set(value) {
            field = value
            // Only touch the MDC when this context is the one bound to the calling thread. Authentication
            // mutates the context on a transport thread where it is not yet bound; that thread must not be
            // polluted, the new value is picked up by the next updateThreadContext.
            if (threadLocal.get() === this) applyMdc()
        }

    @Volatile
    var authStatus: AuthenticationStatus = authStatus
        internal set
    private val cookiesToSet: MutableMap<String, Optional<String>> = ConcurrentHashMap()

    /**
     * Read-only view of the cookies queued to be written to the response. A `null` value signals that
     * the corresponding cookie should be expired.
     */
    val cookies: Map<String, String?>
        get() = cookiesToSet.mapValues { it.value.getOrNull() }

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
        cookiesToSet[name] = Optional.ofNullable(value)
    }

    override fun updateThreadContext(context: CoroutineContext): RequestContext? {
        val previous = threadLocal.get()
        threadLocal.set(this)
        applyMdc()
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: RequestContext?) {
        if (oldState == null) {
            threadLocal.remove()
            clearMdc()
        } else {
            threadLocal.set(oldState)
            oldState.applyMdc()
        }
    }

    /**
     * Mirrors this context's [requestId] and current [userId] into the SLF4J [MDC] for the current thread.
     */
    private fun applyMdc() {
        MDC.put(MDC_REQUEST_ID_KEY, requestId)
        userId?.let { MDC.put(MDC_USER_ID_KEY, it.toString()) } ?: MDC.remove(MDC_USER_ID_KEY)
    }

    companion object Key : CoroutineContext.Key<RequestContext> {
        private val threadLocal = ThreadLocal<RequestContext?>()

        /**
         * MDC key under which the [requestId] is exposed to the logging pattern.
         */
        const val MDC_REQUEST_ID_KEY = "requestId"

        /**
         * MDC key under which the authenticated [userId] is exposed to the logging pattern.
         */
        const val MDC_USER_ID_KEY = "userId"

        private const val REQUEST_ID_LENGTH = 8

        /**
         * Generates a short, random correlation ID for a request.
         */
        fun generateRequestId(): String = UUID.randomUUID().toString().take(REQUEST_ID_LENGTH)

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
            context.applyMdc()
            return previous
        }

        /**
         * Restores [previous] as the context bound to the current thread (or clears it when `null`).
         */
        fun unbind(previous: RequestContext? = null) {
            if (previous == null) {
                threadLocal.remove()
                clearMdc()
            } else {
                threadLocal.set(previous)
                previous.applyMdc()
            }
        }

        /**
         * Removes the request-scoped keys from the SLF4J [MDC] of the current thread. Threads are pooled and
         * reused across requests, so a stale entry would silently mis-attribute a later request's log lines.
         */
        private fun clearMdc() {
            MDC.remove(MDC_REQUEST_ID_KEY)
            MDC.remove(MDC_USER_ID_KEY)
        }

        /**
         * Runs [block] with [requestId] set in the SLF4J [MDC], restoring the previous value afterward.
         *
         * Must not become `inline`. The architecture test reads bytecode, where an inlined body is copied
         * into every caller, which would report the callers as touching the [MDC] themselves.
         */
        fun withRequestIdMdc(requestId: String, block: () -> Unit) {
            val previous = MDC.get(MDC_REQUEST_ID_KEY)
            MDC.put(MDC_REQUEST_ID_KEY, requestId)
            try {
                block()
            } finally {
                if (previous == null) MDC.remove(MDC_REQUEST_ID_KEY) else MDC.put(MDC_REQUEST_ID_KEY, previous)
            }
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
