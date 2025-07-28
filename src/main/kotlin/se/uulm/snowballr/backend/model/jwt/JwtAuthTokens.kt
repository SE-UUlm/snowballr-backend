package se.uulm.snowballr.backend.model.jwt

/**
 * Data class representing a pair of JWT tokens: access token and refresh token.
 */
data class JwtAuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
