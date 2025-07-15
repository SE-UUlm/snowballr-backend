package se.uulm.snowballr.backend.grpc.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import se.uulm.snowballr.backend.model.SnowballRException

private val logger = KotlinLogging.logger {}

/**
 * A [ServerInterceptor] that intercepts server calls and provides
 * custom exception handling behavior. It ensures that exceptions are captured and translated
 * into appropriate gRPC status codes before being sent to the client.
 *
 * The interceptor wraps the gRPC server call in an instance of [ExceptionCall], which is
 * responsible for processing exceptions and mapping them to appropriate gRPC statuses.
 */
val exceptionInterceptor =
    object : ServerInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT?, RespT?>,
            headers: Metadata?,
            next: ServerCallHandler<ReqT?, RespT?>,
        ): ServerCall.Listener<ReqT?> = next.startCall(
            ExceptionCall(call),
            headers,
        )
    }

@Suppress("TooGenericExceptionCaught")
private class ExceptionCall<ReqT, RespT>(
    delegate: ServerCall<ReqT, RespT>,
) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun close(status: Status?, trailers: Metadata?) {
        var newStatus = status
        if (status == null) {
            logger.error { "gRPC call closed with null status." }
        } else if (!status.isOk) {
            // This block is executed when the gRPC call is being closed
            // with an error status, regardless of whether the error
            // originated from your suspend function or elsewhere.
            // When no StatusException is thrown, the status is UNKNOWN, and the cause contains the thrown exception
            // We check the cause and depending on its type return the correct status code
            val cause = status.cause
            if (cause != null) {
                newStatus = handleException(cause)
            }
        }

        super.close(newStatus, trailers)
    }

    /**
     * Returns a status for the passed [Throwable], which then can be returned to the user.
     */
    private fun handleException(e: Throwable): Status = when (e) {
        // Handle all specific application exceptions
        is SnowballRException -> getStatusForSnowballRException(e)

        // Handle specific unexpected exceptions
        is IllegalArgumentException -> getStatusForSpecificUnexpectedException(Status.INVALID_ARGUMENT, e)
        is NoSuchElementException -> getStatusForSpecificUnexpectedException(Status.NOT_FOUND, e)
        is IllegalStateException -> getStatusForSpecificUnexpectedException(Status.FAILED_PRECONDITION, e)

        // Handle the rest of unexpected exceptions
        else -> getStatusForUnexpectedException(e)
    }

    /**
     * The [SnowballRException]s are deliberately thrown in this application, e.g., when preconditions aren't met.
     */
    private fun getStatusForSnowballRException(e: SnowballRException): Status {
        val status =
            when (e) {
                is SnowballRException.NotFoundException -> Status.NOT_FOUND
                is SnowballRException.DuplicateEntityException -> Status.ALREADY_EXISTS
                is SnowballRException.EntityNotPersistedException -> Status.INTERNAL
                is SnowballRException.UnauthorizedException -> Status.PERMISSION_DENIED
                is SnowballRException.InvalidIdException -> Status.INVALID_ARGUMENT
                is SnowballRException.MissingContextException -> Status.INTERNAL
                is SnowballRException.FailedPreconditionException -> Status.FAILED_PRECONDITION
            }.withDescription(e.message).withCause(e.cause)

        logger.debug {
            "gRPC call failed due to ${e::class.qualifiedName} with status: ${status.code}." +
                " Message: ${status.description}"
        }
        logger.trace { e.stackTraceToString() }
        return status
    }

    /**
     * Specific unexpected exceptions point to missing exception handling in this application.
     * They are logged as warning so that they can be handled in the future.
     */
    private fun getStatusForSpecificUnexpectedException(status: Status, e: Exception): Status {
        logger.warn(e) { "gRPC call failed due to unexpected ${e::class.simpleName} with message: ${e.message}" }
        return status
    }

    /**
     * All other unexpected exceptions point to bugs or missing exception handling in this application.
     * Therefore, they are logged as error.
     */
    private fun getStatusForUnexpectedException(e: Throwable): Status {
        logger.error(e) { "gRPC call failed due to an unexpected exception" }
        return Status.INTERNAL.withDescription("An unexpected error occurred")
    }
}
