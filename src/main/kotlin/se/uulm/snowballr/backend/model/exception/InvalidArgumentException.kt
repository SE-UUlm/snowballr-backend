package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Represents an exception that occurs when an invalid argument is provided to a method.
 */
open class InvalidArgumentException protected constructor(
    message: String,
) : SnowballRException(
    Status.BAD_REQUEST,
    message,
)
