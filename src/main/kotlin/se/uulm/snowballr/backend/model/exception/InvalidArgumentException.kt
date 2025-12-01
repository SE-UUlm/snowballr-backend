package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when an invalid argument is provided to a method.
 */
open class InvalidArgumentException protected constructor(
    message: String,
) : SnowballRException(
    Status.INVALID_ARGUMENT,
    message,
)
