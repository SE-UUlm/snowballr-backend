package se.uulm.snowballr.backend.model.auth

import io.grpc.Context
import se.uulm.snowballr.backend.model.jwt.ParsedJwtClaims

/**
 * Represents the result of an authentication attempt.
 *
 * @property parsedJwtClaimsResult The result of the authentication attempt, which can be either a success with parsed JWT claims or a failure.
 * @property updatedContext The gRPC context updated with the authentication status.
 */
data class AuthenticationResult(
    val parsedJwtClaimsResult: Result<ParsedJwtClaims>,
    val updatedContext: Context,
)
