package se.uulm.snowballr.backend.env

import java.nio.file.Path
import kotlin.time.Duration

/**
 * Represents the environment configuration for the application.
 *
 * @property http Configuration related to the HTTP server, such as the port number.
 * @property miscellaneous Miscellaneous configuration, such as the logging level.
 * @property database Configuration related to the database connection, including user credentials.
 * @property encryption Configuration for encryption keys used in the application, such as JWT keys.
 * @property mailer Configuration for sending emails, including the mailer implementation and SMTP settings.
 * @property lifetime Configuration for various lifetimes of tokens and sensitive information retention.
 * @property plugins Configuration for plugins.
 */
data class Env(
    val http: Http,
    val miscellaneous: Miscellaneous,
    val database: Database,
    val encryption: Encryption,
    val mailer: Mailer,
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

    /**
     * The mailer configuration.
     */
    sealed interface Mailer {
        /**
         * Configuration for [MailerImplementationType.SMTP].
         */
        data class Smtp(
            val host: String,
            val port: Int,
            val user: String?,
            val password: String?,
            val transportLoggingOnlyEnabled: Boolean,
            val senderName: String,
            val senderEmail: String,
        ) : Mailer

        /**
         * Configuration for [MailerImplementationType.DUMMY].
         */
        data object Dummy : Mailer
    }

    data class Lifetime(
        val sensitiveInformationRetentionDays: Int,
        val invitationTokenLifeTimeInDays: Int,
        val verificationTokenLifeTimeInDays: Int,
    )

    data class Plugins(
        val fetcher: Fetcher,
        val fetcherRetries: Int,
        val fetcherRetryDelay: Duration,
    ) {
        /**
         * The fetcher configuration.
         */
        sealed interface Fetcher {
            /**
             * Configuration for [FetcherImplementationType.PYTHON].
             */
            data class Python(
                val pluginDirectory: Path,
                val pythonExecutable: String,
            ) : Fetcher

            /**
             * Configuration for [FetcherImplementationType.DUMMY].
             */
            data object Dummy : Fetcher
        }
    }
}
