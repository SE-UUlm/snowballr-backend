package se.uulm.snowballr.backend.env

// Default values
const val DEFAULT_LOG_LEVEL = "DEBUG"
const val DEFAULT_DATABASE_HOST = "localhost"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"

// Database
private const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
private const val DATABASE_HOST = "DATABASE_HOST"

// Encryption
private const val JWT_PRIVATE_KEY_BASE64 = "JWT_PRIVATE_KEY_BASE64"
private const val JWT_PUBLIC_KEY_BASE64 = "JWT_PUBLIC_KEY_BASE64"

private val envService = EnvService()

/**
 * Represents the environment configuration for the application.
 *
 * @property http Configuration related to the HTTP server, such as the port number.
 * @property miscellaneous Miscellaneous configuration, such as the logging level.
 * @property database Configuration related to the database connection, including user credentials.
 * @property encryption Configuration for encryption keys used in the application, such as JWT keys.
 */
data class Env(
    val http: Http = Http(),
    val miscellaneous: Miscellaneous = Miscellaneous(),
    val database: Database = Database(),
    val encryption: Encryption = Encryption(),
) {
    data class Http(
        val port: Int = envService[PORT].toInt(),
    )

    data class Miscellaneous(
        val logLevel: String = envService.getOrDefault(LOG_LEVEL, DEFAULT_LOG_LEVEL),
    )

    data class Database(
        val password: String = envService[DATABASE_PASSWORD],
        val host: String = envService.getOrDefault(DATABASE_HOST, DEFAULT_DATABASE_HOST),
    )

    data class Encryption(
        val jwtPrivateKeyBase64: String = envService[JWT_PRIVATE_KEY_BASE64],
        val jwtPublicKeyBase64: String = envService[JWT_PUBLIC_KEY_BASE64],
    )
}
