package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents a specific type of exception that occurs when a call has the wrong preconditions.
 *
 * @constructor Creates a [FailedPreconditionException] with the description of the failed precondition.
 * @param description The description of the failed precondition.
 */
open class FailedPreconditionException(
    description: String,
) : SnowballRException(Status.FAILED_PRECONDITION, description)
