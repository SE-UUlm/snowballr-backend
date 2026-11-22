package se.uulm.snowballr.backend.env

import kotlin.enums.enumEntries

/**
 * Parses a string to an entry of the enum [T]. The matching is case-insensitive and ignores surrounding whitespace.
 *
 * @param T The enum type to parse into.
 * @param value The string value to parse.
 * @return The corresponding entry of [T], or null if the [value] doesn't match any of them.
 */
inline fun <reified T : Enum<T>> parseEnumIgnoreCase(value: String): T? =
    enumEntries<T>().firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

/**
 * Reads the environment variable [key] and parses it to an entry of the enum [T] using [parseEnumIgnoreCase].
 *
 * @param T The enum type to parse into, inferred from [default].
 * @param key The name of the environment variable.
 * @param default The value to use if the environment variable is not set.
 * @return The parsed entry of [T], or [default] if the environment variable is not set.
 * @throws EnvVariableNotParsableException if the value doesn't match any entry of [T].
 */
inline fun <reified T : Enum<T>> IEnvService.getEnumOrDefault(key: String, default: T): T =
    getOrDefault(key, default) { parseEnumIgnoreCase<T>(it) }
