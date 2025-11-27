package se.uulm.snowballr.backend.model.exception.internal.missingcontext

import se.uulm.snowballr.backend.model.exception.internal.MissingContextException

/**
 * Represents an exception that occurs when the user ID is missing in the context.
 */
class MissingUserIdException : MissingContextException("Authenticated user ID")
