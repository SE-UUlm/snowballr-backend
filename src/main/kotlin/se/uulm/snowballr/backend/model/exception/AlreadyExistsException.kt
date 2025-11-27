package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when something already exists.
 */
open class AlreadyExistsException protected constructor(
    message: String,
) : SnowballRException(
    Status.ALREADY_EXISTS,
    message,
)
