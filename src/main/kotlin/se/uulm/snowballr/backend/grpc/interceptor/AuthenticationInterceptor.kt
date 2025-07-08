package se.uulm.snowballr.backend.grpc.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Context
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.health.v1.HealthGrpc
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.jsonwebtoken.JwtException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.ICookieUtils
import se.uulm.snowballr.backend.auth.IJwtUtils
import se.uulm.snowballr.backend.model.auth.AuthRequestState
import se.uulm.snowballr.backend.model.jwt.ParsedJwtClaims
import snowballr.SnowballRGrpcKt

private val logger = KotlinLogging.logger {}

/**
 * A set of gRPC service names that are excluded from certain processing within the application.
 */
private val PUBLIC_SERVICES =
    setOf(
        HealthGrpc.SERVICE_NAME,
        ServerReflectionGrpc.SERVICE_NAME,
    )

/**
 * A set of public gRPC methods that do not require authentication.
 */
private val PUBLIC_METHODS =
    setOf(
        SnowballRGrpcKt.getAuthenticationStatusMethod.fullMethodName,
        SnowballRGrpcKt.registerMethod.fullMethodName,
        SnowballRGrpcKt.loginMethod.fullMethodName,
        SnowballRGrpcKt.requestPasswordResetMethod.fullMethodName,
        SnowballRGrpcKt.resetPasswordMethod.fullMethodName,
    )

/**
 * A [ServerInterceptor] that authenticates incoming gRPC calls using JWT tokens stored in cookies.
 *
 * This interceptor checks if the called method is public or a health check. If so, it bypasses authentication
 * and directly invokes the next handler. Otherwise, it performs authentication as follows:
 *
 * 1. Parses the `Cookie` header to extract the access and refresh tokens.
 * 2. Validates the access token:
 *    - If valid, extracts the user ID and adds it to the gRPC context before proceeding.
 *    - If invalid, attempts to refresh it using the refresh token.
 *       - If refresh succeeds, sets a new access token cookie and proceeds with the call.
 *       - If refresh fails, clears the auth cookies, and closes the call with UNAUTHENTICATED status.
 *
 * The cookies to set in the response are stored in the context and injected into the response headers by
 * [AuthForwardingCall].
 *
 * This design supports browser-based gRPC clients using cookies for authentication and avoids an extra round-trip
 * by performing pre-emptive refresh. If the refresh token is also invalid or expired, the session is terminated.
 *
 * Note: Only methods listed in PUBLIC_METHODS or starting with the health check service name bypass authentication.
 */
