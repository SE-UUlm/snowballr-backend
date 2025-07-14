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

    /**
     * Returns the boolean value of the env variable with the specified [key] or throws an
     * [EnvVariableNotFoundException] if the env variable couldn't be found or is not a valid boolean.
     */
    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    fun getBoolean(key: String): Boolean

    /**
     * Returns the boolean value of the env variable with the specified [key] or returns the [default]
     * value if the env variable couldn't be found or is not a valid boolean.
     */
    fun getBooleanOrDefault(key: String, default: Boolean): Boolean
}

/**
 * Service responsible for reading environment variables from the .env file
 */
class EnvService : IEnvService {
    private val dotenv = getEnv()

    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    override fun get(key: String): String = dotenv[key] ?: throw EnvVariableNotFoundException(key)

    override fun getOrDefault(key: String, default: String): String = dotenv[key] ?: default

    @kotlin.jvm.Throws(EnvVariableNotFoundException::class)
    override fun getBoolean(key: String): Boolean {
        val value = get(key)
        return value.toBooleanStrictOrNull()
            ?: throw EnvVariableNotFoundException("Invalid boolean value for key '$key': '$value'")
    }

    override fun getBooleanOrDefault(key: String, default: Boolean): Boolean {
        val value = dotenv[key] ?: return default
        return value.toBooleanStrictOrNull()
            ?: throw EnvVariableNotFoundException("Invalid boolean value for key '$key': '$value'")
    }

    companion object {
        private fun getEnv() = dotenv {
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }
    }
}
