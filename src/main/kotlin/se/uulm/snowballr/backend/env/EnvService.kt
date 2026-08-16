package se.uulm.snowballr.backend.env

import io.github.cdimascio.dotenv.dotenv
import java.nio.file.InvalidPathException
import java.nio.file.Path

@Suppress("ComplexInterface")
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
     * @return The value of the environment variable, or `null` if not found.
     */
    fun getOrNull(key: String): String?

    /**
     * Returns the value of the environment variable with the specified [key], parsed to type [T].
     *
     * @param T The type to which the environment variable's value should be parsed.
     * @param key Name of the environment variable to read.
     * @param default The value to return if the environment variable is not set or cannot be parsed.
     * @param parser A function that takes a [String] and returns a nullable [T], used to parse the environment
     * variable's value. The parser should return `null` if parsing fails.
     * @return The parsed value of the environment variable, or [default] if not found.
     * @throws EnvVariableNotFoundException if the parsed value is invalid according to the [parser] function.
     */
    fun <T> getOrDefault(key: String, default: T, parser: (String) -> T?): T

    /**
     * Returns the value of the environment variable with the specified [key], parsed to type [T].
     *
     * @param T The type to which the environment variable's value should be parsed.
     * @param key Name of the environment variable to read.
     * @param default The value to return if the environment variable is not set. If set to `null`, an
     * exception will be thrown given that the variable is not set.
     * @param parser A function that takes a [String] and returns a nullable [T], used to parse the environment
     * variable's value. The parser should return `null` if parsing fails.
     * @return The parsed value of the environment variable, or [default] if not found and [default] is not `null`.
     * @throws EnvVariableNotFoundException if the environment variable is not set and [default] is `null`, or if the
     * parsed value is invalid according to the [parser] function.
     */
    fun <T> getRequiredOrDefault(key: String, default: T?, parser: (String) -> T?): T

    /**
     * Returns the string value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The string value to return if the environment variable is not set.
     * @return The value of the environment variable, or [default] if not found.
     */
    fun getStringOrDefault(key: String, default: String): String

    /**
     * Retrieves the value of the environment variable identified by [key].
     *
     * @param key Name of the environment variable to read.
     * @param default An optional value to return if the environment variable is not set.
     * @return The value of the environment variable, or [default] if provided and the variable is not found.
     * @throws EnvVariableNotFoundException if the environment variable is not set and [default] is `null`.
     */
    fun getRequiredStringOrDefault(key: String, default: String?): String

    /**
     * Returns the boolean value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The boolean value to return if the environment variable is not set.
     * @return The boolean value of the environment variable, or [default] if not found.
     */
    fun getBooleanOrDefault(key: String, default: Boolean): Boolean

    /**
     * Returns the integer value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The integer value to return if the environment variable is not set.
     * @return The integer value of the environment variable.
     */
    fun getIntOrDefault(key: String, default: Int): Int

    /**
     * Retrieves the integer value of the environment variable identified by [key].
     *
     * @param key Name of the environment variable to read.
     * @param default An optional integer value to return if the environment variable is not set.
     * @return The integer value of the environment variable, or [default] if provided and the variable is not found.
     * @throws EnvVariableNotFoundException if the environment variable is not set and [default] is `null`, or if the
     * value cannot be parsed as an integer
     */
    fun getRequiredIntOrDefault(key: String, default: Int?): Int

    /**
     * Returns the path value of the environment variable with the specified [key].
     *
     * @param key Name of the environment variable to read.
     * @param default The path value to return if the environment variable is not set.
     * @return The value of the environment variable, or [default] if not found.
     */
    fun getPathOrDefault(key: String, default: Path): Path
}

/**
 * Service responsible for reading environment variables from the .env file.
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

    override fun getOrNull(key: String): String? = getRaw(key)

    override fun <T> getOrDefault(key: String, default: T, parser: (String) -> T?): T {
        val value = getRaw(key) ?: return default

        return parser(value) ?: throw EnvVariableNotParsableException(key, value)
    }

    override fun getStringOrDefault(key: String, default: String): String = getOrDefault(key, default) { it }

    override fun getBooleanOrDefault(key: String, default: Boolean): Boolean =
        getOrDefault(key, default, String::toBooleanStrictOrNull)

    override fun getIntOrDefault(key: String, default: Int): Int = getOrDefault(key, default, String::toIntOrNull)

    override fun getRequiredStringOrDefault(key: String, default: String?): String =
        getRequiredOrDefault(key, default) { it }

    override fun getRequiredIntOrDefault(key: String, default: Int?): Int =
        getRequiredOrDefault(key, default, String::toIntOrNull)

    override fun <T> getRequiredOrDefault(key: String, default: T?, parser: (String) -> T?): T {
        val value = getRaw(key)
        if (value != null) {
            return parser(value) ?: throw EnvVariableNotParsableException(key, value)
        }

        return default ?: throw EnvVariableNotFoundException(key)
    }

    override fun getPathOrDefault(key: String, default: Path): Path = getOrDefault(key, default) {
        try {
            Path.of(it)
        } catch (_: InvalidPathException) {
            null
        }
    }

    companion object {
        private fun getEnv() = dotenv {
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }
    }
}
