package se.uulm.snowballr.backend.model.exception.internal.missingcontext

import se.uulm.snowballr.backend.model.exception.internal.MissingContextException

/**
 * Represents an exception that occurs when no request context is bound to the current thread or coroutine.
 */
class MissingRequestContextException : MissingContextException("Request context")
