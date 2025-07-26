package se.uulm.snowballr.backend.env

import io.github.oshai.kotlinlogging.KotlinLogging

// Environment variable keys
private const val PROFILE = "PROFILE"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"
private const val AUTH_BYPASS_ENABLED = "AUTH_BYPASS_ENABLED"

// Database
private const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
private const val DATABASE_HOST = "DATABASE_HOST"
private const val DATABASE_SEED_USER_ENABLED = "DATABASE_SEED_USER_ENABLED"

// Encryption
private const val JWT_PRIVATE_KEY_BASE64 = "JWT_PRIVATE_KEY_BASE64"
private const val JWT_PUBLIC_KEY_BASE64 = "JWT_PUBLIC_KEY_BASE64"

// Default values
private const val DEFAULT_PORT = 8080
const val DEFAULT_LOG_LEVEL = "DEBUG"
private const val DEFAULT_DATABASE_HOST = "localhost"

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
    envService: IEnvService,
) {
    val env: Env

    init {
        val activeProfile = AppProfile.fromString(envService[PROFILE])
        logger.info { "Application starting with profile: $activeProfile" }

        val defaults = defaultsForProfile(activeProfile)

        // Read final values, applying defaults and allowing overrides
        val port = envService.getRequiredOrDefault(PORT, defaults.port?.toString()).toInt()
        val host = envService.getRequiredOrDefault(DATABASE_HOST, defaults.databaseHost)
        val logLevel = envService.getOrDefault(LOG_LEVEL, defaults.logLevel)

        // If AUTH_BYPASS_ENABLED is `true`, we must also seed the user
        val authBypassEnabled = envService.getBooleanOrDefault(AUTH_BYPASS_ENABLED, defaults.authBypassEnabled)
        val seedUserEnabled =
            authBypassEnabled || envService.getBooleanOrDefault(DATABASE_SEED_USER_ENABLED, defaults.seedUserEnabled)

        env = Env(
            http = Env.Http(port),
            miscellaneous = Env.Miscellaneous(logLevel, authBypassEnabled),
            database = Env.Database(
                password = envService[DATABASE_PASSWORD],
                host = host,
                seedUserEnabled = seedUserEnabled,
            ),
            encryption = Env.Encryption(
                jwtPrivateKeyBase64 = envService[JWT_PRIVATE_KEY_BASE64],
                jwtPublicKeyBase64 = envService[JWT_PUBLIC_KEY_BASE64],
            ),
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
            authBypassEnabled = true,
            seedUserEnabled = true,
        )

        AppProfile.DEVELOPMENT -> ProfileDefaults(
            port = DEFAULT_PORT,
            databaseHost = DEFAULT_DATABASE_HOST,
            logLevel = "DEBUG",
            authBypassEnabled = false,
            seedUserEnabled = true,
        )

        AppProfile.PRODUCTION -> ProfileDefaults(
            port = null,
            databaseHost = null,
            logLevel = "INFO",
            authBypassEnabled = false,
            seedUserEnabled = false,
        )
    }

    /**
     * Represents the application profile, which determines the environment configuration.
     *
     * @property port The port number for the HTTP server, or null if not set.
     * @property databaseHost The host for the database, or null if not set.
     * @property logLevel The logging level for the application.
     * @property authBypassEnabled Whether authentication bypass is enabled.
     * @property seedUserEnabled Whether the seed user is enabled.
     */
    private data class ProfileDefaults(
        val port: Int?,
        val databaseHost: String?,
        val logLevel: String,
        val authBypassEnabled: Boolean,
        val seedUserEnabled: Boolean,
    )
}
