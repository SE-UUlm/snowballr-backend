package se.uulm.snowballr.backend.env

import io.github.cdimascio.dotenv.dotenv

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

private val envService = EnvService()

/**
 * Represents the environment configuration for the application.
 *
 * @property http Configuration related to the HTTP server, such as the port number.
 * @property miscellaneous Miscellaneous configuration, such as the logging level.
 * @property database Configuration related to the database connection, including user credentials.
 */
data class Env(
    val http: Http = Http(),
    val miscellaneous: Miscellaneous = Miscellaneous(),
    val database: Database = Database(),
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
}

class EnvVariableNotFoundException(
    key: String,
) : Exception(
        "The env variable with key '$key' could not be found. Please check the variables.",
    )

interface IEnvService {
    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    operator fun get(key: String): String

    fun getOrDefault(
        key: String,
        default: String,
    ): String
}

/**
 * Service responsible for reading environment variables from the .env file
 */
class EnvService : IEnvService {
    private val dotenv = getEnv()

    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    override fun get(key: String): String = dotenv[key] ?: throw EnvVariableNotFoundException(key)

    override fun getOrDefault(
        key: String,
        default: String,
    ): String = dotenv[key] ?: default

    companion object {
        private fun getEnv() =
            dotenv {
                ignoreIfMissing = true
                ignoreIfMalformed = true
            }
    }
}
