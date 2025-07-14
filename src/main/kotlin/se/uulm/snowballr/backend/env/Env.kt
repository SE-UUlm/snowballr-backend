package se.uulm.snowballr.backend.env

/**
 * Represents the environment configuration for the application.
 *
 * @property http Configuration related to the HTTP server, such as the port number.
 * @property miscellaneous Miscellaneous configuration, such as the logging level.
 * @property database Configuration related to the database connection, including user credentials.
 * @property encryption Configuration for encryption keys used in the application, such as JWT keys.
 */
data class Env(
    val http: Http,
    val miscellaneous: Miscellaneous,
    val database: Database,
    val encryption: Encryption,
) {
    data class Http(
        val port: Int,
    )

    data class Miscellaneous(
        val logLevel: String,
        val useDummyUser: Boolean,
    )

    data class Database(
        val password: String,
        val host: String,
    )

    data class Encryption(
        val jwtPrivateKeyBase64: String,
        val jwtPublicKeyBase64: String,
    )
}
