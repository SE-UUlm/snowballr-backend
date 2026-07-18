package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Base class for all exceptions in the SnowballR application.
 *
 * Used to encapsulate specific error details and provide a consistent exception structure.
 * Can be extended to create more detailed exceptions specific to various error scenarios.
 *
 * @param status The status code that should be returned to the client when the exception is thrown.
 * @param message Detailed message describing the reason for the exception.
 * @param cause The cause of the exception, which can be another exception, or null if not provided.
 */
sealed class SnowballRException(
    private val status: Status,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    fun getStatus(): Status = status
}
