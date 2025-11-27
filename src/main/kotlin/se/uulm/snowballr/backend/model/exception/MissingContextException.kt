package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when expected gRPC context data is missing.
 *
 * This may indicate a misconfigured interceptor, a bug in the server flow,
 * or a misuse of context propagation.
 *
 * @param keyDescription A human-readable description of the missing key or context value.
 */
sealed class MissingContextException(
    keyDescription: String,
) : SnowballRException(Status.INTERNAL, "Missing context value: $keyDescription") {
    /**
     * Represents a [MissingContextException] that occurs when the authentication status is missing in the context.
     */
    class MissingAuthenticationStatus : MissingContextException("Authentication status")

    /**
     * Represents a [MissingContextException] that occurs when the user ID is missing in the context.
     */
    class MissingUserId : MissingContextException("Authenticated user ID")

    /**
     * Represents a [MissingContextException] that occurs when the cookies map is missing in the context.
     */
    class MissingCookiesMap : MissingContextException("Cookie map")
}
