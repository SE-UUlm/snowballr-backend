package se.uulm.snowballr.backend.model.exception.internal.missingcontext

import se.uulm.snowballr.backend.model.exception.internal.MissingContextException

/**
 * Represents an exception that occurs when the cookie map is missing in the context.
 */
class MissingCookiesMapException : MissingContextException("Cookie map")
