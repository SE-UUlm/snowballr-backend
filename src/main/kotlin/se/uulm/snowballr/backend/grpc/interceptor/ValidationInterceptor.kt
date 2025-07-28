package se.uulm.snowballr.backend.grpc.interceptor

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import se.uulm.snowballr.backend.validation.validateRequest

private val logger = KotlinLogging.logger {}

/**
 * A [ServerInterceptor] implementation responsible for validating incoming requests in gRPC communication.
 *
 * This interceptor wraps the original server call listener using a custom [ValidationCallListener],
 * which performs input validation on received messages. If validation fails, the listener closes
 * the call with an [Status.INVALID_ARGUMENT] status and a detailed error description.
 */
val validationInterceptor =
    object : ServerInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT?, RespT?>?,
            headers: Metadata?,
            next: ServerCallHandler<ReqT?, RespT?>?,
        ): ServerCall.Listener<ReqT?> {
            val originalListener = next?.startCall(call, headers)
            return ValidationCallListener(call, headers, originalListener)
        }
    }

private class ValidationCallListener<ReqT, RespT>(
    private val call: ServerCall<ReqT?, RespT?>?,
    @Suppress("unused") private val headers: Metadata?,
    listener: ServerCall.Listener<ReqT?>?,
) : SimpleForwardingServerCallListener<ReqT>(listener) {
    // Track whether the call was already closed and return if that's the case
    private var isCallClosedByInterceptor = false

    override fun onMessage(message: ReqT?) {
        // Perform input validation
        val result = validateRequest(message)

        // Return `INVALID_ARGUMENT` status if input validation failed
        if (result is Either.Left) {
            val reasons = result.value.toList().map { it.toString() }
            logger.debug { "Received invalid request: ${message?.javaClass ?: "<unknown class>"} - $reasons" }
            call?.close(
                Status.INVALID_ARGUMENT
                    .withDescription("Request validation failed: $reasons"),
                Metadata(),
            )
            isCallClosedByInterceptor = true
            // Stop further processing
            return
        }

        // Continue to the next interceptor or actual service method
        super.onMessage(message)
    }

    override fun onHalfClose() {
        if (isCallClosedByInterceptor) {
            return
        }
        super.onHalfClose()
    }

    override fun onCancel() {
        if (isCallClosedByInterceptor) {
            return
        }
        super.onCancel()
    }

    override fun onComplete() {
        if (isCallClosedByInterceptor) {
            return
        }
        super.onComplete()
    }
}
