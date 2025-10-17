package se.uulm.snowballr.backend.env

import io.github.cdimascio.dotenv.dotenv

interface IEnvService {
    /**
     * Returns the value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @return The value of the environment variable.
     * @throws EnvVariableNotFoundException if the environment variable is not set.
     */
    operator fun get(key: String): String

    /**
     * Returns the value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The value to return if the environment variable is not set.
     * @return The value of the environment variable, or [default] if not found.
     */
    fun getOrDefault(key: String, default: String): String

    /**
     * Returns the value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @return The value of the environment variable, or `null` if not found.
     */
    fun getOrNull(key: String): String?

    /**
     * Returns the boolean value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @return The boolean value of the environment variable.
     * @throws EnvVariableNotFoundException if the variable is not found or is not a valid boolean.
     */
    fun getBoolean(key: String): Boolean

    /**
     * Returns the boolean value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The value to return if the environment variable is not set.
     * @return The boolean value of the environment variable, or [default] if not found.
     */
    fun getBooleanOrDefault(key: String, default: Boolean): Boolean

    /**
     * Retrieves the value of the environment variable identified by [key].
     *
     * @param key Name of the environment variable to read.
     * @param default An optional value to return if the environment variable is not set.
     * @return The value of the environment variable, or [default] if provided and the variable is not found.
     * @throws EnvVariableNotFoundException if the environment variable is not set and [default] is `null`.
     */
    fun getRequiredOrDefault(key: String, default: String?): String
}

/**
 * Service responsible for reading environment variables from the .env file
 */
class EnvService : IEnvService {
    private val dotenv = getEnv()

    /**
     * Returns the raw value of the env variable or `null` if it couldn't be found or is empty.
     * It is checked whether the value is `null` or empty because the docker-compose file
     * may provide an empty string for a variable that is defined but not assigned a value.
     *
     * @param key Name of the environment variable to read
     * @return Value of the environment variable or `null` if not found or empty
     */
    private fun getRaw(key: String): String? {
        val value = dotenv[key]
        return if (value.isNullOrEmpty()) null else value
    }

    override fun get(key: String): String = getRaw(key) ?: throw EnvVariableNotFoundException(key)

    override fun getOrDefault(key: String, default: String): String = getRaw(key) ?: default

    override fun getOrNull(key: String): String? = getRaw(key)

    override fun getBoolean(key: String): Boolean {
        val value = get(key)
        return value.toBooleanStrictOrNull()
            ?: throw EnvVariableNotFoundException("Invalid boolean value for key '$key': '$value'")
    }

    override fun getBooleanOrDefault(key: String, default: Boolean): Boolean {
        val value = getRaw(key) ?: return default
        return value.toBooleanStrictOrNull()
            ?: throw EnvVariableNotFoundException("Invalid boolean value for key '$key': '$value'")
    }

    override fun getRequiredOrDefault(key: String, default: String?): String =
        getRaw(key) ?: default ?: throw EnvVariableNotFoundException(key)

    companion object {
        private fun getEnv() = dotenv {
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }
    }
}
