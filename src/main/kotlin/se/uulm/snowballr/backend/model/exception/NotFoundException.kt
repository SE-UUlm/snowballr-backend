package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Represents an exception that occurs when something could not be found.
 */
open class NotFoundException protected constructor(message: String) : SnowballRException(Status.NOT_FOUND, message)
