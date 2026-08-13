package se.uulm.snowballr.backend.rest.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

        if (envReader.env.miscellaneous.authBypassEnabled) {
            context.userId = DummyUser.id
            context.authStatus = AuthenticationStatus.AUTHENTICATED
        } else {
            authenticate(request, context)
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
    }
}
