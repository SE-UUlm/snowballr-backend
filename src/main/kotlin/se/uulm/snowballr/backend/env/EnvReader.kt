package se.uulm.snowballr.backend.env

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

// Environment variable keys
private const val PROFILE = "PROFILE"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"
private const val AUTH_BYPASS_ENABLED = "AUTH_BYPASS_ENABLED"
private const val FRONTEND_BASE_URL = "FRONTEND_BASE_URL"

// Database
private const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
private const val DATABASE_HOST = "DATABASE_HOST"
private const val DATABASE_SEED_USER_ENABLED = "DATABASE_SEED_USER_ENABLED"

// Encryption
private const val JWT_PRIVATE_KEY_BASE64 = "JWT_PRIVATE_KEY_BASE64"
private const val JWT_PUBLIC_KEY_BASE64 = "JWT_PUBLIC_KEY_BASE64"

// SMTP
private const val SMTP_HOST = "SMTP_HOST"
private const val SMTP_PORT = "SMTP_PORT"
private const val SMTP_USER = "SMTP_USER"
private const val SMTP_PASSWORD = "SMTP_PASSWORD"
private const val SMTP_TRANSPORT_LOGGING_ONLY_ENABLED = "SMTP_TRANSPORT_LOGGING_ONLY_ENABLED"
private const val SMTP_SENDER_NAME = "SMTP_SENDER_NAME"
private const val SMTP_SENDER_EMAIL = "SMTP_SENDER_EMAIL"

// Lifetime
private const val SENSITIVE_INFORMATION_RETENTION_DAYS = "SENSITIVE_INFORMATION_RETENTION_DAYS"
private const val INVITATION_TOKEN_LIFETIME_IN_DAYS = "INVITATION_TOKEN_LIFETIME_IN_DAYS"
private const val VERIFICATION_TOKEN_LIFETIME_IN_DAYS = "VERIFICATION_TOKEN_LIFETIME_IN_DAYS"

// Plugins
private const val PLUGIN_DIRECTORY = "PLUGIN_DIRECTORY"

// Default values
private val DEFAULT_PROFILE = AppProfile.PRODUCTION
private const val DEFAULT_PORT = 8080
const val DEFAULT_LOG_LEVEL = "DEBUG"
private const val DEFAULT_SENSITIVE_INFORMATION_RETENTION_DAYS = 30
private const val DEFAULT_DATABASE_HOST = "localhost"
private const val DEFAULT_INVITATION_TOKEN_LIFETIME_IN_DAYS = 7
private const val DEFAULT_VERIFICATION_TOKEN_LIFETIME_IN_DAYS = 1
private val DEFAULT_PLUGIN_DIRECTORY = Path.of("./plugins")

private val logger = KotlinLogging.logger {}

/**
 * The entrypoint for accessing environment variables. It determines the active configuration [AppProfile]
 * via the `PROFILE` variable to set sensible defaults. Explicitly set variables will always override
 * profile-based defaults.
 *
 * All registered variables can be accessed using this reader class.
 * Use the [env] property to access the environment variables in their respective group objects.
 *
 * @param envService The underlying service that provides methods to access variables by their key.
 */
