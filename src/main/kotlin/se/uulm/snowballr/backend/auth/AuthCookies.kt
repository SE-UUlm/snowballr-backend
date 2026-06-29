package se.uulm.snowballr.backend.auth

import se.uulm.snowballr.backend.context.RequestContext

/**
 * Name of the cookie holding the access token.
 */
const val ACCESS_TOKEN_COOKIE_NAME = "access_token"

/**
 * Name of the cookie holding the refresh token.
 */
const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

/**
 * Queues the authentication cookies (access and refresh token) to be set on the response.
 *
 * This is the designated way for service logic (e.g. login, logout) to set session cookies without
 * needing to know about the underlying transport. Passing empty values expires the cookies.
 *
 * @param accessToken The new access token.
 * @param refreshToken The new refresh token.
 */
fun RequestContext.setAuthCookies(accessToken: String, refreshToken: String) {
    queueCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken)
    queueCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
}
