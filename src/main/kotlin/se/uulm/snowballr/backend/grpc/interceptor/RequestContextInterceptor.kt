package se.uulm.snowballr.backend.grpc.interceptor

import io.grpc.Context
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.kotlin.CoroutineContextServerInterceptor
import se.uulm.snowballr.backend.context.RequestContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * gRPC [Context] key used to hand the per-call [RequestContext] built by the [authenticationInterceptor]
 * over to the [requestContextCoroutineInterceptor].
 *
 * This is the single place where the transport-internal [io.grpc.Context] still carries the request
 * context; it is consumed solely within the gRPC interceptor layer and never read by service/auth code.
 */
internal val REQUEST_CONTEXT_KEY: Context.Key<RequestContext> = Context.key("requestContext")

/**
 * A [CoroutineContextServerInterceptor] that installs the per-call [RequestContext] (populated by the
 * [authenticationInterceptor]) into the coroutine context of the gRPC service method.
 *
 * grpc-kotlin composes the RPC coroutine context from the implementation base context, the value
 * provided here, and the captured [io.grpc.Context]. Installing the [RequestContext] (a
 * [kotlinx.coroutines.ThreadContextElement]) here lets service and auth code read it through
 * [RequestContext.current] without depending on [io.grpc.Context].
 *
 * For calls that bypass authentication (e.g. health and reflection services) no [RequestContext] is
 * attached and [EmptyCoroutineContext] is returned.
 */
val requestContextCoroutineInterceptor: CoroutineContextServerInterceptor =
    object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext =
            REQUEST_CONTEXT_KEY.get() ?: EmptyCoroutineContext
    }
