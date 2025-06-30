package se.uulm.snowballr.backend.env

import io.github.cdimascio.dotenv.dotenv

interface IEnvService {
    /**
     * Returns the value of the env variable with the specified [key] or throws an
     * [EnvVariableNotFoundException] if the env variable couldn't be found.
     */
    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    operator fun get(key: String): String

    /**
     * Returns the value of the env variable with the specified [key] or returns the [default]
     * value if the env variable couldn't be found.
     */
    fun getOrDefault(key: String, default: String): String
}

/**
 * Service responsible for reading environment variables from the .env file
 */
class EnvService : IEnvService {
    private val dotenv = getEnv()

    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    override fun get(key: String): String = dotenv[key] ?: throw EnvVariableNotFoundException(key)

    override fun getOrDefault(key: String, default: String): String = dotenv[key] ?: default

    companion object {
        private fun getEnv() = dotenv {
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }
    }
}
