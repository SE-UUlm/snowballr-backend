package se.uulm.snowballr.backend.model.auth

/**
 * The status of the user's authentication.
 *
 * It is required to be authenticated to use the SnowballR app.
 */
enum class AuthenticationStatus {
    /**
     * The user is unauthenticated.
     *
     * They have to sign in to the app.
     */
    UNAUTHENTICATED,

    /**
     * The user's access token is expired.
     *
     * They have to use the refresh token to get a new access token.
     */
    ACCESS_TOKEN_EXPIRED,

    /**
     * The user is authenticated.
     */
    AUTHENTICATED,
}
