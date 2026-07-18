package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Represents an exception that occurs when a call has the wrong preconditions.
 *
 * @param description The description of the failed precondition.
 */
open class FailedPreconditionException(
    description: String,
) : SnowballRException(Status.FORBIDDEN, description)
