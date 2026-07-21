package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.Status

/**
 * Represents an exception that occurs when a user is not authenticated.
 *
 * This exception is thrown when an operation requires user authentication, but the user is not authenticated.
 */
class UnauthenticatedException : SnowballRException(Status.UNAUTHORIZED, "User is not authenticated.")
