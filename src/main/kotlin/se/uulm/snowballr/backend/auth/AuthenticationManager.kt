package se.uulm.snowballr.backend.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.JwtException
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.jwt.ParsedJwtAuthClaims

private val logger = KotlinLogging.logger {}

/**
 * Interface for authentication operations such as validating access tokens, refreshing them,
 * and managing authentication state.
 */
fun interface IAuthenticationManager {
    /**
     * Authenticates a request using the provided tokens.
     *
     * This method attempts to parse and validate the given access token. If the access token is invalid
     * or expired, it attempts to refresh it using the provided refresh token. If both tokens are invalid
     * or the refresh fails, it returns a failure result.
     *
     * The resulting authentication status, and any refreshed or cleared cookies, are written to the
     * provided [requestContext].
     *
     * @param accessToken The access token to validate.
     * @param refreshToken The refresh token to use for refreshing the access token if it is invalid or expired.
     * @param skipRefresh If true, skips the refresh token logic and only validates the access token.
     * @param requestContext The request context to populate with the authentication status and cookies to set.
     * @return A [Result] containing the parsed JWT claims on success, or a failure otherwise.
     */
    fun authenticate(
        accessToken: String?,
        refreshToken: String?,
        skipRefresh: Boolean,
        requestContext: RequestContext,
    ): Result<ParsedJwtAuthClaims>
}

/**
 * Default implementation of [IAuthenticationManager].
 */
class AuthenticationManager(private val jwtManager: IJwtManager) : IAuthenticationManager {
    override fun authenticate(
        accessToken: String?,
        refreshToken: String?,
        skipRefresh: Boolean,
        requestContext: RequestContext,
    ): Result<ParsedJwtAuthClaims> {
        val parsedAccessTokenResult = runCatching {
            jwtManager.parseAuthToken(accessToken)
        }

        val (status, result) = if (parsedAccessTokenResult.isSuccess) {
            AuthenticationStatus.AUTHENTICATED to parsedAccessTokenResult
        } else {
            val refreshResult = attemptTokenRefresh(refreshToken, skipRefresh, requestContext)
            if (refreshResult.isSuccess) {
                AuthenticationStatus.ACCESS_TOKEN_EXPIRED to refreshResult
            } else {
                AuthenticationStatus.UNAUTHENTICATED to Result.failure(JwtException("Authentication failed"))
            }
        }

        requestContext.authStatus = status
        return result
    }

    /**
     * Attempts to refresh the access token using the provided refresh token.
     * If successful, it queues the new access token cookie on the [requestContext].
     * If the refresh token is invalid or expired, it clears the cookies and returns an error.
     *
     * @param refreshToken The refresh token to use for refreshing the access token.
     * @param skipRefresh If true, skips the refresh logic and only validates the refresh token.
     * @param requestContext The request context to queue cookie changes on.
     * @return A [Result] containing the parsed JWT claims if successful, or an error if the refresh fails.
     */
    private fun attemptTokenRefresh(
        refreshToken: String?,
        skipRefresh: Boolean,
        requestContext: RequestContext,
    ): Result<ParsedJwtAuthClaims> {
        if (refreshToken == null) {
            return Result.failure(JwtException("Refresh token is missing"))
        }

        return runCatching {
            val parsedRefreshToken = jwtManager.parseAuthToken(refreshToken)

            if (!skipRefresh) {
                val newAccessToken = jwtManager.refreshAccessToken(parsedRefreshToken)
                requestContext.queueCookie(ACCESS_TOKEN_COOKIE_NAME, newAccessToken)
            }

            parsedRefreshToken
        }.onFailure { _ ->
            if (!skipRefresh) {
                logger.debug { "Refresh token is invalid or expired. Clearing cookies." }
                requestContext.queueCookie(ACCESS_TOKEN_COOKIE_NAME, null)
                requestContext.queueCookie(REFRESH_TOKEN_COOKIE_NAME, null)
            }
        }
    }
}
