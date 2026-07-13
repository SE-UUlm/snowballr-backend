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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.auth.ACCESS_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.DummyUser
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.REFRESH_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.auth.AuthRequestState
import snowballr.Authentication.AuthenticationStatus
import snowballr.SnowballRGrpcKt
import io.grpc.reflection.v1.ServerReflectionGrpc as ServerReflectionV1Grpc
import io.grpc.reflection.v1alpha.ServerReflectionGrpc as ServerReflectionV1AlphaGrpc

private val logger = KotlinLogging.logger {}

/**
 * Metadata key for reading the inbound `Cookie` header.
 */
private val COOKIE_METADATA_KEY: Metadata.Key<String> =
    Metadata.Key.of("cookie", Metadata.ASCII_STRING_MARSHALLER)

/**
 * Metadata key for writing the outbound `Set-Cookie` header.
 */
private val SET_COOKIE_METADATA_KEY: Metadata.Key<String> =
    Metadata.Key.of("set-cookie", Metadata.ASCII_STRING_MARSHALLER)

/**
 * A set of gRPC service names that are excluded from certain processing within the application.
 */
private val PUBLIC_SERVICES =
    setOf(
        HealthGrpc.SERVICE_NAME,
        ServerReflectionV1Grpc.SERVICE_NAME,
        ServerReflectionV1AlphaGrpc.SERVICE_NAME,
    )

/**
 * A set of public gRPC methods that do not require authentication.
 *
 * **Note:**
 * [SnowballRGrpcKt.getAuthenticationStatusMethod] is intentionally excluded from this list, since it is used to
 * check the authentication status of the user. Although it does not require authentication, this method needs to be
 * processed by the authentication interceptor to determine if the user is authenticated or not.
 */
private val PUBLIC_METHODS =
    setOf(
        SnowballRGrpcKt.registerMethod.fullMethodName,
        SnowballRGrpcKt.verifyEmailMethod.fullMethodName,
        SnowballRGrpcKt.loginMethod.fullMethodName,
        SnowballRGrpcKt.requestPasswordResetMethod.fullMethodName,
        SnowballRGrpcKt.resetPasswordMethod.fullMethodName,
        SnowballRGrpcKt.acceptProjectInvitationMethod.fullMethodName,
    )

/**
 * A whitelist of gRPC methods that are allowed to be called when authentication bypass is active.
 *
 * When `AUTH_BYPASS_ENABLED` is `true`, only calls to methods listed in this set will be permitted
 * to proceed with the dummy user's context. All other calls will be rejected.
 */
private val AUTH_BYPASS_METHODS =
    setOf(SnowballRGrpcKt.getAuthenticationStatusMethod.fullMethodName)

/**
 * A [ServerInterceptor] that authenticates incoming gRPC calls using JWT tokens stored in cookies.
 *
 * This interceptor checks if the called method is public or a health check. If so, it bypasses authentication
 * and directly invokes the next handler. Otherwise, it performs authentication as follows:
 *
 * 1. Parses the `Cookie` header to extract the access and refresh tokens.
 * 2. Validates the access token:
 *    - If valid, extracts the user ID and stores it in the [RequestContext] before proceeding.
 *    - If invalid, attempts to refresh it using the refresh token.
 *       - If refresh succeeds, sets a new access token cookie and proceeds with the call.
 *       - If refresh fails, clears the auth cookies, and closes the call with UNAUTHENTICATED status.
 *
 * The per-call [RequestContext] (identity, authentication status, and cookies to set) is built here and
 * handed to the [requestContextCoroutineInterceptor] via [REQUEST_CONTEXT_KEY], which installs it into the
 * service coroutine. Cookies queued on the [RequestContext] are injected into the response headers by
 * [AuthForwardingCall].
 *
 * This design supports browser-based gRPC clients using cookies for authentication and avoids an extra round-trip
 * by performing pre-emptive refresh. If the refresh token is also invalid or expired, the session is terminated.
 *
 * Note: Only methods listed in [PUBLIC_METHODS] or [PUBLIC_SERVICES] bypass authentication.
 */
