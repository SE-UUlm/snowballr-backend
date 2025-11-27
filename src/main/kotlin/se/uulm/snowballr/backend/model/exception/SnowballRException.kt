package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Base class for all exceptions in the SnowballR application.
 *
 * Used to encapsulate specific error details and provide a consistent exception structure.
 * Can be extended to create more detailed exceptions specific to various error scenarios.
 *
 * @param grpcStatus The gRPC status code that should be returned to the client when the exception is thrown.
 * @param message Detailed message describing the reason for the exception, or null if not provided.
 * @param cause The cause of the exception, which can be another exception, or null if not provided.
 */
open class SnowballRException protected constructor(
    private val grpcStatus: Status,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    fun getGrpcStatus(): Status = grpcStatus
}
