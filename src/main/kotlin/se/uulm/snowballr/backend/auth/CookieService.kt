package se.uulm.snowballr.backend.auth

import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.auth.CookieConfig

interface ICookieService {
    /**
     * Parses a `Cookie` header string into a map of key-value pairs.
     *
     * Example:
     * ```
     * Input:  "key1=value1; key2=value2"
     * Output: mapOf("key1" to "value1", "key2" to "value2")
     * ```
     *
     * @param cookieHeader The string value of the `Cookie` header, or null if no cookie is present.
     * @return A map of cookie names to cookie values. Returns an empty map if the header is null or blank.
     */
    fun parseCookies(cookieHeader: String?): Map<String, String>

    /**
     * Builds a complete `Set-Cookie` string for a specific authentication cookie.
     *
     * This is a specialized helper that uses strict security settings (`SameSite=Strict`, etc.)
     * and automatically determines the correct Max-Age for access and refresh tokens.
     *
     * @param name The name of the auth cookie (e.g., "access_token").
     * @param value The token value. A null or empty value signals that the cookie should be expired.
     * @return A formatted `Set-Cookie` string, or null if the name is not a recognized auth cookie.
     */
    fun buildAuthCookieString(name: String, value: String?): String?

    /**
     * Creates a general-purpose `Set-Cookie` header string from the provided [CookieConfig].
     *
     * Example:
     * ```
     * val config = CookieConfig(name = "theme", value = "dark", maxAgeSeconds = 31536000)
     * val cookieHeader = cookieService.createCookieString(config)
     * // "theme=dark; Max-Age=31536000; Path=/; SameSite=Lax; HttpOnly; Secure"
     * ```
     *
     * @param config The cookie configuration.
     * @return A formatted `Set-Cookie` string.
     */
    fun createCookieString(config: CookieConfig): String
}

/**
 * Utility object for parsing and constructing HTTP cookie headers.
 *
 * @param jwtService The JWT service used to resolve token TTLs.
 * @param envReader The environment reader used to access the current configuration.
 */
class CookieService(
    private val jwtService: IJwtService,
    private val envReader: EnvReader,
) : ICookieService {
    override fun parseCookies(cookieHeader: String?): Map<String, String> {
        if (cookieHeader.isNullOrBlank()) return emptyMap()
        return cookieHeader
            .split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts[1]
            }
    }

    override fun buildAuthCookieString(name: String, value: String?): String? {
        val ttl =
            when (name) {
                GrpcContext.ACCESS_TOKEN_COOKIE_NAME -> resolveTokenTTL(value) { jwtService.getAccessTokenTTL() }
                GrpcContext.REFRESH_TOKEN_COOKIE_NAME -> resolveTokenTTL(value) { jwtService.getRefreshTokenTTL() }
                else -> return null // Not a recognized authentication cookie.
            }

        val authCookieConfig = CookieConfig(
            name = name,
            value = value.orEmpty(), // Use empty string for expiration
            maxAgeSeconds = ttl,
            path = "/",
            sameSite = "Strict", // Stricter policy for auth tokens
            httpOnly = true,
            // Inverted condition here so it defaults to `true`
            secure = !envReader.env.miscellaneous.frontendBaseUrl.startsWith("http:"),
        )

        return createCookieString(authCookieConfig)
    }

    /**
     * Resolves the TTL for a token based on its value.
     *
     * @param value The token value, which may be null or empty.
     * @param ttlProvider A function that provides the TTL in seconds if the token is valid.
     * @return The TTL in seconds, or 0 if the value is null or empty (indicating an expired cookie).
     */
    private fun resolveTokenTTL(value: String?, ttlProvider: () -> Long): Long =
        if (value.isNullOrEmpty()) 0 else ttlProvider()

    override fun createCookieString(config: CookieConfig): String {
        val parts = mutableListOf(
            "${config.name}=${config.value}",
            "Max-Age=${config.maxAgeSeconds}",
            "Path=${config.path}",
            "SameSite=${config.sameSite}",
        )
        if (config.domain != null) parts.add("Domain=${config.domain}")
        if (config.httpOnly) parts.add("HttpOnly")
        if (config.secure) parts.add("Secure")

        return parts.joinToString("; ")
    }
}
