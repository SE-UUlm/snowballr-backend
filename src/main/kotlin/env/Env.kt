package se.uulm.snowballr.backend.env

import io.github.cdimascio.dotenv.dotenv

// Default values
public const val DEFAULT_LOG_LEVEL = "DEBUG"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"

private val envService = EnvService()

data class Env(
    val http: Http = Http(),
    val miscellaneous: Miscellaneous = Miscellaneous(),
) {
    data class Http(
        val port: Int = envService[PORT].toInt(),
    )

    data class Miscellaneous(
        val logLevel: String = envService.getOrDefault(LOG_LEVEL, DEFAULT_LOG_LEVEL),
    )
}

interface IEnvService {
    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    operator fun get(key: String): String

    fun getOrDefault(
        key: String,
        default: String,
    ): String
}

class EnvVariableNotFoundException(
    key: String,
) : Exception(
        "The env variable with key '$key' could not be found. Please check the variables.",
    )

/**
 * Responsible to read data from the .env file
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
