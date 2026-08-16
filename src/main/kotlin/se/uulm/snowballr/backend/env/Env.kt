package se.uulm.snowballr.backend.env

import java.nio.file.Path

/**
 * Represents the environment configuration for the application.
 *
 * @property http Configuration related to the HTTP server, such as the port number.
 * @property miscellaneous Miscellaneous configuration, such as the logging level.
 * @property database Configuration related to the database connection, including user credentials.
 * @property encryption Configuration for encryption keys used in the application, such as JWT keys.
 * @property smtp Configuration for email settings, including SMTP host and credentials.
 * @property lifetime Configuration for various lifetimes of tokens and sensitive information retention.
 * @property plugins Configuration for plugins.
 */
data class Env(
    val http: Http,
    val miscellaneous: Miscellaneous,
    val database: Database,
    val encryption: Encryption,
    val smtp: SMTP,
    val lifetime: Lifetime,
    val plugins: Plugins,
) {
    data class Http(
        val port: Int,
    )

    data class Miscellaneous(
        val logLevel: String,
        val authBypassEnabled: Boolean,
        // Guaranteed to not contain a trailing slash
        val frontendBaseUrl: String,
    )

    data class Database(
        val password: String,
        val host: String,
        val seedUserEnabled: Boolean,
    )

    data class Encryption(
        val jwtPrivateKeyBase64: String,
        val jwtPublicKeyBase64: String,
    )

    data class SMTP(
        val smtpHost: String,
        val smtpPort: Int,
        val smtpUser: String?,
        val smtpPassword: String?,
        val smtpTransportLoggingOnlyEnabled: Boolean,
        val smtpSenderName: String,
        val smtpSenderEmail: String,
    )

    data class Lifetime(
        val sensitiveInformationRetentionDays: Int,
        val invitationTokenLifeTimeInDays: Int,
        val verificationTokenLifeTimeInDays: Int,
    )

    data class Plugins(
        val pluginDirectory: Path,
        val pythonExecutable: String,
    )
}
