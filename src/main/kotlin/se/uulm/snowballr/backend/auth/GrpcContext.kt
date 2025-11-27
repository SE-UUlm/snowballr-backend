package se.uulm.snowballr.backend.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Context
import io.grpc.Metadata
import se.uulm.snowballr.backend.model.exception.SnowballRException.MissingContextException
import snowballr.Authentication.AuthenticationStatus
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * A container for gRPC Context keys and Metadata keys used for authentication.
 */
object GrpcContext {
    /**
     * Context key to store the authentication status of the user.
     */
    val AUTHENTICATION_STATUS: Context.Key<AuthenticationStatus> = Context.key("authenticationStatus")

    /**
     * Context key to store the authenticated user's ID.
     */
    val USER_ID_CONTEXT_KEY: Context.Key<UUID> = Context.key("userId")

    /**
     * Context key to store cookies that need to be set in the response.
     */
    val COOKIES_TO_SET_CONTEXT_KEY: Context.Key<MutableMap<String, String?>> = Context.key("cookiesToSet")

    const val ACCESS_TOKEN_COOKIE_NAME = "access_token"

    const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

    val COOKIE_METADATA_KEY: Metadata.Key<String> =
        Metadata.Key.of("cookie", Metadata.ASCII_STRING_MARSHALLER)

    val SET_COOKIE_METADATA_KEY: Metadata.Key<String> =
        Metadata.Key.of("set-cookie", Metadata.ASCII_STRING_MARSHALLER)

    /**
     * Retrieves the authentication status from the gRPC context.
     *
     * @throws MissingContextException.MissingAuthenticationStatus if the authentication status is not in the context.
     * @return The current [AuthenticationStatus] from the context.
     */
    fun getAuthenticationStatusFromContext(): AuthenticationStatus {
        val authStatus = AUTHENTICATION_STATUS.get()
        if (authStatus == null) {
            logger.warn { "Authentication status is missing from the gRPC context." }
            throw MissingContextException.MissingAuthenticationStatus()
        }
        return authStatus
    }

    /**
     * Retrieves the user ID from the gRPC context.
     *
     * @throws MissingContextException.MissingUserId if the user ID is not in the context.
     * @return The user ID as a [UUID].
     */
    fun getUserIdFromContext(): UUID {
        val userId = USER_ID_CONTEXT_KEY.get()
        if (userId == null) {
            logger.error { "User ID is missing from the gRPC context." }
            throw MissingContextException.MissingUserId()
        }

        return userId
    }

    /**
     * Signals that new authentication tokens should be set as cookies in the response.
     *
     * This is the designated way for service logic (e.g., login, register) to set
     * session cookies without needing to know about the underlying gRPC Context keys
     * or interceptor implementation.
     *
     * This function is designated to be called by service logic that needs to set
     * authentication cookies in the gRPC context, without needing to know about the
     * underlying implementation details of how cookies are managed in the context.
     *
     * It relies on the `authAndCookieInterceptor` having already initialized the
     * cookie map in the context.
     *
     * @param accessToken The new access token.
     * @param refreshToken The new refresh token.
     * @throws MissingContextException.MissingCookiesMap if the cookie context has not been initialized
     */
    fun setAuthCookiesInContext(accessToken: String, refreshToken: String) {
        val cookiesToSet = COOKIES_TO_SET_CONTEXT_KEY.get()
        if (cookiesToSet == null) {
            logger.error { "Attempted to set auth cookies, but the cookie map is missing from the context." }
            throw MissingContextException.MissingCookiesMap()
        }

        cookiesToSet[ACCESS_TOKEN_COOKIE_NAME] = accessToken
        cookiesToSet[REFRESH_TOKEN_COOKIE_NAME] = refreshToken
    }
}
