package se.uulm.snowballr.backend.model.auth

/**
 * Configuration for creating a cookie string.
 *
 * @property name The name of the cookie.
 * @property value The value of the cookie.
 * @property maxAgeSeconds The max-age in seconds. A value of 0 will expire the cookie.
 * @property path The path for which the cookie is valid.
 * @property domain The domain for which the cookie is valid. Optional.
 * @property sameSite The SameSite policy ("Strict", "Lax", "None").
 * @property httpOnly If true, the cookie cannot be accessed by client-side scripts.
 * @property secure If true, the cookie will only be sent over HTTPS.
 */
data class CookieConfig(
    val name: String,
    val value: String,
    val maxAgeSeconds: Long,
    val path: String = "/",
    val domain: String? = null,
    val sameSite: String = "Lax",
    val httpOnly: Boolean = true,
    val secure: Boolean = true,
)
