package se.uulm.snowballr.backend.rest.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import se.uulm.snowballr.backend.auth.ACCESS_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.auth.DummyUser
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.REFRESH_TOKEN_COOKIE_NAME
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus

class RequestContextFilter(
    private val authenticationManager: IAuthenticationManager,
    private val cookieManager: ICookieManager,
    private val envReader: EnvReader,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val context = RequestContext()

        when {
            envReader.env.miscellaneous.authBypassEnabled -> {
                context.userId = DummyUser.id
                context.authStatus = AuthenticationStatus.AUTHENTICATED
            }
            // CSRF defense-in-depth: SameSite=Strict already blocks classic cross-site requests, but it still
            // permits requests from a sibling subdomain. Requiring a custom header on cookie-authenticated,
            // state-changing requests defeats that gap, since forms/simple cross-site requests cannot set one.
            !passesCsrfDefenseInDepthCheck(request) -> {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Missing $REQUESTED_WITH_HEADER header")
                return
            }
            else -> {
                authenticate(request, context)
            }
        }

        publishAuthentication(context)

        try {
            RequestContext.with(context) {
                filterChain.doFilter(request, response)
            }
        } finally {
            // Cookies may have been queued above (pre-emptive token refresh / clearing) or by the handler
            // itself (e.g. login, logout). Both are flushed together now that the handler has run.
            writeQueuedCookies(context, response)
            SecurityContextHolder.clearContext()
        }
    }

    private fun authenticate(request: HttpServletRequest, context: RequestContext) {
        val cookies = cookieManager.parseCookies(request.getHeader(COOKIE_HEADER))
        val accessToken = cookies[ACCESS_TOKEN_COOKIE_NAME]
        val refreshToken = cookies[REFRESH_TOKEN_COOKIE_NAME]
        authenticationManager
            .authenticate(accessToken, refreshToken, skipRefresh = false, requestContext = context)
            .onSuccess { claims -> context.userId = claims.userId }
    }

    private fun passesCsrfDefenseInDepthCheck(request: HttpServletRequest): Boolean {
        if (request.method in SAFE_HTTP_METHODS) return true
        val cookies = cookieManager.parseCookies(request.getHeader(COOKIE_HEADER))
        val hasAuthCookie = cookies[ACCESS_TOKEN_COOKIE_NAME] != null || cookies[REFRESH_TOKEN_COOKIE_NAME] != null
        if (!hasAuthCookie) return true
        return request.getHeader(REQUESTED_WITH_HEADER) == REQUESTED_WITH_VALUE
    }

    private fun writeQueuedCookies(context: RequestContext, response: HttpServletResponse) {
        context.cookies.forEach { (name, value) ->
            cookieManager.buildAuthCookieString(name, value)?.let { response.addHeader(SET_COOKIE_HEADER, it) }
        }
    }

    private fun publishAuthentication(context: RequestContext) {
        val userId = context.userId ?: return
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }

    private companion object {
        const val COOKIE_HEADER = "Cookie"
        const val SET_COOKIE_HEADER = "Set-Cookie"
        const val REQUESTED_WITH_HEADER = "X-Requested-With"
        const val REQUESTED_WITH_VALUE = "XMLHttpRequest"
        val SAFE_HTTP_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE)
            .map { it.name() }
            .toSet()
    }
}
