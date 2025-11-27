package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents a specific type of exception that occurs when a user is not authenticated.
 *
 * This exception is thrown when an operation requires user authentication,
 * but the user is not authenticated.
 *
 * @constructor Creates an [UnauthenticatedException] with a default message.
 */
class UnauthenticatedException : SnowballRException(Status.UNAUTHENTICATED, "User is not authenticated.")
