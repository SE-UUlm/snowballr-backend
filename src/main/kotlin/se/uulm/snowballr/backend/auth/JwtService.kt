package se.uulm.snowballr.backend.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.jwt.JwtTokens
import se.uulm.snowballr.backend.model.jwt.ParsedJwtClaims
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

interface IJwtService {
    /**
     * Generates a signed access and refresh token for the given user ID.
     *
     * @param userId The unique identifier of the user for whom the tokens are generated.
     * @return A [JwtTokens] object containing the generated access and refresh tokens.
     */
    fun generateTokens(userId: UUID): JwtTokens

    /**
     * Parses and validates a JWT token, returning structured claims.
     *
     * @param token The JWT token to parse.
     * @return ParsedJwtClaims if valid.
     * @throws JwtException If the token is invalid or expired.
     */
    fun parseToken(token: String?): ParsedJwtClaims

    /**
     * Uses the parsed refresh token claims to generate a new access token.
     *
     * **Note:** This function assumes the provided claims are from a valid, verified refresh token.
     * The caller is responsible for parsing and validating the token before invoking this function.
     *
     * @param refreshTokenClaims The parsed claims of the refresh token.
     * @return A new access token as a string.
     */
    fun refreshAccessToken(refreshTokenClaims: ParsedJwtClaims): String

    /**
     * Returns the configured time-to-live (TTL) for an access token in seconds.
     *
     * This is useful for setting cookie `Max-Age` attributes.
     *
     * @return The access token TTL in seconds.
     */
    fun getAccessTokenTTL(): Long

    /**
     * Returns the configured time-to-live (TTL) for a refresh token in seconds.
     *
     * This is useful for setting cookie `Max-Age` attributes.
     *
     * @return The refresh token TTL in seconds.
     */
    fun getRefreshTokenTTL(): Long
}

/**
 * Utility object for handling JWT (JSON Web Token) operations, including token generation,
 * parsing, and validation.
 */
class JwtService(
    private val envReader: EnvReader,
) : IJwtService {
    companion object {
        const val KEY_ALGORITHM = "RSA"
        const val ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L // 15 minutes
        const val REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        private const val ISSUER = "SnowballR"
        private const val AUDIENCE = "snowballr-backend"
        private const val CLOCK_SKEW_SECONDS = 180L
    }

    private val privateKey: PrivateKey
    private val publicKey: PublicKey

    init {
        logger.debug { "Initializing JWT private and public keys" }

        val env = envReader.env

        // Private Key
        val privateKeyBytes = Decoders.BASE64.decode(env.encryption.jwtPrivateKeyBase64)
        val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        privateKey = KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(privateKeySpec)

        // Public Key
        val publicKeyBytes = Decoders.BASE64.decode(env.encryption.jwtPublicKeyBase64)
        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        publicKey = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(publicKeySpec)

        logger.debug { "Initialized JWT private and public keys" }
    }

    override fun generateTokens(userId: UUID): JwtTokens {
        val accessToken = generateToken(userId, ACCESS_TOKEN_EXPIRATION_MS)
        val refreshToken = generateToken(userId, REFRESH_TOKEN_EXPIRATION_MS)
        return JwtTokens(accessToken, refreshToken)
    }

    /**
     * Generates a signed JWT token for the given user ID and expiration time.
     *
     * @param userId The user ID for the token subject.
     * @param expirationMs The expiration time in milliseconds from now.
     * @return The signed JWT token as a string.
     */
    private fun generateToken(userId: UUID, expirationMs: Long): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(userId.toString())
            .issuer(ISSUER)
            .audience()
            .add(AUDIENCE)
            .and()
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .claim("jti", UUID.randomUUID().toString())
            .signWith(privateKey)
            .compact()
    }

    override fun parseToken(token: String?): ParsedJwtClaims {
        if (token == null) throw JwtException("Token is null")

        val jws: Jws<Claims> = Jwts
            .parser()
            .verifyWith(publicKey)
            .requireIssuer(ISSUER)
            .requireAudience(AUDIENCE)
            .clockSkewSeconds(CLOCK_SKEW_SECONDS)
            .build()
            .parseSignedClaims(token)

        val claims = jws.payload

        return ParsedJwtClaims(
            userId = UUID.fromString(claims.subject),
            issuedAt = claims.issuedAt,
            expiration = claims.expiration,
        )
    }

    override fun refreshAccessToken(refreshTokenClaims: ParsedJwtClaims): String =
        generateToken(refreshTokenClaims.userId, ACCESS_TOKEN_EXPIRATION_MS)

    override fun getAccessTokenTTL(): Long = TimeUnit.MILLISECONDS.toSeconds(ACCESS_TOKEN_EXPIRATION_MS)

    override fun getRefreshTokenTTL(): Long = TimeUnit.MILLISECONDS.toSeconds(REFRESH_TOKEN_EXPIRATION_MS)
}