class EnvReader(
    private val envService: IEnvService,
) {
    val env: Env

    init {
        val activeProfile = envService.getOrDefault(PROFILE, DEFAULT_PROFILE, AppProfile::fromString)
        logger.info { "Application starting with profile: $activeProfile" }

        val defaults = defaultsForProfile(activeProfile)

        // Build miscellaneous config to access `authBypassEnabled` for database config
        val miscellaneous = buildMiscellaneous(defaults)
        val database = buildDatabase(defaults, miscellaneous.authBypassEnabled)

        env = Env(
            http = buildHttp(defaults),
            miscellaneous = miscellaneous,
            database = database,
            encryption = buildEncryption(),
            smtp = buildSmtp(defaults),
            lifetime = buildLifetime(defaults),
            plugins = buildPlugins(),
        )
    }

    /**
     * Builds the HTTP configuration by reading and processing related environment variables.
     *
     * @param defaults Default values for the HTTP configuration.
     * @return An [Env.Http] object containing the HTTP configuration.
     */
    private fun buildHttp(defaults: ProfileDefaults): Env.Http {
        return Env.Http(
            port = envService.getRequiredOrDefault(PORT, defaults.port, String::toIntOrNull),
        )
    }

    /**
     * Builds the miscellaneous configuration by reading and processing related environment variables.
     *
     * @param defaults Default values for the miscellaneous configuration.
     * @return An [Env.Miscellaneous] object containing the miscellaneous configuration.
     */
    private fun buildMiscellaneous(defaults: ProfileDefaults): Env.Miscellaneous {
        val frontendBaseUrl = envService.getRequiredStringOrDefault(FRONTEND_BASE_URL, defaults.frontendBaseUrl)
            .trim().trimEnd('/')

        if (frontendBaseUrl.startsWith("http:")) {
            logger.warn {
                "The frontend url is set to the 'http' protocol, hinting at a possible security issue." +
                    " Prefer 'https' wherever you can."
            }
        }

        return Env.Miscellaneous(
            logLevel = envService.getStringOrDefault(LOG_LEVEL, defaults.logLevel),
            authBypassEnabled = envService.getBooleanOrDefault(AUTH_BYPASS_ENABLED, defaults.authBypassEnabled),
            frontendBaseUrl = frontendBaseUrl,
        )
    }

    /**
     * Builds the database configuration by reading and processing related environment variables.
     *
     * @param defaults Default values for the database configuration.
     * @param authBypassEnabled Whether authentication bypass is enabled.
     * @return An [Env.Database] object containing the database configuration.
     */
    private fun buildDatabase(defaults: ProfileDefaults, authBypassEnabled: Boolean): Env.Database {
        val seedUserEnabled =
            authBypassEnabled || envService.getBooleanOrDefault(DATABASE_SEED_USER_ENABLED, defaults.seedUserEnabled)

        return Env.Database(
            password = envService[DATABASE_PASSWORD],
            host = envService.getRequiredStringOrDefault(DATABASE_HOST, defaults.databaseHost),
            seedUserEnabled = seedUserEnabled,
        )
    }

    /**
     * Builds the encryption configuration by reading and processing related environment variables.
     *
     * @return An [Env.Encryption] object containing the encryption configuration.
     */
    private fun buildEncryption(): Env.Encryption = Env.Encryption(
        jwtPrivateKeyBase64 = envService[JWT_PRIVATE_KEY_BASE64],
        jwtPublicKeyBase64 = envService[JWT_PUBLIC_KEY_BASE64],
    )

    /**
     * Builds the SMTP configuration by reading and processing related environment variables.
     *
     * @param defaults Default values for the SMTP configuration.
     * @return An [Env.SMTP] object containing the SMTP configuration.
     */
    private fun buildSmtp(defaults: ProfileDefaults): Env.SMTP {
        val smtpTransportLoggingOnlyEnabled =
            envService.getBooleanOrDefault(
                SMTP_TRANSPORT_LOGGING_ONLY_ENABLED,
                defaults.smtpTransportLoggingOnlyEnabled,
            )

        return Env.SMTP(
            smtpHost = envService[SMTP_HOST],
            smtpPort = envService[SMTP_PORT].toInt(),
            smtpUser = envService.getOrNull(SMTP_USER),
            smtpPassword = envService.getOrNull(SMTP_PASSWORD),
            smtpTransportLoggingOnlyEnabled = smtpTransportLoggingOnlyEnabled,
            smtpSenderName = envService[SMTP_SENDER_NAME],
            smtpSenderEmail = envService[SMTP_SENDER_EMAIL],
        )
    }

    private fun buildLifetime(defaults: ProfileDefaults): Env.Lifetime {
        val sensitiveInformationRetentionDays = envService.getRequiredIntOrDefault(
            SENSITIVE_INFORMATION_RETENTION_DAYS,
            defaults.sensitiveInformationRetentionDays,
        )
        val invitationTokenLifeTimeInDays = envService.getIntOrDefault(
            INVITATION_TOKEN_LIFETIME_IN_DAYS,
            DEFAULT_INVITATION_TOKEN_LIFETIME_IN_DAYS,
        )
        val verificationTokenLifeTimeInDays = envService.getIntOrDefault(
            VERIFICATION_TOKEN_LIFETIME_IN_DAYS,
            DEFAULT_VERIFICATION_TOKEN_LIFETIME_IN_DAYS,
        )

        return Env.Lifetime(
            sensitiveInformationRetentionDays = sensitiveInformationRetentionDays,
            invitationTokenLifeTimeInDays = invitationTokenLifeTimeInDays,
            verificationTokenLifeTimeInDays = verificationTokenLifeTimeInDays,
        )
    }

    /**
     * Builds the plugins configuration by reading and processing related environment variables.
     *
     * @return An [Env.Plugins] object containing the plugins configuration.
     */
    private fun buildPlugins(): Env.Plugins {
        return Env.Plugins(
            pluginDirectory = envService.getPathOrDefault(PLUGIN_DIRECTORY, DEFAULT_PLUGIN_DIRECTORY),
        )
    }

    /**
     * Returns the default values for the specified [profile]. These defaults are used to
     * initialize the environment configuration if the corresponding environment variables are not set.
     *
     * @param profile The application profile for which to retrieve the defaults.
     * @return A [ProfileDefaults] object containing the default values for the specified profile.
     */
    private fun defaultsForProfile(profile: AppProfile): ProfileDefaults = when (profile) {
        AppProfile.TESTING -> ProfileDefaults(
            port = DEFAULT_PORT,
            databaseHost = DEFAULT_DATABASE_HOST,
            logLevel = "TRACE",
            sensitiveInformationRetentionDays = DEFAULT_SENSITIVE_INFORMATION_RETENTION_DAYS,
            authBypassEnabled = true,
            frontendBaseUrl = "http://localhost:5173",
            seedUserEnabled = true,
            smtpTransportLoggingOnlyEnabled = true,
        )

        AppProfile.DEVELOPMENT -> ProfileDefaults(
            port = DEFAULT_PORT,
            databaseHost = DEFAULT_DATABASE_HOST,
            logLevel = "DEBUG",
            sensitiveInformationRetentionDays = DEFAULT_SENSITIVE_INFORMATION_RETENTION_DAYS,
            authBypassEnabled = false,
            frontendBaseUrl = "http://localhost:5173",
            seedUserEnabled = true,
            smtpTransportLoggingOnlyEnabled = true,
        )

        AppProfile.PRODUCTION -> ProfileDefaults(
            port = null,
            databaseHost = null,
            logLevel = "INFO",
            sensitiveInformationRetentionDays = DEFAULT_SENSITIVE_INFORMATION_RETENTION_DAYS,
            authBypassEnabled = false,
            frontendBaseUrl = null,
            seedUserEnabled = false,
            smtpTransportLoggingOnlyEnabled = false,
        )
    }

    /**
     * Represents the application profile, which determines the environment configuration.
     *
     * @property port The port number for the HTTP server, or null if not set.
     * @property databaseHost The host for the database, or null if not set.
     * @property logLevel The logging level for the application.
     * @property sensitiveInformationRetentionDays The number of days to retain sensitive information.
     * @property authBypassEnabled Whether authentication bypass is enabled.
     * @property frontendBaseUrl The base URL for the frontend application.
     * @property seedUserEnabled Whether the seed user is enabled.
     * @property smtpTransportLoggingOnlyEnabled Whether SMTP transport logging is enabled.
     */
    private data class ProfileDefaults(
        val port: Int?,
        val databaseHost: String?,
        val logLevel: String,
        val sensitiveInformationRetentionDays: Int,
        val authBypassEnabled: Boolean,
        val frontendBaseUrl: String?,
        val seedUserEnabled: Boolean,
        val smtpTransportLoggingOnlyEnabled: Boolean,
    )
}
