package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Context
import io.jsonwebtoken.JwtException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.model.auth.AuthenticationResult
import se.uulm.snowballr.backend.model.jwt.ParsedJwtClaims
import snowballr.Authentication.AuthenticationStatus

private val logger = KotlinLogging.logger {}

/**
 * Interface for authentication operations such as validating access tokens, refreshing them,
 * and managing authentication state.
 */
interface IAuthenticationService {
    /**
     * Authenticates a request using the provided tokens.
     *
     * This method attempts to parse and validate the given access token. If the access token is invalid
     * or expired, it attempts to refresh it using the provided refresh token. If both tokens are invalid
     * or the refresh fails, it returns a failure result.
     *
     * On success, it returns the parsed JWT claims containing the authenticated user's information and
     * sets the appropriate authentication status in the [GrpcContext].
     *
     * @param accessToken The access token to validate.
     * @param refreshToken The refresh token to use for refreshing the access token if it is invalid or expired.
     * @param skipRefresh If true, skips the refresh token logic and only validates the access token.
     * @return An [AuthenticationResult] containing the result of the authentication attempt and the updated gRPC context.
     */
    fun authenticate(accessToken: String?, refreshToken: String?, skipRefresh: Boolean): AuthenticationResult
}

/**
 * Default implementation of [IAuthenticationService].
 */
class AuthenticationService(private val jwtService: IJwtService) : IAuthenticationService {
    override fun authenticate(accessToken: String?, refreshToken: String?, skipRefresh: Boolean): AuthenticationResult {
        val contextBuilder = Context.current()
        val cookiesToSet = GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()

        val parsedAccessTokenResult = runCatching {
            jwtService.parseToken(accessToken)
        }

        val (status, result) = if (parsedAccessTokenResult.isSuccess) {
            AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED to parsedAccessTokenResult
        } else {
            val refreshResult = attemptTokenRefresh(refreshToken, skipRefresh, cookiesToSet)
            if (refreshResult.isSuccess) {
                AuthenticationStatus.AUTHENTICATION_STATUS_ACCESS_TOKEN_EXPIRED to refreshResult
            } else {
                AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED to Result
                    .failure(JwtException("Authentication failed"))
            }
        }

        val updatedContext = contextBuilder.withValue(GrpcContext.AUTHENTICATION_STATUS, status)
        return AuthenticationResult(result, updatedContext)
    }

    /**
     * Attempts to refresh the access token using the provided refresh token.
     * If successful, it updates the cookies to set in the context.
     * If the refresh token is invalid or expired, it clears the cookies and returns an error.
     *
     * @param refreshToken The refresh token to use for refreshing the access token.
     * @param skipRefresh If true, skips the refresh logic and only validates the refresh token.
     * @param cookiesToSet The map of cookies to set in the gRPC context.
     * @return A [Result] containing the parsed JWT claims if successful, or an error if the refresh fails.
     */
    private fun attemptTokenRefresh(
        refreshToken: String?,
        skipRefresh: Boolean,
        cookiesToSet: MutableMap<String, String?>,
    ): Result<ParsedJwtClaims> {
        if (refreshToken == null) {
            return Result.failure(JwtException("Refresh token is missing"))
        }

        return runCatching {
            val parsedRefreshToken = jwtService.parseToken(refreshToken)

            if (!skipRefresh) {
                val newAccessToken = jwtService.refreshAccessToken(parsedRefreshToken)
                cookiesToSet[GrpcContext.ACCESS_TOKEN_COOKIE_NAME] = newAccessToken
            }

            parsedRefreshToken
        }.onFailure { e ->
            if (!skipRefresh) {
                logger.debug { "Refresh token is invalid or expired. Clearing cookies." }
                cookiesToSet[GrpcContext.ACCESS_TOKEN_COOKIE_NAME] = null
                cookiesToSet[GrpcContext.REFRESH_TOKEN_COOKIE_NAME] = null
            }
        }
    }
}
