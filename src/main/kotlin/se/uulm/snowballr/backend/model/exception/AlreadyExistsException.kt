package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Represents an exception that occurs when something already exists.
 */
open class AlreadyExistsException protected constructor(
    message: String,
) : SnowballRException(
    Status.BAD_REQUEST,
    message,
)