val authenticationInterceptor: ServerInterceptor =
    object : ServerInterceptor, KoinComponent {
        private val authManager: IAuthenticationManager by inject()
        private val cookieManager: ICookieManager by inject()
        private val envReader: EnvReader by inject()

        /**
         * Returns a no-op listener. Used when the call chain cannot proceed.
         */
        private fun <ReqT> emptyListener(): ServerCall.Listener<ReqT?> = object : ServerCall.Listener<ReqT?>() {}

        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT?, RespT?>?,
            headers: Metadata?,
            next: ServerCallHandler<ReqT?, RespT?>?,
        ): ServerCall.Listener<ReqT?>? {
            val serviceName = call?.run { methodDescriptor.serviceName }
            val methodName = call?.run { methodDescriptor.fullMethodName }
            logger.info { "Authenticating call to ${methodName ?: "<unknown method>"}" }

            if (methodName == null || serviceName == null) {
                call?.close(Status.UNAUTHENTICATED.withDescription("Method or service name is null"), Metadata())
                return emptyListener()
            }

            if (serviceName in PUBLIC_SERVICES) {
                logger.trace { "Service $serviceName is a public service, bypassing authentication for $methodName." }
                return next?.startCall(call, headers)
            }

            // The request context is mutated during authentication and handed to the coroutine via REQUEST_CONTEXT_KEY.
            val requestContext = RequestContext()
            val forwardingCall = AuthForwardingCall(call, cookieManager, requestContext)
            val grpcContext = Context.current().withValue(REQUEST_CONTEXT_KEY, requestContext)

            return grpcContext.call {
                when (methodName) {
                    in PUBLIC_METHODS -> {
                        logger.trace { "Method $methodName is public, bypassing authentication." }
                        next?.startCall(forwardingCall, headers)
                    }

                    SnowballRGrpcKt.getAuthenticationStatusMethod.fullMethodName -> {
                        logger.trace {
                            "Method $methodName is for checking authentication status, " +
                                "processing authentication status."
                        }
                        val authState = AuthRequestState(forwardingCall, headers, next)
                        handleAuthentication(authState, requestContext, true, methodName)
                    }

                    else -> {
                        logger.trace { "Method $methodName requires authentication." }
                        val authState = AuthRequestState(forwardingCall, headers, next)
                        handleAuthentication(authState, requestContext, false, methodName)
                    }
                }
            }
        }

        /**
         * Handles the authentication process for a gRPC call.
         *
         * This method retrieves the access and refresh tokens from the request cookies and delegates
         * authentication to the [IAuthenticationManager], which populates the [requestContext] with the
         * authentication status and any refreshed cookies. If the access token is valid or successfully
         * refreshed, it stores the user ID in the [requestContext] and proceeds with the call.
         *
         * If authentication fails (i.e., both the access and refresh tokens are invalid or expired), the
         * call is closed with an UNAUTHENTICATED status and an empty listener is returned.
         *
         * @param ReqT The type of the request.
         * @param RespT The type of the response.
         * @param authState The [AuthRequestState] containing the call, headers, and next handler.
         * @param requestContext The per-call [RequestContext] to populate.
         * @param skipRefresh If true, skips the refresh token logic and only validates the access token.
         * @param methodName The full method name being called, used to determine if the call should proceed.
         * @return A [ServerCall.Listener] that will handle the call, or an empty listener if authentication fails.
         */
        private fun <ReqT : Any?, RespT : Any?> handleAuthentication(
            authState: AuthRequestState<ReqT, RespT>,
            requestContext: RequestContext,
            skipRefresh: Boolean,
            methodName: String,
        ): ServerCall.Listener<ReqT?>? {
            val authBypassEnabled = this.envReader.env.miscellaneous.authBypassEnabled
            if (authBypassEnabled) {
                return proceedWithDummyUser(authState, requestContext)
            }

            val cookieHeader = authState.headers?.get(COOKIE_METADATA_KEY)
            val cookies = cookieManager.parseCookies(cookieHeader)
            val accessToken = cookies[ACCESS_TOKEN_COOKIE_NAME]
            val refreshToken = cookies[REFRESH_TOKEN_COOKIE_NAME]

            val claimsResult = authManager.authenticate(accessToken, refreshToken, skipRefresh, requestContext)
            return claimsResult.fold(
                onSuccess = { claims ->
                    requestContext.userId = claims.userId
                    authState.next?.startCall(authState.call, authState.headers)
                },
                onFailure = {
                    if (methodName == SnowballRGrpcKt.getAuthenticationStatusMethod.fullMethodName) {
                        authState.next?.startCall(authState.call, authState.headers)
                    } else {
                        authState.call.close(Status.UNAUTHENTICATED.withDescription("Session is invalid"), Metadata())
                        emptyListener()
                    }
                },
            )
        }

        /**
         * Proceeds with the call using a dummy user.
         * This method is used for testing purposes and allows certain methods to be called
         * without requiring a valid user ID.
         *
         * @param ReqT The type of the request.
         * @param RespT The type of the response.
         * @param authState The [AuthRequestState] containing the call, headers, and next handler.
         * @param requestContext The per-call [RequestContext] to populate with the dummy user.
         * @return A [ServerCall.Listener] that will handle the call, or an empty listener if the method is not allowed.
         */
        private fun <ReqT : Any?, RespT : Any?> proceedWithDummyUser(
            authState: AuthRequestState<ReqT, RespT>,
            requestContext: RequestContext,
        ): ServerCall.Listener<ReqT?>? {
            val methodName = authState.call.methodDescriptor.fullMethodName
            if (methodName !in AUTH_BYPASS_METHODS) {
                logger.warn { "Dummy user is not allowed to call $methodName" }
                authState.call.close(
                    Status.PERMISSION_DENIED.withDescription("Dummy user cannot call this method"),
                    Metadata(),
                )
                return emptyListener()
            }

            requestContext.userId = DummyUser.id
            requestContext.authStatus = AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED
            return authState.next?.startCall(authState.call, authState.headers)
        }
    }

/**
 * A forwarding server call that sets authentication-related cookies in the response headers.
 *
 * This class extends [ForwardingServerCall] to intercept the `sendHeaders` method and add cookies
 * to the response headers based on the cookies queued on the [requestContext].
 *
 * @param ReqT The type of the request.
 * @param RespT The type of the response.
 * @param delegate The original server call to forward to.
 * @param cookieManager The cookie service instance.
 * @param requestContext The per-call request context holding the cookies that should be set in the response.
 */
private class AuthForwardingCall<ReqT, RespT>(
    delegate: ServerCall<ReqT, RespT>?,
    private val cookieManager: ICookieManager,
    private val requestContext: RequestContext,
) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun sendHeaders(headers: Metadata) {
        val cookiesToSet = requestContext.cookies
        if (cookiesToSet.isNotEmpty()) {
            cookiesToSet.entries
                .mapNotNull { (name, value) -> cookieManager.buildAuthCookieString(name, value) }
                .forEach { headers.put(SET_COOKIE_METADATA_KEY, it) }
        }
        super.sendHeaders(headers)
    }
}