@Suppress("ReturnCount")
val authenticationInterceptor: ServerInterceptor =
    object : ServerInterceptor, KoinComponent {
        private val cookieUtils: ICookieUtils by inject()
        private val jwtUtils: IJwtUtils by inject()

        /**
         * Returns a no-op listener. Used when the call chain cannot proceed.
         */
        private fun <ReqT> emptyListener(): ServerCall.Listener<ReqT?> = object : ServerCall.Listener<ReqT?>() {}

        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT?, RespT?>?,
            headers: Metadata?,
            next: ServerCallHandler<ReqT?, RespT?>?,
        ): ServerCall.Listener<ReqT?>? {
            val methodName = call?.methodDescriptor?.fullMethodName
            logger.info { "Authenticating call to $methodName" }

            if (methodName == null) {
                call?.close(Status.UNAUTHENTICATED.withDescription("Method name is null"), Metadata())
                return emptyListener()
            }

            if (methodName in PUBLIC_SERVICES) {
                logger.trace { "Method $methodName is a public service, bypassing authentication." }
                return next?.startCall(call, headers)
            }

            // This map will be captured by the forwarding call's closure
            val cookiesToSet = mutableMapOf<String, String?>()
            val forwardingCall = AuthForwardingCall(call, cookieUtils, cookiesToSet)
            val contextWithCookies = Context.current().withValue(
                GrpcContext.COOKIES_TO_SET_CONTEXT_KEY,
                cookiesToSet,
            )

            return contextWithCookies.call {
                // Bypass authentication for public methods
                if (methodName in PUBLIC_METHODS) {
                    logger.trace { "Method $methodName is public, bypassing authentication." }
                    next?.startCall(forwardingCall, headers)
                } else {
                    logger.trace { "Method $methodName requires authentication." }
                    val authState = AuthRequestState(forwardingCall, headers, next)
                    handleAuthentication(authState)
                }
            }
        }

        /**
         * Handles the authentication process for a gRPC call.
         * This method checks the provided [AuthRequestState] for access and refresh tokens,
         * validates the access token, and attempts to refresh it if necessary.
         * If the access token is valid, it proceeds with the call, attaching the user ID to the
         * gRPC context.
         * If the access token is invalid and the refresh token is also invalid or expired,
         * it closes the call with an UNAUTHENTICATED status.
         *
         * @param ReqT The type of the request.
         * @param RespT The type of the response.
         * @param authState The [AuthRequestState] containing the call, headers, and next handler.
         * @return A [ServerCall.Listener] that will handle the call, or an empty listener if authentication fails.
         */
        private fun <ReqT : Any?, RespT : Any?> handleAuthentication(
            authState: AuthRequestState<ReqT, RespT>,
        ): ServerCall.Listener<ReqT?>? {
            val cookieHeader = authState.headers?.get(GrpcContext.COOKIE_METADATA_KEY)
            val cookies = cookieUtils.parseCookies(cookieHeader)
            val accessToken = cookies[GrpcContext.ACCESS_TOKEN_COOKIE_NAME]
            val refreshToken = cookies[GrpcContext.REFRESH_TOKEN_COOKIE_NAME]

            try {
                // Try access token first
                val parsedAccessToken = jwtUtils.parseToken(accessToken)
                val context = Context.current().withValue(GrpcContext.USER_ID_CONTEXT_KEY, parsedAccessToken.userId)

                // Proceed with forwarding call, no new cookies to set
                return context.call { authState.next?.startCall(authState.call, authState.headers) }
            } catch (_: JwtException) {
                val refreshResult = preEmptiveTokenRefresh(refreshToken)

                return if (refreshResult.isSuccess) {
                    // Refresh succeeded, proceed with the call. The forwarding call will send the new cookie.
                    val claims = refreshResult.getOrThrow()
                    val context = Context.current().withValue(GrpcContext.USER_ID_CONTEXT_KEY, claims.userId)

                    context.call { authState.next?.startCall(authState.call, authState.headers) }
                } else {
                    // Refresh failed, terminate the call.
                    // The forwarding call will still be used to send the cookie-clearing headers.
                    authState.call.close(Status.UNAUTHENTICATED.withDescription("Session is invalid"), Metadata())
                    emptyListener()
                }
            }
        }

        /**
         * Attempts to refresh the access token using the provided refresh token.
         * If successful, it updates the cookies to set in the context.
         * If the refresh token is invalid or expired, it clears the cookies and returns an error.
         *
         * @param refreshToken The refresh token to use for refreshing the access token.
         * @return A [Result] containing the parsed JWT claims if successful, or an error if the refresh fails.
         */
        private fun preEmptiveTokenRefresh(refreshToken: String?): Result<ParsedJwtClaims> {
            if (refreshToken == null) {
                return Result.failure(JwtException("Refresh token is missing"))
            }

            val cookiesToSet = GrpcContext.COOKIES_TO_SET_CONTEXT_KEY.get()

            return try {
                val parsedRefreshToken = jwtUtils.parseToken(refreshToken)
                val newAccessToken = jwtUtils.refreshAccessToken(parsedRefreshToken)
                cookiesToSet[GrpcContext.ACCESS_TOKEN_COOKIE_NAME] = newAccessToken
                Result.success(parsedRefreshToken)
            } catch (e: JwtException) {
                logger.debug { "Refresh token is invalid of expired. Clearing cookies." }
                cookiesToSet[GrpcContext.ACCESS_TOKEN_COOKIE_NAME] = null
                cookiesToSet[GrpcContext.REFRESH_TOKEN_COOKIE_NAME] = null
                return Result.failure(e)
            }
        }
    }

/**
 * A forwarding server call that sets authentication-related cookies in the response headers.
 *
 * This class extends [ForwardingServerCall] to intercept the `sendHeaders` method and add cookies
 * to the response headers based on the context's cookiesToSet map.
 *
 * @param ReqT The type of the request.
 * @param RespT The type of the response.
 * @param delegate The original server call to forward to.
 * @param cookieUtils The cookie utils instance.
 * @param cookiesToSet The map of cookies that should be set in the response headers.
 */
private class AuthForwardingCall<ReqT, RespT>(
    delegate: ServerCall<ReqT, RespT>?,
    private val cookieUtils: ICookieUtils,
    private val cookiesToSet: Map<String, String?>,
) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun sendHeaders(headers: Metadata) {
        if (cookiesToSet.isNotEmpty()) {
            cookiesToSet.entries
                .mapNotNull { (name, value) -> cookieUtils.buildAuthCookieString(name, value) }
                .forEach { headers.put(GrpcContext.SET_COOKIE_METADATA_KEY, it) }
        }
        super.sendHeaders(headers)
    }
}
