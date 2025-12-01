package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when an unexpected error occurs within the application. These exceptions are not
 * intended to be handled by the client and are probably caused by bugs in the application logic.
 */
open class InternalException protected constructor(
    message: String,
    cause: Throwable? = null,
) : SnowballRException(Status.INTERNAL, message, cause)
