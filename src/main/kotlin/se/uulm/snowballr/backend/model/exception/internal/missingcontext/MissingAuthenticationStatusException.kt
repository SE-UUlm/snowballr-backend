package se.uulm.snowballr.backend.model.exception.internal.missingcontext

import se.uulm.snowballr.backend.model.exception.internal.MissingContextException

/**
 * Represents an exception that occurs when the authentication status is missing in the context.
 */
class MissingAuthenticationStatusException : MissingContextException("Authentication status")
