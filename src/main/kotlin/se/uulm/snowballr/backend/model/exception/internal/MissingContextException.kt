package se.uulm.snowballr.backend.model.exception.internal

import se.uulm.snowballr.backend.model.exception.InternalException

/**
 * Represents an exception that occurs when expected gRPC context data is missing.
 *
 * This may indicate a misconfigured interceptor, a bug in the server flow,
 * or a misuse of context propagation.
 *
 * @param keyDescription A human-readable description of the missing key or context value.
 */
open class MissingContextException protected constructor(
    keyDescription: String,
) : InternalException("Missing context value: $keyDescription")
