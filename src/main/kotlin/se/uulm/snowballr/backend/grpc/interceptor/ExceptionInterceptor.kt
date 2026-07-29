package se.uulm.snowballr.backend.grpc.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import se.uulm.snowballr.backend.model.exception.AlreadyExistsException
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.InternalException
import se.uulm.snowballr.backend.model.exception.InvalidArgumentException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.SnowballRException
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.UnauthorizedFetcherPathException

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
        override fun <ReqT, RespT> interceptCall(
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
    private fun handleException(throwable: Throwable): Status = when (throwable) {
        // Handle all specific application exceptions
        is SnowballRException -> getStatusForSnowballRException(throwable)

        // Handle specific unexpected exceptions
        is IllegalArgumentException -> getStatusForSpecificUnexpectedException(Status.INVALID_ARGUMENT, throwable)
        is NoSuchElementException -> getStatusForSpecificUnexpectedException(Status.NOT_FOUND, throwable)
        is IllegalStateException -> getStatusForSpecificUnexpectedException(Status.FAILED_PRECONDITION, throwable)

        // Handle the rest of unexpected exceptions
        else -> getStatusForUnexpectedException(throwable)
    }

    /**
     * The [SnowballRException]s are deliberately thrown in this application, e.g., when preconditions aren't met.
     */
    private fun getStatusForSnowballRException(exception: SnowballRException): Status {
        val status = when (exception) {
            is AlreadyExistsException -> Status.ALREADY_EXISTS
            is FailedPreconditionException -> Status.FAILED_PRECONDITION
            is InternalException -> Status.INTERNAL
            is InvalidArgumentException -> Status.INVALID_ARGUMENT
            is NotFoundException -> Status.NOT_FOUND
            is UnauthenticatedException -> Status.UNAUTHENTICATED
            is UnauthorizedException -> Status.PERMISSION_DENIED
            is UnauthorizedFetcherPathException -> Status.INTERNAL
        }.withDescription(exception.message).withCause(exception.cause)

        val logMessage = {
            "gRPC call failed due to ${exception::class.qualifiedName ?: "<unknown class>"} with status: " +
                "${status.code}. Message: ${status.description ?: "<no message>"}"
        }

        when (exception) {
            is InternalException, is UnauthorizedFetcherPathException -> logger.error(exception, logMessage)
            is UnauthenticatedException, is UnauthorizedException -> logger.warn(logMessage)
            is AlreadyExistsException,
            is FailedPreconditionException,
            is InvalidArgumentException,
            is NotFoundException,
            -> logger.debug(logMessage)
        }
        logger.trace { exception.stackTraceToString() }
        return status
    }

    /**
     * Specific unexpected exceptions point to missing exception handling in this application.
     * They are logged as warning so that they can be handled in the future.
     */
    private fun getStatusForSpecificUnexpectedException(status: Status, exception: Exception): Status {
        logger.warn(exception) {
            "gRPC call failed due to unexpected ${exception::class.simpleName ?: "<unknown class>"} " +
                "with message: ${exception.message ?: "<no message>"}."
        }
        return status.withDescription(exception.message).withCause(exception.cause)
    }

    /**
     * All other unexpected exceptions point to bugs or missing exception handling in this application.
     * Therefore, they are logged as error.
     */
    private fun getStatusForUnexpectedException(throwable: Throwable): Status {
        logger.error(throwable) { "gRPC call failed due to an unexpected exception" }
        return Status.INTERNAL.withDescription("An unexpected error occurred")
    }
}
