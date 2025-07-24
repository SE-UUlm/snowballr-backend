package se.uulm.snowballr.backend.env

import io.github.oshai.kotlinlogging.KotlinLogging

// Environment variable keys
private const val PROFILE = "PROFILE"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"
private const val AUTH_BYPASS = "AUTH_BYPASS"

// Database
private const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
private const val DATABASE_HOST = "DATABASE_HOST"
private const val DATABASE_SEED_USER = "DATABASE_SEED_USER"

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

        // Define profile-based defaults
        // For PRODUCTION, required values are left as null to enforce they are explicitly set
        val defaultPort: Int?
        val defaultDBHost: String?
        val defaultLogLevel: String
        val defaultAuthBypass: Boolean
        val defaultSeedUser: Boolean

        when (activeProfile) {
            AppProfile.TESTING -> {
                defaultPort = DEFAULT_PORT
                defaultDBHost = DEFAULT_DATABASE_HOST
                defaultLogLevel = "TRACE"
                defaultAuthBypass = true
                defaultSeedUser = true
            }

            AppProfile.DEVELOPMENT -> {
                defaultPort = DEFAULT_PORT
                defaultDBHost = DEFAULT_DATABASE_HOST
                defaultLogLevel = "DEBUG"
                defaultAuthBypass = false
                defaultSeedUser = true
            }

            AppProfile.PRODUCTION -> {
                defaultPort = null
                defaultDBHost = null
                defaultLogLevel = "INFO"
                defaultAuthBypass = false
                defaultSeedUser = false
            }
        }

        // Read final values, applying defaults and allowing overrides
        val port = envService.getRequiredOrDefault(PORT, defaultPort?.toString()).toInt()
        val host = envService.getRequiredOrDefault(DATABASE_HOST, defaultDBHost)
        val logLevel = envService.getOrDefault(LOG_LEVEL, defaultLogLevel)

        // If AUTH_BYPASS is true, we must also seed the user
        val authBypass = envService.getBooleanOrDefault(AUTH_BYPASS, defaultAuthBypass)
        val seedUser = authBypass || envService.getBooleanOrDefault(DATABASE_SEED_USER, defaultSeedUser)

        env = Env(
            http = Env.Http(port),
            miscellaneous = Env.Miscellaneous(logLevel, authBypass),
            database = Env.Database(
                password = envService[DATABASE_PASSWORD],
                host = host,
                seedUser = seedUser,
            ),
            encryption = Env.Encryption(
                jwtPrivateKeyBase64 = envService[JWT_PRIVATE_KEY_BASE64],
                jwtPublicKeyBase64 = envService[JWT_PUBLIC_KEY_BASE64],
            ),
        )
    }
}
